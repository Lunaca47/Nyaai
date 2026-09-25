package com.nyaai

import com.nyaai.data.local.MatterEntity
import com.nyaai.data.local.RagDao
import com.nyaai.data.matter.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MatterWorkspaceTest {

    private class WorkspaceFakeRagDao : FakeRagDao() {
        private val matters = mutableMapOf<String, MatterEntity>()

        override suspend fun insertMatter(matter: MatterEntity) {
            matters[matter.matterId] = matter
        }

        override suspend fun getMatter(matterId: String): MatterEntity? {
            return matters[matterId]
        }

        override suspend fun getAllMatters(): List<MatterEntity> {
            return matters.values.sortedByDescending { it.updatedAt }
        }

        override suspend fun updateMatter(matter: MatterEntity) {
            matters[matter.matterId] = matter
        }

        override suspend fun deleteMatter(matterId: String) {
            matters.remove(matterId)
        }
    }

    @Test
    fun testMatterCreationWithFullTabSchema() = runBlocking {
        val dao = WorkspaceFakeRagDao()
        val stateManager = CaseStateManager(dao)

        val matterId = UUID.randomUUID().toString()
        val matter = Matter(
            matterId = matterId,
            userId = "user_123",
            title = "Unlawful Eviction in Koramangala",
            domain = "tenancy_deposit",
            status = "active",
            jurisdiction = JurisdictionInfo(state = "Karnataka", confidence = JurisdictionConfidence.CONFIRMED),
            facts = listOf(
                MatterFact(statement = "Deposit of 80000 INR withheld without justification", source = FactSource.USER_STATED, confidence = "stated"),
                MatterFact(statement = "Lockout occurred on 2026-08-15 without notice", source = FactSource.USER_STATED, confidence = "stated")
            ),
            timeline = listOf(
                TimelineEvent(eventDate = "2026-08-15", description = "Landlord replaced outer door lock", sourceFactId = "user_statement"),
                TimelineEvent(eventDate = "2026-08-18", description = "Legal demand notice sent via Registered Post", sourceFactId = "postal_receipt")
            ),
            evidence = listOf(
                MatterEvidence(
                    objectKey = "lease_agreement.pdf",
                    docType = "Rental Agreement",
                    extractedText = "Proves lease duration and deposit amount"
                )
            ),
            applicableLaws = listOf(
                ApplicableLaw(
                    act = "Bharatiya Nyaya Sanhita, 2023",
                    section = "Section 329",
                    currentnessStatus = "in_force"
                ),
                ApplicableLaw(
                    act = "Transfer of Property Act, 1882",
                    section = "Section 106",
                    currentnessStatus = "in_force"
                )
            ),
            proceduralStage = "legal_notice_issued",
            missingInformation = listOf("Written lease copy", "Notice delivery proof"),
            options = listOf("Send statutory 15-day demand notice", "File complaint u/s 173 BNSS"),
            risks = listOf("Disputed damage claims by landlord"),
            nextActions = listOf("Verify tracking status of legal notice", "Draft magistrate application"),
            openQuestions = listOf("Was any inspection done prior to lockout?")
        )

        // 1. Create Matter
        val createdId = stateManager.createMatter(matter)
        assertEquals(matterId, createdId)

        // 2. Retrieve Matter
        val retrieved = stateManager.getMatter(matterId)
        assertNotNull(retrieved)
        assertEquals("Unlawful Eviction in Koramangala", retrieved!!.title)
        assertEquals("Karnataka", retrieved.jurisdiction.state)
        assertEquals(JurisdictionConfidence.CONFIRMED, retrieved.jurisdiction.confidence)
        assertEquals(2, retrieved.facts.size)
        assertEquals(2, retrieved.timeline.size)
        assertEquals(1, retrieved.evidence.size)
        assertEquals(2, retrieved.applicableLaws.size)
        assertEquals(2, retrieved.missingInformation.size)

        // 3. Update Matter (marking next action complete)
        val updated = retrieved.copy(
            proceduralStage = "completed",
            nextActions = listOf("Draft magistrate application")
        )
        stateManager.updateMatter(updated)

        val reRetrieved = stateManager.getMatter(matterId)
        assertNotNull(reRetrieved)
        assertEquals("completed", reRetrieved!!.proceduralStage)
        assertEquals(1, reRetrieved.nextActions.size)

        // 4. Delete Matter
        stateManager.deleteMatter(matterId)
        assertNull(stateManager.getMatter(matterId))
    }

    @Test
    fun testMatterBackendSyncGracefulFallback() = runBlocking {
        val dao = WorkspaceFakeRagDao()
        val stateManager = CaseStateManager(dao)

        val matterId = UUID.randomUUID().toString()
        val matter = Matter(
            matterId = matterId,
            userId = "user_456",
            title = "Cheque Dishonour 138 NI Act",
            domain = "cheque_bounce",
            status = "active",
            jurisdiction = JurisdictionInfo(state = "Delhi", confidence = JurisdictionConfidence.CONFIRMED),
            proceduralStage = "notice_period"
        )
        stateManager.createMatter(matter)

        // Sync with non-existent server port should gracefully fall back to local Matter without throwing
        val syncResult = stateManager.syncMatterWithBackend(matterId, "http://127.0.0.1:59999")
        assertTrue("Sync should succeed with local fallback", syncResult.isSuccess)
        assertEquals("Cheque Dishonour 138 NI Act", syncResult.getOrNull()?.title)
    }

    @Test
    fun testGetAllMattersOrdering() = runBlocking {
        val dao = WorkspaceFakeRagDao()
        val stateManager = CaseStateManager(dao)

        val m1 = Matter(matterId = "m1", userId = "u1", title = "Case 1", domain = "tenancy", updatedAt = 1000L)
        val m2 = Matter(matterId = "m2", userId = "u1", title = "Case 2", domain = "cyber", updatedAt = 2000L)
        stateManager.createMatter(m1)
        stateManager.createMatter(m2)

        val all = stateManager.getAllMatters()
        assertEquals(2, all.size)
    }

    @Test
    fun testMatterDataEncryptionAtRest() = runBlocking {
        val dao = WorkspaceFakeRagDao()
        val stateManager = CaseStateManager(dao)

        val sensitiveStatement = "Allegation: Stridhan withheld and physical harassment occurred on 2026-07-10"
        val matter = Matter(
            matterId = "matter_enc_001",
            userId = "user_victim",
            title = "Domestic Violence and Maintenance Application",
            domain = "domestic_violence",
            facts = listOf(MatterFact(statement = sensitiveStatement, source = FactSource.USER_STATED, confidence = "HIGH"))
        )

        // Save to Room via state manager
        stateManager.createMatter(matter)

        // Inspect raw stored entity in DAO
        val rawEntity = dao.getMatter("matter_enc_001")
        assertNotNull(rawEntity)
        assertTrue("Stored caseStateJson MUST be encrypted with enc:v1: prefix", rawEntity!!.caseStateJson.startsWith("enc:v1:"))
        assertFalse("Plaintext sensitive statement MUST NOT be visible on disk in raw database entity", rawEntity.caseStateJson.contains("Stridhan"))

        // Retrieve through state manager -> MUST be transparently decrypted
        val decryptedMatter = stateManager.getMatter("matter_enc_001")
        assertNotNull(decryptedMatter)
        assertEquals(1, decryptedMatter!!.facts.size)
        assertEquals(sensitiveStatement, decryptedMatter.facts[0].statement)
    }

    @Test
    fun testToBackendUuidNormalization() {
        // Standard UUID remains unchanged
        val validUuid = "123e4567-e89b-12d3-a456-426614174000"
        assertEquals(validUuid, CaseStateManager.toBackendUuid(validUuid))

        // Guest user or arbitrary string is deterministically converted into valid UUIDv3
        val guestUuid = CaseStateManager.toBackendUuid("guest_user")
        assertNotNull(UUID.fromString(guestUuid))
        // Deterministic check
        assertEquals(guestUuid, CaseStateManager.toBackendUuid("guest_user"))
    }

    @Test
    fun testSimulatedNewDeviceMatterRecovery() = runBlocking {
        // 1. Device 1 creates rich Matter
        val daoDevice1 = WorkspaceFakeRagDao()
        val stateManagerDevice1 = CaseStateManager(daoDevice1)

        val originalMatter = Matter(
            matterId = "matter_cross_device_999",
            userId = "user_cross_device",
            title = "Unpaid Salary and Gratuity Recovery",
            domain = "employment_dispute",
            facts = listOf(
                MatterFact(statement = "Last 3 months salary unpaid: Rs 150000", source = FactSource.DOCUMENT_EXTRACTED, confidence = "HIGH"),
                MatterFact(statement = "6 years of continuous service completed", source = FactSource.USER_STATED, confidence = "HIGH")
            ),
            timeline = listOf(
                TimelineEvent(eventDate = "2026-06-30", description = "Resignation tendered with 30-day notice", sourceFactId = "fact_1")
            ),
            evidence = listOf(
                MatterEvidence(objectKey = "salary_slips.pdf", docType = "Salary Slip", extractedText = "Base CTC Rs 50000/mo")
            ),
            applicableLaws = listOf(
                ApplicableLaw(act = "Payment of Gratuity Act, 1972", section = "Section 7", currentnessStatus = "in_force")
            )
        )
        stateManagerDevice1.createMatter(originalMatter)

        // 2. Prepare mock backend JSON response (as returned by GET /api/v1/matters)
        val backendJsonArray = """
            [
                {
                    "id": "${originalMatter.matterId}",
                    "user_id": "${CaseStateManager.toBackendUuid(originalMatter.userId)}",
                    "title": "${originalMatter.title}",
                    "domain": "${originalMatter.domain}",
                    "procedural_stage": "${originalMatter.proceduralStage}",
                    "status": "${originalMatter.status}",
                    "case_state_json": ${org.json.JSONObject.quote(originalMatter.toJson())}
                }
            ]
        """.trimIndent()

        // 3. Device 2 starts with fresh, empty Room store (new phone setup)
        val daoDevice2 = WorkspaceFakeRagDao()
        val stateManagerDevice2 = CaseStateManager(daoDevice2)
        assertEquals(0, stateManagerDevice2.getAllMatters().size)

        // 4. Restore matters on Device 2
        val restored = stateManagerDevice2.restoreFromBackendJson(backendJsonArray, originalMatter.userId)
        assertEquals(1, restored.size)

        // 5. Verify full fidelity on Device 2
        val retrievedDevice2 = stateManagerDevice2.getMatter("matter_cross_device_999")
        assertNotNull(retrievedDevice2)
        assertEquals("Unpaid Salary and Gratuity Recovery", retrievedDevice2!!.title)
        assertEquals(2, retrievedDevice2.facts.size)
        assertEquals("Last 3 months salary unpaid: Rs 150000", retrievedDevice2.facts[0].statement)
        assertEquals(1, retrievedDevice2.timeline.size)
        assertEquals(1, retrievedDevice2.evidence.size)
        assertEquals("Payment of Gratuity Act, 1972", retrievedDevice2.applicableLaws[0].act)

        // 6. Confirm Device 2's local store is encrypted at rest
        val rawEntityDevice2 = daoDevice2.getMatter("matter_cross_device_999")
        assertNotNull(rawEntityDevice2)
        assertTrue("Device 2 stored data must be encrypted with enc:v1:", rawEntityDevice2!!.caseStateJson.startsWith("enc:v1:"))
    }
}



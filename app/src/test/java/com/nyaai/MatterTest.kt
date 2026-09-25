package com.nyaai

import com.nyaai.data.matter.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MatterTest {

    @Test
    fun testMatterSerializationRoundTrip() {
        val original = Matter(
            matterId = "matter-1234",
            userId = "citizen-001",
            title = "Illegal Flat Lockout",
            status = "active",
            domain = "tenancy_deposit",
            jurisdiction = JurisdictionInfo(
                state = "Maharashtra",
                city = "Mumbai",
                confidence = JurisdictionConfidence.CONFIRMED
            ),
            facts = listOf(
                MatterFact(
                    slotName = "deposit_amount",
                    statement = "Security deposit of Rs 1,00,000 withheld by landlord",
                    source = FactSource.USER_STATED,
                    confidence = "HIGH"
                )
            ),
            timeline = listOf(
                TimelineEvent(
                    eventDate = "2026-09-01",
                    description = "Landlord placed padlocks on flat door"
                )
            ),
            legalIssues = listOf(
                LegalIssue(
                    title = "Unlawful Dispossession without Due Process",
                    statutoryBasis = "Section 6 Specific Relief Act / Sec 329 BNS"
                )
            ),
            applicableLaws = listOf(
                ApplicableLaw(
                    act = "Bharatiya Nyaya Sanhita, 2023",
                    section = "329",
                    currentnessStatus = "in_force"
                )
            ),
            proceduralStage = "assessment"
        )

        val json = original.toJson()
        assertNotNull(json)
        assertTrue(json.contains("matter-1234"))
        assertTrue(json.contains("Maharashtra"))

        val restored = Matter.fromJson(json)
        assertEquals(original.matterId, restored.matterId)
        assertEquals(original.userId, restored.userId)
        assertEquals(original.title, restored.title)
        assertEquals(original.domain, restored.domain)
        assertEquals(original.jurisdiction.state, restored.jurisdiction.state)
        assertEquals(original.jurisdiction.confidence, restored.jurisdiction.confidence)
        assertEquals(1, restored.facts.size)
        assertEquals("deposit_amount", restored.facts[0].slotName)
        assertEquals(FactSource.USER_STATED, restored.facts[0].source)
        assertEquals(1, restored.timeline.size)
        assertEquals(1, restored.legalIssues.size)
        assertEquals(1, restored.applicableLaws.size)
    }

    @Test
    fun testCaseStateManagerCrud() = runTest {
        val fakeDao = FakeRagDao()
        val manager = CaseStateManager(fakeDao)

        val matter = Matter(
            matterId = "m-test-1",
            title = "Test Cheque Bounce",
            domain = "cheque_bounce"
        )

        val createdId = manager.createMatter(matter)
        assertEquals("m-test-1", createdId)

        val retrieved = manager.getMatter("m-test-1")
        assertNotNull(retrieved)
        assertEquals("Test Cheque Bounce", retrieved!!.title)

        val updated = retrieved.copy(status = "closed", proceduralStage = "completed")
        manager.updateMatter(updated)

        val retrievedUpdated = manager.getMatter("m-test-1")
        assertEquals("closed", retrievedUpdated!!.status)
        assertEquals("completed", retrievedUpdated.proceduralStage)

        val all = manager.getAllMatters()
        assertEquals(1, all.size)

        manager.deleteMatter("m-test-1")
        val afterDelete = manager.getMatter("m-test-1")
        assertNull(afterDelete)
    }
}

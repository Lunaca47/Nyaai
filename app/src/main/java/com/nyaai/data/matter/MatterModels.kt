package com.nyaai.data.matter

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class JurisdictionConfidence {
    CONFIRMED, INFERRED, UNKNOWN
}

enum class FactSource {
    USER_STATED, DOCUMENT_EXTRACTED, INFERRED
}

data class JurisdictionInfo(
    val state: String? = null,
    val city: String? = null,
    val confidence: JurisdictionConfidence = JurisdictionConfidence.UNKNOWN
)

data class MatterFact(
    val factId: String = UUID.randomUUID().toString(),
    val slotName: String? = null,
    val statement: String,
    val source: FactSource = FactSource.USER_STATED,
    val confidence: String = "STATED",
    val supportedByEvidenceIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class TimelineEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventDate: String? = null,
    val description: String,
    val sourceFactId: String? = null
)

data class MatterEvidence(
    val evidenceId: String = UUID.randomUUID().toString(),
    val objectKey: String,
    val docType: String? = null,
    val extractedText: String? = null,
    val classificationConfidence: Float? = null,
    val uploadedAt: Long = System.currentTimeMillis()
)

data class LegalIssue(
    val issueId: String = UUID.randomUUID().toString(),
    val title: String,
    val statutoryBasis: String,
    val confidence: Double = 0.90
)

data class ApplicableLaw(
    val act: String,
    val section: String,
    val currentnessStatus: String = "in_force", // "in_force", "amended", "repealed"
    val asOf: String = "2026-09"
)

data class EscalationInfo(
    val triggerReason: String,
    val recommendedPath: String,
    val escalatedAt: Long = System.currentTimeMillis()
)

data class Matter(
    val matterId: String = UUID.randomUUID().toString(),
    val userId: String = "guest_user",
    val title: String,
    val status: String = "active", // "active", "closed", "escalated"
    val domain: String,
    val jurisdiction: JurisdictionInfo = JurisdictionInfo(),
    val facts: List<MatterFact> = emptyList(),
    val disputedFacts: List<String> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val evidence: List<MatterEvidence> = emptyList(),
    val documents: List<String> = emptyList(),
    val legalIssues: List<LegalIssue> = emptyList(),
    val applicableLaws: List<ApplicableLaw> = emptyList(),
    val caseLaw: List<String> = emptyList(),
    val proceduralStage: String = "intake", // "intake", "assessment", "notice_drafting", "statutory_escalation", "judicial_petition"
    val deadlines: List<String> = emptyList(),
    val missingInformation: List<String> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val nextActions: List<String> = emptyList(),
    val openQuestions: List<String> = emptyList(),
    val escalation: EscalationInfo? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("matterId", matterId)
        root.put("userId", userId)
        root.put("title", title)
        root.put("status", status)
        root.put("domain", domain)
        root.put("proceduralStage", proceduralStage)
        root.put("createdAt", createdAt)
        root.put("updatedAt", updatedAt)

        val jObj = JSONObject()
        jObj.put("state", jurisdiction.state ?: JSONObject.NULL)
        jObj.put("city", jurisdiction.city ?: JSONObject.NULL)
        jObj.put("confidence", jurisdiction.confidence.name)
        root.put("jurisdiction", jObj)

        val fArray = JSONArray()
        facts.forEach { f ->
            val fo = JSONObject()
            fo.put("factId", f.factId)
            fo.put("slotName", f.slotName ?: JSONObject.NULL)
            fo.put("statement", f.statement)
            fo.put("source", f.source.name)
            fo.put("confidence", f.confidence)
            fo.put("supportedBy", JSONArray(f.supportedByEvidenceIds))
            fo.put("createdAt", f.createdAt)
            fArray.put(fo)
        }
        root.put("facts", fArray)

        root.put("disputedFacts", JSONArray(disputedFacts))

        val tArray = JSONArray()
        timeline.forEach { t ->
            val to = JSONObject()
            to.put("eventId", t.eventId)
            to.put("eventDate", t.eventDate ?: JSONObject.NULL)
            to.put("description", t.description)
            to.put("sourceFactId", t.sourceFactId ?: JSONObject.NULL)
            tArray.put(to)
        }
        root.put("timeline", tArray)

        val eArray = JSONArray()
        evidence.forEach { e ->
            val eo = JSONObject()
            eo.put("evidenceId", e.evidenceId)
            eo.put("objectKey", e.objectKey)
            eo.put("docType", e.docType ?: JSONObject.NULL)
            eo.put("extractedText", e.extractedText ?: JSONObject.NULL)
            eo.put("classificationConfidence", e.classificationConfidence?.toDouble() ?: JSONObject.NULL)
            eo.put("uploadedAt", e.uploadedAt)
            eArray.put(eo)
        }
        root.put("evidence", eArray)

        root.put("documents", JSONArray(documents))

        val iArray = JSONArray()
        legalIssues.forEach { i ->
            val io = JSONObject()
            io.put("issueId", i.issueId)
            io.put("title", i.title)
            io.put("statutoryBasis", i.statutoryBasis)
            io.put("confidence", i.confidence)
            iArray.put(io)
        }
        root.put("legalIssues", iArray)

        val lArray = JSONArray()
        applicableLaws.forEach { l ->
            val lo = JSONObject()
            lo.put("act", l.act)
            lo.put("section", l.section)
            lo.put("currentnessStatus", l.currentnessStatus)
            lo.put("asOf", l.asOf)
            lArray.put(lo)
        }
        root.put("applicableLaws", lArray)

        root.put("caseLaw", JSONArray(caseLaw))
        root.put("deadlines", JSONArray(deadlines))
        root.put("missingInformation", JSONArray(missingInformation))
        root.put("assumptions", JSONArray(assumptions))
        root.put("options", JSONArray(options))
        root.put("risks", JSONArray(risks))
        root.put("nextActions", JSONArray(nextActions))
        root.put("openQuestions", JSONArray(openQuestions))

        if (escalation != null) {
            val esc = JSONObject()
            esc.put("triggerReason", escalation.triggerReason)
            esc.put("recommendedPath", escalation.recommendedPath)
            esc.put("escalatedAt", escalation.escalatedAt)
            root.put("escalation", esc)
        }

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): Matter {
            val root = JSONObject(jsonStr)
            val jObj = root.optJSONObject("jurisdiction")
            val jInfo = if (jObj != null) {
                JurisdictionInfo(
                    state = if (jObj.isNull("state")) null else jObj.optString("state"),
                    city = if (jObj.isNull("city")) null else jObj.optString("city"),
                    confidence = try {
                        JurisdictionConfidence.valueOf(jObj.optString("confidence", "UNKNOWN"))
                    } catch (_: Exception) { JurisdictionConfidence.UNKNOWN }
                )
            } else JurisdictionInfo()

            val fArray = root.optJSONArray("facts")
            val factsList = mutableListOf<MatterFact>()
            if (fArray != null) {
                for (idx in 0 until fArray.length()) {
                    val fo = fArray.getJSONObject(idx)
                    val suppArray = fo.optJSONArray("supportedBy")
                    val suppList = mutableListOf<String>()
                    if (suppArray != null) {
                        for (s in 0 until suppArray.length()) suppList.add(suppArray.getString(s))
                    }
                    factsList.add(
                        MatterFact(
                            factId = fo.optString("factId", UUID.randomUUID().toString()),
                            slotName = if (fo.isNull("slotName")) null else fo.optString("slotName"),
                            statement = fo.optString("statement", ""),
                            source = try { FactSource.valueOf(fo.optString("source", "USER_STATED")) } catch (_: Exception) { FactSource.USER_STATED },
                            confidence = fo.optString("confidence", "STATED"),
                            supportedByEvidenceIds = suppList,
                            createdAt = fo.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            fun toStringList(arr: JSONArray?): List<String> {
                if (arr == null) return emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                return list
            }

            val tArray = root.optJSONArray("timeline")
            val timelineList = mutableListOf<TimelineEvent>()
            if (tArray != null) {
                for (idx in 0 until tArray.length()) {
                    val to = tArray.getJSONObject(idx)
                    timelineList.add(
                        TimelineEvent(
                            eventId = to.optString("eventId", UUID.randomUUID().toString()),
                            eventDate = if (to.isNull("eventDate")) null else to.optString("eventDate"),
                            description = to.optString("description", ""),
                            sourceFactId = if (to.isNull("sourceFactId")) null else to.optString("sourceFactId")
                        )
                    )
                }
            }

            val eArray = root.optJSONArray("evidence")
            val evidenceList = mutableListOf<MatterEvidence>()
            if (eArray != null) {
                for (idx in 0 until eArray.length()) {
                    val eo = eArray.getJSONObject(idx)
                    evidenceList.add(
                        MatterEvidence(
                            evidenceId = eo.optString("evidenceId", UUID.randomUUID().toString()),
                            objectKey = eo.optString("objectKey", ""),
                            docType = if (eo.isNull("docType")) null else eo.optString("docType"),
                            extractedText = if (eo.isNull("extractedText")) null else eo.optString("extractedText"),
                            classificationConfidence = if (eo.isNull("classificationConfidence")) null else eo.optDouble("classificationConfidence").toFloat(),
                            uploadedAt = eo.optLong("uploadedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val iArray = root.optJSONArray("legalIssues")
            val issueList = mutableListOf<LegalIssue>()
            if (iArray != null) {
                for (idx in 0 until iArray.length()) {
                    val io = iArray.getJSONObject(idx)
                    issueList.add(
                        LegalIssue(
                            issueId = io.optString("issueId", UUID.randomUUID().toString()),
                            title = io.optString("title", ""),
                            statutoryBasis = io.optString("statutoryBasis", ""),
                            confidence = io.optDouble("confidence", 0.9)
                        )
                    )
                }
            }

            val lArray = root.optJSONArray("applicableLaws")
            val lawsList = mutableListOf<ApplicableLaw>()
            if (lArray != null) {
                for (idx in 0 until lArray.length()) {
                    val lo = lArray.getJSONObject(idx)
                    lawsList.add(
                        ApplicableLaw(
                            act = lo.optString("act", ""),
                            section = lo.optString("section", ""),
                            currentnessStatus = lo.optString("currentnessStatus", "in_force"),
                            asOf = lo.optString("asOf", "2026-09")
                        )
                    )
                }
            }

            val escObj = root.optJSONObject("escalation")
            val escInfo = if (escObj != null) {
                EscalationInfo(
                    triggerReason = escObj.optString("triggerReason", ""),
                    recommendedPath = escObj.optString("recommendedPath", ""),
                    escalatedAt = escObj.optLong("escalatedAt", System.currentTimeMillis())
                )
            } else null

            return Matter(
                matterId = root.optString("matterId", UUID.randomUUID().toString()),
                userId = root.optString("userId", "guest_user"),
                title = root.optString("title", "Legal Matter"),
                status = root.optString("status", "active"),
                domain = root.optString("domain", "General Dispute"),
                jurisdiction = jInfo,
                facts = factsList,
                disputedFacts = toStringList(root.optJSONArray("disputedFacts")),
                timeline = timelineList,
                evidence = evidenceList,
                documents = toStringList(root.optJSONArray("documents")),
                legalIssues = issueList,
                applicableLaws = lawsList,
                caseLaw = toStringList(root.optJSONArray("caseLaw")),
                proceduralStage = root.optString("proceduralStage", "intake"),
                deadlines = toStringList(root.optJSONArray("deadlines")),
                missingInformation = toStringList(root.optJSONArray("missingInformation")),
                assumptions = toStringList(root.optJSONArray("assumptions")),
                options = toStringList(root.optJSONArray("options")),
                risks = toStringList(root.optJSONArray("risks")),
                nextActions = toStringList(root.optJSONArray("nextActions")),
                openQuestions = toStringList(root.optJSONArray("openQuestions")),
                escalation = escInfo,
                createdAt = root.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

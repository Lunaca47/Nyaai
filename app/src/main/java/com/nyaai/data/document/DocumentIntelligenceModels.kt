package com.nyaai.data.document

import com.nyaai.data.matter.MatterEvidence
import com.nyaai.data.matter.MatterFact
import com.nyaai.data.matter.TimelineEvent

enum class LegalDocType(val displayName: String) {
    RENTAL_AGREEMENT("Rental Agreement / Lease Deed"),
    LEGAL_NOTICE("Statutory Legal Demand Notice"),
    CHEQUE_RETURN_MEMO("Bank Cheque Return Memo"),
    FIR_POLICE_COMPLAINT("Police Complaint / First Information Report"),
    SALARY_SLIP("Salary Slip / Employment Record"),
    BANK_STATEMENT("Bank Account Statement"),
    CONSUMER_INVOICE("Tax Invoice / Purchase Receipt"),
    RTI_APPLICATION("Right to Information (RTI) Application"),
    GENERAL_LEGAL_DOCUMENT("General Legal Document")
}

data class DocClassificationResult(
    val docType: LegalDocType,
    val confidence: Float,
    val matchedSignals: List<String> = emptyList()
)

data class ExtractedDate(
    val dateString: String,
    val contextLabel: String
)

data class ExtractedAmount(
    val amountFormatted: String,
    val numericValue: Double,
    val contextLabel: String
)

data class ExtractedClause(
    val clauseType: String,
    val snippet: String
)

data class ExtractedEntities(
    val parties: Map<String, String> = emptyMap(),
    val dates: List<ExtractedDate> = emptyList(),
    val amounts: List<ExtractedAmount> = emptyList(),
    val clauses: List<ExtractedClause> = emptyList()
)

data class DocumentIntelligenceResult(
    val rawText: String,
    val sanitizedText: String,
    val classification: DocClassificationResult,
    val entities: ExtractedEntities,
    val isAdversarial: Boolean,
    val injectionThreats: List<String> = emptyList(),
    val derivedFacts: List<MatterFact> = emptyList(),
    val derivedTimeline: List<TimelineEvent> = emptyList(),
    val evidenceRecord: MatterEvidence
)

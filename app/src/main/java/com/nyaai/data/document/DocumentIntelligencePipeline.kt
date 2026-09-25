package com.nyaai.data.document

import com.nyaai.data.matter.FactSource
import com.nyaai.data.matter.MatterEvidence
import com.nyaai.data.matter.MatterFact
import com.nyaai.data.matter.TimelineEvent
import java.util.UUID

object DocumentIntelligencePipeline {

    private val CLASSIFICATION_RULES: Map<LegalDocType, List<String>> = mapOf(
        LegalDocType.RENTAL_AGREEMENT to listOf(
            "rental agreement", "lease agreement", "lease deed", "tenancy agreement",
            "lessor", "lessee", "landlord", "tenant", "security deposit", "monthly rent",
            "premises", "schedule property"
        ),
        LegalDocType.LEGAL_NOTICE to listOf(
            "legal notice", "statutory notice", "demand notice", "under instructions from my client",
            "speed post", "call upon you", "within 15 days", "failing which", "legal proceedings",
            "advocate"
        ),
        LegalDocType.CHEQUE_RETURN_MEMO to listOf(
            "return memo", "cheque return memo", "funds insufficient", "exceeds arrangement",
            "account closed", "drawer", "payee", "cheque no", "drawee bank", "dishonoured"
        ),
        LegalDocType.FIR_POLICE_COMPLAINT to listOf(
            "first information report", "police station", "fir no", "complainant",
            "accused", "general diary", "section 173", "cognizable", "sho", "sub-inspector"
        ),
        LegalDocType.SALARY_SLIP to listOf(
            "salary slip", "payslip", "basic pay", "hra", "pf contribution",
            "gross salary", "net pay", "employee code", "employee id", "ctc"
        ),
        LegalDocType.BANK_STATEMENT to listOf(
            "statement of account", "bank statement", "transaction date", "withdrawal",
            "deposit", "balance", "ifsc", "account number", "cr/dr", "upi/"
        ),
        LegalDocType.CONSUMER_INVOICE to listOf(
            "tax invoice", "bill of supply", "gstin", "invoice no",
            "warranty", "purchase order", "customer name", "mrp", "total amount", "order id"
        ),
        LegalDocType.RTI_APPLICATION to listOf(
            "right to information", "rti act", "public information officer",
            "pio", "information sought", "application fee", "section 6(1)"
        )
    )

    fun classify(text: String): DocClassificationResult {
        val lower = text.lowercase()
        var bestType = LegalDocType.GENERAL_LEGAL_DOCUMENT
        var maxScore = 0
        var bestSignals = emptyList<String>()

        for ((docType, keywords) in CLASSIFICATION_RULES) {
            val matched = keywords.filter { lower.contains(it) }
            if (matched.size > maxScore) {
                maxScore = matched.size
                bestType = docType
                bestSignals = matched
            }
        }

        val confidence = when {
            maxScore >= 4 -> 0.98f
            maxScore == 3 -> 0.90f
            maxScore == 2 -> 0.80f
            maxScore == 1 -> 0.65f
            else -> 0.40f
        }

        return DocClassificationResult(
            docType = bestType,
            confidence = confidence,
            matchedSignals = bestSignals
        )
    }

    fun extractEntities(text: String, docType: LegalDocType): ExtractedEntities {
        val parties = extractParties(text, docType)
        val dates = extractDates(text)
        val amounts = extractAmounts(text)
        val clauses = extractClauses(text)

        return ExtractedEntities(
            parties = parties,
            dates = dates,
            amounts = amounts,
            clauses = clauses
        )
    }

    private fun extractParties(text: String, docType: LegalDocType): Map<String, String> {
        val map = mutableMapOf<String, String>()

        when (docType) {
            LegalDocType.RENTAL_AGREEMENT -> {
                val landlordMatch = Regex("""(?i)(?:landlord|lessor)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (landlordMatch != null) map["landlord"] = landlordMatch.groupValues[1].trim()

                val tenantMatch = Regex("""(?i)(?:tenant|lessee)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (tenantMatch != null) map["tenant"] = tenantMatch.groupValues[1].trim()
            }
            LegalDocType.LEGAL_NOTICE -> {
                val toMatch = Regex("""(?i)\bTo\s*[:,\-]?\s*([A-Za-z0-9/&.\s]{3,40})(?:,|\n|\s{2})""").find(text)
                if (toMatch != null) map["recipient"] = toMatch.groupValues[1].trim()

                val fromMatch = Regex("""(?i)\bfrom\s+(?:my\s+client\s*)?[:\-]?\s*([A-Za-z0-9/&.\s]{3,40})(?:,|\n|\s{2})""").find(text)
                if (fromMatch != null) map["client"] = fromMatch.groupValues[1].trim()
            }
            LegalDocType.FIR_POLICE_COMPLAINT -> {
                val compMatch = Regex("""(?i)(?:complainant|informant)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (compMatch != null) map["complainant"] = compMatch.groupValues[1].trim()

                val accMatch = Regex("""(?i)(?:accused|suspect)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (accMatch != null) map["accused"] = accMatch.groupValues[1].trim()

                val psMatch = Regex("""(?i)police\s+station\s*[:\-]?\s*([A-Za-z\s]{3,30})(?:,|\n)""").find(text)
                if (psMatch != null) map["police_station"] = psMatch.groupValues[1].trim()
            }
            LegalDocType.CHEQUE_RETURN_MEMO -> {
                val drawerMatch = Regex("""(?i)(?:drawer|account\s+holder)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (drawerMatch != null) map["drawer"] = drawerMatch.groupValues[1].trim()

                val bankMatch = Regex("""(?i)(?:bank|branch)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (bankMatch != null) map["bank"] = bankMatch.groupValues[1].trim()
            }
            LegalDocType.SALARY_SLIP -> {
                val empMatch = Regex("""(?i)(?:employee\s+name|name)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (empMatch != null) map["employee"] = empMatch.groupValues[1].trim()

                val compMatch = Regex("""(?i)(?:company|employer)\s*[:\-]\s*([A-Za-z\s.]{3,40})(?:,|\n|\s{2})""").find(text)
                if (compMatch != null) map["employer"] = compMatch.groupValues[1].trim()
            }
            else -> {}
        }

        return map
    }

    private fun extractDates(text: String): List<ExtractedDate> {
        val list = mutableListOf<ExtractedDate>()
        val seenDates = mutableSetOf<String>()

        // 1. Numeric dates: 15/08/2026, 15-08-2026, 2026-08-15
        val numericDatePattern = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{2}-\d{2})\b""")
        for (match in numericDatePattern.findAll(text)) {
            val dateStr = match.value
            if (dateStr !in seenDates) {
                seenDates.add(dateStr)
                val label = detectDateContext(text, match.range.first)
                list.add(ExtractedDate(dateString = dateStr, contextLabel = label))
            }
        }

        // 2. Textual dates: 15th August 2026, 15 August 2026
        val textDatePattern = Regex("""\b\d{1,2}(?:st|nd|rd|th)?\s+(?:January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[,.\s]+\d{4}\b""", RegexOption.IGNORE_CASE)
        for (match in textDatePattern.findAll(text)) {
            val dateStr = match.value
            if (dateStr !in seenDates) {
                seenDates.add(dateStr)
                val label = detectDateContext(text, match.range.first)
                list.add(ExtractedDate(dateString = dateStr, contextLabel = label))
            }
        }

        return list.take(5)
    }

    private fun detectDateContext(text: String, dateStartIdx: Int): String {
        val start = (dateStartIdx - 40).coerceAtLeast(0)
        val context = text.substring(start, dateStartIdx).lowercase()
        return when {
            context.contains("dated") || context.contains("date of") -> "execution_date"
            context.contains("notice") -> "notice_date"
            context.contains("incident") || context.contains("accident") || context.contains("occurred") -> "incident_date"
            context.contains("cheque") -> "cheque_date"
            context.contains("expiry") || context.contains("valid until") -> "expiry_date"
            else -> "document_date"
        }
    }

    private fun extractAmounts(text: String): List<ExtractedAmount> {
        val list = mutableListOf<ExtractedAmount>()
        val seenAmounts = mutableSetOf<String>()

        val amountPattern = Regex("""(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        for (match in amountPattern.findAll(text)) {
            val formatted = match.value.trim()
            val rawNum = match.groupValues[1].replace(",", "").trim()
            val numValue = rawNum.toDoubleOrNull() ?: continue

            if (formatted !in seenAmounts && numValue > 0) {
                seenAmounts.add(formatted)
                val label = detectAmountContext(text, match.range.first)
                list.add(ExtractedAmount(amountFormatted = formatted, numericValue = numValue, contextLabel = label))
            }
        }

        return list.take(5)
    }

    private fun detectAmountContext(text: String, amountStartIdx: Int): String {
        val start = (amountStartIdx - 50).coerceAtLeast(0)
        val context = text.substring(start, amountStartIdx).lowercase()
        return when {
            context.contains("deposit") || context.contains("security") -> "security_deposit"
            context.contains("rent") -> "monthly_rent"
            context.contains("salary") || context.contains("wages") -> "salary_due"
            context.contains("cheque") || context.contains("dishonour") -> "cheque_amount"
            context.contains("refund") || context.contains("claimed") -> "dispute_amount"
            context.contains("damages") || context.contains("compensation") -> "compensation_claimed"
            else -> "monetary_amount"
        }
    }

    private fun extractClauses(text: String): List<ExtractedClause> {
        val clauses = mutableListOf<ExtractedClause>()

        // Lock-in clause
        val lockIn = Regex("""(?i)\b(?:lock[- ]in|minimum)\s*(?:period)?\s*(?:of)?\s*(\d+\s*(?:months?|years?))\b""").find(text)
        if (lockIn != null) {
            clauses.add(ExtractedClause("LOCK_IN_PERIOD", lockIn.value.trim()))
        }

        // Notice period clause
        val noticePeriod = Regex("""(?i)\b(?:notice\s*period|prior\s*notice)\s*(?:of)?\s*(\d+\s*(?:days?|months?))\b""").find(text)
        if (noticePeriod != null) {
            clauses.add(ExtractedClause("NOTICE_PERIOD", noticePeriod.value.trim()))
        }

        // Jurisdiction clause
        val jurisdiction = Regex("""(?i)(?:subject\s+to\s+the\s+exclusive\s+jurisdiction\s+of\s+courts\s+at|courts\s+(?:in|at))\s+([A-Za-z]+)""").find(text)
        if (jurisdiction != null) {
            clauses.add(ExtractedClause("EXCLUSIVE_JURISDICTION", jurisdiction.value.trim()))
        }

        // Interest clause
        val interest = Regex("""(?i)\binterest\s*(?:@|at\s+the\s+rate\s+of)?\s*(\d+(?:\.\d+)?\s*%\s*(?:p\.?a\.?|per\s+annum)?)\b""").find(text)
        if (interest != null) {
            clauses.add(ExtractedClause("INTEREST_RATE", interest.value.trim()))
        }

        return clauses
    }

    fun process(
        rawText: String,
        fileName: String = "scanned_doc_${System.currentTimeMillis()}"
    ): DocumentIntelligenceResult {
        // Step 1: Adversarial Prompt Injection Defense
        val defense = PromptInjectionDefense.scan(rawText)

        // Step 2: Document Classification
        val classification = classify(defense.sanitizedText)

        // Step 3: Entity & Clause Extraction
        val entities = extractEntities(defense.sanitizedText, classification.docType)

        // Step 4: Derive Structured MatterFacts
        val derivedFacts = mutableListOf<MatterFact>()

        entities.parties.forEach { (role, name) ->
            derivedFacts.add(
                MatterFact(
                    statement = "${classification.docType.displayName} party '$role' identified as: $name",
                    source = FactSource.DOCUMENT_EXTRACTED,
                    confidence = "HIGH"
                )
            )
        }

        entities.amounts.forEach { amt ->
            derivedFacts.add(
                MatterFact(
                    statement = "Identified ${amt.contextLabel.replace('_', ' ')}: ${amt.amountFormatted}",
                    source = FactSource.DOCUMENT_EXTRACTED,
                    confidence = "HIGH"
                )
            )
        }

        entities.clauses.forEach { clause ->
            derivedFacts.add(
                MatterFact(
                    statement = "Statutory/Contractual clause (${clause.clauseType}): ${clause.snippet}",
                    source = FactSource.DOCUMENT_EXTRACTED,
                    confidence = "HIGH"
                )
            )
        }

        // Derive Timeline Events
        val derivedTimeline = entities.dates.map { dt ->
            TimelineEvent(
                eventId = UUID.randomUUID().toString(),
                eventDate = dt.dateString,
                description = "Document recorded date for ${dt.contextLabel.replace('_', ' ')} (${classification.docType.displayName})",
                sourceFactId = "document_extracted"
            )
        }

        // Create MatterEvidence
        val evidenceRecord = MatterEvidence(
            objectKey = fileName,
            docType = classification.docType.displayName,
            extractedText = defense.safeEnvelope,
            classificationConfidence = classification.confidence,
            uploadedAt = System.currentTimeMillis()
        )

        return DocumentIntelligenceResult(
            rawText = rawText,
            sanitizedText = defense.sanitizedText,
            classification = classification,
            entities = entities,
            isAdversarial = defense.isAdversarial,
            injectionThreats = defense.threats,
            derivedFacts = derivedFacts,
            derivedTimeline = derivedTimeline,
            evidenceRecord = evidenceRecord
        )
    }
}

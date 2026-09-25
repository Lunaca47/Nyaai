package com.nyaai.data.verification

import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.TrainingExampleEntity

enum class GateAction {
    PASSED,
    ANNOTATED_REPEALED,
    REJECTED_UNGROUNDED,
    ANNOTATED_UNGROUNDED,
    ANNOTATED_MODEL_LAW
}

data class CitationVerificationResult(
    val isGrounded: Boolean,
    val groundedCitations: List<String>,
    val ungroundedCitations: List<String>,
    val repealedCitations: List<String>,
    val warnings: List<String>
)

data class GatedResponse(
    val gatedText: String,
    val action: GateAction,
    val result: CitationVerificationResult,
    val shouldFallback: Boolean
)

class CitationVerifier {

    companion object {
        private val REPEALED_STATUTES = mapOf(
            "indian penal code" to "Bharatiya Nyaya Sanhita, 2023 (BNS)",
            "ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS)",
            "code of criminal procedure" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)",
            "crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)",
            "indian evidence act" to "Bharatiya Sakshya Adhiniyam, 2023 (BSA)",
            "iea" to "Bharatiya Sakshya Adhiniyam, 2023 (BSA)"
        )

        private val REPEALED_REGEX = Regex(
            "\\b(Indian Penal Code|IPC|Code of Criminal Procedure|CrPC|Indian Evidence Act|IEA)\\b",
            RegexOption.IGNORE_CASE
        )

        private val SECTION_MIGRATIONS = mapOf(
            "ipc 420" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 318(4) (Cheating)",
            "section 420 ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 318(4) (Cheating)",
            "ipc 302" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 103(1) (Murder)",
            "section 302 ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 103(1) (Murder)",
            "ipc 376" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 64 (Rape)",
            "section 376 ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 64 (Rape)",
            "ipc 498a" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 85 / 86 (Cruelty to woman)",
            "section 498a ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 85 / 86 (Cruelty to woman)",
            "ipc 304a" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 106(1) (Death by negligence)",
            "section 304a ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 106(1) (Death by negligence)",
            "ipc 279" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 281 (Rash driving)",
            "section 279 ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 281 (Rash driving)",
            "ipc 323" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 115(2) (Voluntarily causing hurt)",
            "section 323 ipc" to "Bharatiya Nyaya Sanhita, 2023 (BNS) Section 115(2) (Voluntarily causing hurt)",
            "crpc 154" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 173 (FIR / Zero FIR)",
            "section 154 crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 173 (FIR / Zero FIR)",
            "crpc 156(3)" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 175(3) / 175(4) (Magistrate investigation)",
            "section 156(3) crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 175(3) / 175(4) (Magistrate investigation)",
            "crpc 161" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 180 (Witness statements)",
            "section 161 crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 180 (Witness statements)",
            "crpc 173" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 193 (Police chargesheet)",
            "section 173 crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 193 (Police chargesheet)",
            "crpc 437" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 480 (Bail)",
            "crpc 439" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 482 (High Court bail powers)",
            "iea 65b" to "Bharatiya Sakshya Adhiniyam, 2023 (BSA) Section 63 (Electronic records admissibility)",
            "section 65b iea" to "Bharatiya Sakshya Adhiniyam, 2023 (BSA) Section 63 (Electronic records admissibility)"
        )
    }

    fun verify(
        responseText: String,
        retrievedDocuments: List<DocumentEntity> = emptyList(),
        retrievedExamples: List<TrainingExampleEntity> = emptyList()
    ): CitationVerificationResult {
        val textLower = responseText.lowercase()

        // 1. Currentness Check: Check for repealed statutes cited without noting repeal
        val repealedMatches = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        REPEALED_REGEX.findAll(responseText).forEach { match ->
            val statute = match.value.lowercase()
            val replacement = REPEALED_STATUTES[statute]
            if (replacement != null) {
                // Check if text already clarifies transition to BNS/BNSS/BSA
                val mentionsNewAct = textLower.contains("bns") || textLower.contains("bnss") || textLower.contains("bsa") || textLower.contains("repealed")
                if (!mentionsNewAct) {
                    repealedMatches.add(match.value)
                    warnings.add("Caution: \"${match.value}\" is repealed. Governed by $replacement for offenses post July 1, 2024.")
                }
            }
        }

        // 2. Grounding Check: Extract section numbers or acts mentioned in response
        val citationPattern = Regex("\\b(?:Section|Article|Sec\\.?|Art\\.?)\\s+(\\d+[A-Za-z]*)", RegexOption.IGNORE_CASE)
        val citedSections = citationPattern.findAll(responseText).map { it.groupValues[1] }.distinct().toList()

        val allRetrievedText = buildString {
            retrievedDocuments.forEach { append(it.content).append(" ").append(it.sourcePath).append(" ") }
            retrievedExamples.forEach { append(it.question).append(" ").append(it.answer).append(" ").append(it.sourcePath).append(" ") }
        }

        val grounded = mutableListOf<String>()
        val ungrounded = mutableListOf<String>()

        for (section in citedSections) {
            if (allRetrievedText.contains(section, ignoreCase = true)) {
                grounded.add("Section $section")
            } else {
                ungrounded.add("Section $section")
            }
        }

        val isGrounded = ungrounded.isEmpty()

        return CitationVerificationResult(
            isGrounded = isGrounded,
            groundedCitations = grounded,
            ungroundedCitations = ungrounded,
            repealedCitations = repealedMatches.distinct(),
            warnings = warnings.distinct()
        )
    }

    fun enforceHardGate(
        responseText: String,
        retrievedDocuments: List<DocumentEntity> = emptyList(),
        retrievedExamples: List<TrainingExampleEntity> = emptyList()
    ): GatedResponse {
        val verification = verify(responseText, retrievedDocuments, retrievedExamples)

        // 1. Severe Ungrounded Hallucination check
        // If the model produced section citations, but NONE of them exist in retrieved sources/examples:
        if (verification.groundedCitations.isEmpty() && verification.ungroundedCitations.isNotEmpty()) {
            return GatedResponse(
                gatedText = responseText,
                action = GateAction.REJECTED_UNGROUNDED,
                result = verification,
                shouldFallback = true
            )
        }

        // 2. Mixed Ungrounded Hallucination check
        // If some citations are grounded but others are ungrounded/hallucinated,
        // do not let the hallucinated sections pass cleanly. Annotate them prominently.
        if (verification.ungroundedCitations.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine(responseText.trim())
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine("⚠️ **UNGROUNDED CITATION WARNING:**")
            sb.appendLine("The following cited provisions were not substantiated in the verified statutory context: ${verification.ungroundedCitations.joinToString(", ")}. Verify against official gazettes before relying on them.")

            return GatedResponse(
                gatedText = sb.toString(),
                action = GateAction.ANNOTATED_UNGROUNDED,
                result = verification,
                shouldFallback = false
            )
        }

        // 3. Grounded Repealed Statute Check & Auto-Annotation
        // Only reached if all citations are grounded (no ungrounded citations)
        if (verification.repealedCitations.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine(responseText.trim())
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine("⚠️ **STATUTORY CURRENTNESS & REPEAL NOTICE:**")
            sb.appendLine("The above guidance references colonial-era statutes that were officially repealed on July 1, 2024. Under Indian law effective July 1, 2024:")

            val lower = responseText.lowercase()
            val specificReplacements = mutableListOf<String>()
            for ((oldSec, newSec) in SECTION_MIGRATIONS) {
                if (lower.contains(oldSec)) {
                    specificReplacements.add("• **${oldSec.uppercase()}:** Replaced by **$newSec**")
                }
            }
            if (specificReplacements.isNotEmpty()) {
                specificReplacements.distinct().forEach { sb.appendLine(it) }
            } else {
                sb.appendLine("• **Indian Penal Code (IPC):** Replaced by **Bharatiya Nyaya Sanhita, 2023 (BNS)**.")
                sb.appendLine("• **Code of Criminal Procedure (CrPC):** Replaced by **Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)**.")
                sb.appendLine("• **Indian Evidence Act (IEA):** Replaced by **Bharatiya Sakshya Adhiniyam, 2023 (BSA)**.")
            }
            sb.append("⚡ Note: Please verify the date of offense to determine whether repealed or new Sanhita provisions govern.")

            return GatedResponse(
                gatedText = sb.toString(),
                action = GateAction.ANNOTATED_REPEALED,
                result = verification,
                shouldFallback = false
            )
        }

        // 4. Grounded Model Law Applicability Caveat (e.g. Model Tenancy Act, 2021)
        // Only reached if all citations are grounded and citations/sources are verified
        val lowerText = responseText.lowercase()
        val mentionsModelTenancy = lowerText.contains("model tenancy") || lowerText.contains("mta 2021")
        val mtaRetrievedText = buildString {
            retrievedDocuments.filter { it.content.contains("Model Tenancy", ignoreCase = true) || it.sourcePath.contains("mta", ignoreCase = true) }
                .forEach { append(it.content).append(" ").append(it.sourcePath).append(" ") }
            retrievedExamples.filter { it.answer.contains("Model Tenancy", ignoreCase = true) || it.sourcePath.contains("mta", ignoreCase = true) }
                .forEach { append(it.question).append(" ").append(it.answer).append(" ").append(it.sourcePath).append(" ") }
        }
        val hasGroundedMta = mentionsModelTenancy && verification.groundedCitations.any { citation ->
            val num = citation.replace(Regex("[^0-9]"), "")
            (citation.contains("Model Tenancy", ignoreCase = true) || citation.contains("MTA", ignoreCase = true)) ||
            (num.isNotEmpty() && mtaRetrievedText.contains(num))
        }

        if (hasGroundedMta && !lowerText.contains("model law") && !lowerText.contains("state adoption") && !lowerText.contains("not uniformly adopted")) {
            val sb = StringBuilder()
            sb.appendLine(responseText.trim())
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine("⚠️ **STATUTORY APPLICABILITY CAVEAT (MODEL LAW):**")
            sb.appendLine("The Model Tenancy Act, 2021 is a non-binding model framework circulated to States under Entry 18 of the State List. It does not automatically apply statewide unless enacted or adopted by your State Legislature. Existing State rent control legislation (e.g., Delhi Rent Control Act 1958, Maharashtra Rent Control Act 1999) or the Transfer of Property Act, 1882 governs in states that have not adopted the MTA.")

            return GatedResponse(
                gatedText = sb.toString(),
                action = GateAction.ANNOTATED_MODEL_LAW,
                result = verification,
                shouldFallback = false
            )
        }

        // 5. Clean pass
        return GatedResponse(
            gatedText = responseText,
            action = GateAction.PASSED,
            result = verification,
            shouldFallback = false
        )
    }
}

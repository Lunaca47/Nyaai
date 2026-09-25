package com.nyaai.data.verification

import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.TrainingExampleEntity

enum class GateAction {
    PASSED,
    ANNOTATED_REPEALED,
    REJECTED_UNGROUNDED,
    ANNOTATED_UNGROUNDED,
    ANNOTATED_MODEL_LAW,
    ANNOTATED_SUPERSEDED_PRECEDENT
}

data class CitationVerificationResult(
    val isGrounded: Boolean,
    val groundedCitations: List<String>,
    val ungroundedCitations: List<String>,
    val repealedCitations: List<String>,
    val warnings: List<String>,
    val supersededPrecedents: List<String> = emptyList()
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
            "crpc 70" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 72 (Form of warrant of arrest and duration)",
            "section 70 crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 72 (Form of warrant of arrest and duration)",
            "crpc 71" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 73 (Power to direct security to be taken upon warrant)",
            "section 71 crpc" to "Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) Section 73 (Power to direct security to be taken upon warrant)",
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

        // Case Law Precedent Extraction: e.g. "Lalita Kumari v. Govt. of U.P." or "Dr. Subhash Kashinath Mahajan v. State of Maharashtra, (2018) 6 SCC 454"
        val casePattern = Regex(
            "\\b([A-Z][A-Za-z0-9\\.\\'\\s]{1,45}?\\s+(?:v\\.|vs\\.)\\s+[A-Z][A-Za-z0-9\\.\\'\\s]{1,45}?(?:,\\s*(?:\\(\\d{4}\\)|\\d{4})\\s+[A-Za-z0-9\\s\\(\\)]+)?)(?=[,\\.\\n;]|$)",
            RegexOption.IGNORE_CASE
        )
        val citedCases = casePattern.findAll(responseText).map { it.groupValues[1].trim() }.distinct().toList()

        val allRetrievedText = buildString {
            retrievedDocuments.forEach { append(it.content).append(" ").append(it.sourcePath).append(" ") }
            retrievedExamples.forEach { append(it.question).append(" ").append(it.answer).append(" ").append(it.sourcePath).append(" ") }
        }
        val allRetrievedLower = allRetrievedText.lowercase()

        val grounded = mutableListOf<String>()
        val ungrounded = mutableListOf<String>()
        val supersededPrecedents = mutableListOf<String>()

        for (section in citedSections) {
            if (allRetrievedText.contains(section, ignoreCase = true)) {
                grounded.add("Section $section")
            } else {
                ungrounded.add("Section $section")
            }
        }

        for (caseCite in citedCases) {
            var p1 = caseCite.split(Regex("\\s+(?:v\\.|vs\\.)\\s+", RegexOption.IGNORE_CASE)).firstOrNull() ?: ""
            p1 = p1.replace(Regex("^(?:according\\s+to|as\\s+held\\s+in|in|per|see|vide|under)\\s+", RegexOption.IGNORE_CASE), "").trim()
            val tokens = Regex("[A-Za-z]+").findAll(p1).map { it.value.lowercase() }
                .filter { it !in setOf("dr", "state", "union", "india", "govt", "of", "the", "and", "according", "to") && it.length > 2 }
                .toList()

            val isCaseGrounded = if (tokens.isNotEmpty()) {
                tokens.all { allRetrievedLower.contains(it) }
            } else {
                allRetrievedLower.contains(caseCite.lowercase())
            }

            if (isCaseGrounded) {
                grounded.add(caseCite)
                val mentionsSuperseded = allRetrievedText.contains("SUPERSEDED_BY_STATUTE", ignoreCase = true) ||
                        allRetrievedText.contains("OVERRULED", ignoreCase = true) ||
                        allRetrievedText.contains("MODIFIED", ignoreCase = true) ||
                        allRetrievedText.contains("superseded by parliament", ignoreCase = true)

                if (mentionsSuperseded && tokens.any { allRetrievedLower.contains(it) }) {
                    supersededPrecedents.add(caseCite)
                    warnings.add("Caution: Case precedent \"$caseCite\" has been superseded by statute or overruled by subsequent larger Bench decision.")
                }
            } else {
                ungrounded.add(caseCite)
                warnings.add("Citation \"$caseCite\" is ungrounded in retrieved context.")
            }
        }

        val isGrounded = ungrounded.isEmpty()

        return CitationVerificationResult(
            isGrounded = isGrounded,
            groundedCitations = grounded,
            ungroundedCitations = ungrounded,
            repealedCitations = repealedMatches.distinct(),
            warnings = warnings.distinct(),
            supersededPrecedents = supersededPrecedents.distinct()
        )
    }

    fun enforceHardGate(
        responseText: String,
        retrievedDocuments: List<DocumentEntity> = emptyList(),
        retrievedExamples: List<TrainingExampleEntity> = emptyList()
    ): GatedResponse {
        val verification = verify(responseText, retrievedDocuments, retrievedExamples)

        // 1. Severe Ungrounded Hallucination check
        // If the model produced citations, but NONE of them exist in retrieved sources/examples:
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
            sb.appendLine("The following cited provisions or precedents were not substantiated in the verified statutory context: ${verification.ungroundedCitations.joinToString(", ")}. Verify against official gazettes before relying on them.")

            return GatedResponse(
                gatedText = sb.toString(),
                action = GateAction.ANNOTATED_UNGROUNDED,
                result = verification,
                shouldFallback = false
            )
        }

        // 3. Grounded Superseded Precedent Check & Auto-Annotation
        if (verification.supersededPrecedents.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine(responseText.trim())
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine("⚠️ **SUPERSEDED PRECEDENT NOTICE:**")
            sb.appendLine("The above guidance references judicial precedent that has been superseded by legislative enactment or recalled by the Supreme Court:")

            val allRetrieved = buildString {
                retrievedDocuments.forEach { append(it.content).append(" ") }
                retrievedExamples.forEach { append(it.answer).append(" ") }
            }

            for (supCase in verification.supersededPrecedents) {
                if (supCase.contains("Mahajan", ignoreCase = true) || allRetrieved.contains("18A")) {
                    sb.appendLine("• **Dr. Subhash Kashinath Mahajan (2018):** Superseded by Parliament via Section 18A of the Scheduled Castes and the Scheduled Tribes (Prevention of Atrocities) Amendment Act, 2018 (constitutionality upheld in *Prathvi Raj Chauhan*, (2020) 4 SCC 727; directions recalled in (2020) 4 SCC 761).")
                } else {
                    sb.appendLine("• **$supCase:** Superseded or overruled by subsequent statutory enactment or larger Bench ruling.")
                }
            }
            sb.append("⚡ Note: Do not rely on superseded ratios for legal filings without citing current statutory amendments and review judgments.")

            return GatedResponse(
                gatedText = sb.toString(),
                action = GateAction.ANNOTATED_SUPERSEDED_PRECEDENT,
                result = verification,
                shouldFallback = false
            )
        }

        // 4. Grounded Repealed Statute Check & Auto-Annotation
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

        // 5. Grounded Model Law Applicability Caveat (e.g. Model Tenancy Act, 2021)
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

        // 6. Clean pass
        return GatedResponse(
            gatedText = responseText,
            action = GateAction.PASSED,
            result = verification,
            shouldFallback = false
        )
    }
}

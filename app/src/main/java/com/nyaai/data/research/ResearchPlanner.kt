package com.nyaai.data.research

import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.TrainingExampleEntity
import com.nyaai.data.matter.Matter
import com.nyaai.data.retrieval.LegalRetrievalService
import com.nyaai.data.verification.CitationVerifier

enum class ResearchSubIssueType(val title: String) {
    SUBSTANTIVE_ELEMENTS("Substantive Legal Provisions & Supporting Precedents"),
    PROCEDURAL_FORUM("Competent Judicial Forum & Pecuniary Jurisdiction"),
    LIMITATION_PERIOD("Statutory Limitation Periods & Deadlines"),
    PRE_LITIGATION_DEMAND("Mandatory Pre-Litigation Notice Requirements"),
    INTERIM_RELIEF("Interim & Emergency Injunctions / Orders")
}

data class ResearchSubIssue(
    val issueType: ResearchSubIssueType,
    val subQuery: String,
    val targetStatutes: List<String>,
    val supportingPrecedents: List<String> = emptyList()
)

data class SubIssueFinding(
    val subIssue: ResearchSubIssue,
    val retrievedDocs: List<DocumentEntity>,
    val retrievedExamples: List<TrainingExampleEntity>,
    val synthesisText: String,
    val supportingPrecedents: List<String> = emptyList()
)

data class LegalAssessmentReport(
    val matterTitle: String,
    val domain: String,
    val jurisdiction: String?,
    val findings: List<SubIssueFinding>,
    val synthesizedAssessmentMemo: String,
    val isVerified: Boolean,
    val verifiedStatutes: List<String>,
    val citedPrecedents: List<String> = emptyList()
)

class ResearchPlanner(
    private val retrievalService: LegalRetrievalService,
    private val citationVerifier: CitationVerifier = CitationVerifier()
) {

    fun decomposeQuery(query: String, domain: String = "general_dispute"): List<ResearchSubIssue> {
        val qLower = query.lowercase()

        return when {
            domain.contains("tenan", ignoreCase = true) || qLower.contains("landlord") || qLower.contains("tenant") || qLower.contains("rent") -> {
                listOf(
                    ResearchSubIssue(
                        ResearchSubIssueType.SUBSTANTIVE_ELEMENTS,
                        "unlawful eviction criminal trespass BNS 329 criminal breach of trust BNS 316",
                        listOf("Bharatiya Nyaya Sanhita 2023", "Transfer of Property Act 1882"),
                        listOf("State of Haryana v. Bhajan Lal, 1992 Supp (1) SCC 335")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PROCEDURAL_FORUM,
                        "Section 6 Specific Relief Act civil court JMFC Section 175 BNSS",
                        listOf("Specific Relief Act 1963", "BNSS 2023")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.LIMITATION_PERIOD,
                        "limitation 6 months dispossession Section 6 Specific Relief Act",
                        listOf("Limitation Act 1963", "Specific Relief Act 1963")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PRE_LITIGATION_DEMAND,
                        "15 days legal notice determine lease Section 106 Transfer Property Act",
                        listOf("Transfer of Property Act 1882")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.INTERIM_RELIEF,
                        "Order 39 Rules 1 2 CPC temporary mandatory injunction unlock premises",
                        listOf("Code of Civil Procedure 1908")
                    )
                )
            }

            domain.contains("cheque", ignoreCase = true) || qLower.contains("cheque") || qLower.contains("bounce") -> {
                listOf(
                    ResearchSubIssue(
                        ResearchSubIssueType.SUBSTANTIVE_ELEMENTS,
                        "Section 138 Negotiable Instruments Act dishonour funds insufficient debt",
                        listOf("Negotiable Instruments Act 1881"),
                        listOf(
                            "M/s Meters and Instruments Pvt. Ltd. v. Kanchan Mehta, (2018) 1 SCC 560",
                            "P. Mohanraj v. Shah Brothers Ispat Pvt. Ltd., (2021) 6 SCC 258"
                        )
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PROCEDURAL_FORUM,
                        "Judicial Magistrate First Class complaint Section 142 NI Act territorial jurisdiction",
                        listOf("Negotiable Instruments Act 1881", "BNSS 2023")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.LIMITATION_PERIOD,
                        "30 days notice 15 days payment 30 days filing Section 138 142 NI Act",
                        listOf("Negotiable Instruments Act 1881")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PRE_LITIGATION_DEMAND,
                        "15 days statutory legal demand notice Section 138(b) Speed Post delivery",
                        listOf("Negotiable Instruments Act 1881")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.INTERIM_RELIEF,
                        "Section 143A NI Act 20 percent interim compensation pre-summoning",
                        listOf("Negotiable Instruments Act 1881")
                    )
                )
            }

            domain.contains("consumer", ignoreCase = true) || qLower.contains("consumer") || qLower.contains("defective") || qLower.contains("refund") -> {
                listOf(
                    ResearchSubIssue(
                        ResearchSubIssueType.SUBSTANTIVE_ELEMENTS,
                        "defect in goods deficiency in service product liability Section 84 85 CPA 2019",
                        listOf("Consumer Protection Act 2019"),
                        listOf("Arjun Panditrao Khotkar v. Kailash Kushanrao Gorantyal, (2020) 7 SCC 1")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PROCEDURAL_FORUM,
                        "District Consumer Commission Section 35 State Commission Section 47 e-Daakhil",
                        listOf("Consumer Protection Act 2019")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.LIMITATION_PERIOD,
                        "2 years limitation period Section 69 Consumer Protection Act",
                        listOf("Consumer Protection Act 2019")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PRE_LITIGATION_DEMAND,
                        "15 days legal notice National Consumer Helpline 1915 e-commerce grievance officer",
                        listOf("Consumer Protection Act 2019", "E-Commerce Rules 2020")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.INTERIM_RELIEF,
                        "Section 38(8) CPA interim order replacement refund compensation punitive damages",
                        listOf("Consumer Protection Act 2019")
                    )
                )
            }

            domain.contains("employ", ignoreCase = true) || domain.contains("salary", ignoreCase = true) || qLower.contains("salary") || qLower.contains("wages") -> {
                listOf(
                    ResearchSubIssue(
                        ResearchSubIssueType.SUBSTANTIVE_ELEMENTS,
                        "unpaid wages Section 17 Code on Wages 2019 gratuity Section 4 7 Gratuity Act",
                        listOf("Code on Wages 2019", "Payment of Gratuity Act 1972"),
                        listOf("Common Cause v. Union of India, (2018) 5 SCC 1")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PROCEDURAL_FORUM,
                        "Assistant Labor Commissioner conciliation Section 33C(2) Industrial Disputes Act",
                        listOf("Industrial Disputes Act 1947", "Code on Wages 2019")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.LIMITATION_PERIOD,
                        "3 years limitation wage recovery 30 days gratuity payment",
                        listOf("Code on Wages 2019", "Payment of Gratuity Act 1972")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PRE_LITIGATION_DEMAND,
                        "15 days advocate demand notice to directors Full Final settlement",
                        listOf("Code on Wages 2019")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.INTERIM_RELIEF,
                        "Controlling Authority 10 percent compound interest Revenue Recovery Certificate RRC Collector",
                        listOf("Payment of Gratuity Act 1972")
                    )
                )
            }

            domain.contains("domestic", ignoreCase = true) || qLower.contains("domestic violence") || qLower.contains("dowry") -> {
                listOf(
                    ResearchSubIssue(
                        ResearchSubIssueType.SUBSTANTIVE_ELEMENTS,
                        "domestic violence physical emotional economic Section 3 DV Act cruelty BNS 85",
                        listOf("Protection of Women from Domestic Violence Act 2005", "BNS 2023"),
                        listOf(
                            "Social Action Forum for Manav Adhikar v. Union of India, (2018) 10 SCC 443",
                            "Arnesh Kumar v. State of Bihar, (2014) 8 SCC 273"
                        )
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PROCEDURAL_FORUM,
                        "Section 12 DV application JMFC MM Protection Officer Domestic Incident Report DIR",
                        listOf("Protection of Women from Domestic Violence Act 2005")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.LIMITATION_PERIOD,
                        "continuing cause of action 30 days summons returnable",
                        listOf("Protection of Women from Domestic Violence Act 2005")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PRE_LITIGATION_DEMAND,
                        "Domestic Incident Report Form I 181 Women Helpline Mahila Thana",
                        listOf("Protection of Women from Domestic Violence Act 2005")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.INTERIM_RELIEF,
                        "Section 23 ex-parte interim orders Section 18 protection Section 19 residence Section 144 BNSS maintenance",
                        listOf("Protection of Women from Domestic Violence Act 2005", "BNSS 2023")
                    )
                )
            }

            else -> {
                listOf(
                    ResearchSubIssue(
                        ResearchSubIssueType.SUBSTANTIVE_ELEMENTS,
                        "$query substantive offenses civil criminal liability BNS 2023",
                        listOf("Bharatiya Nyaya Sanhita 2023", "Civil Law"),
                        listOf(
                            "Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1",
                            "D.K. Basu v. State of West Bengal, (1997) 1 SCC 416"
                        )
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PROCEDURAL_FORUM,
                        "$query competent court jurisdiction Section 173 175 BNSS District Court",
                        listOf("BNSS 2023", "Code of Civil Procedure 1908")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.LIMITATION_PERIOD,
                        "$query limitation clock Limitation Act 1963",
                        listOf("Limitation Act 1963")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.PRE_LITIGATION_DEMAND,
                        "$query formal 15 days advocate demand notice Speed Post",
                        listOf("Special Acts")
                    ),
                    ResearchSubIssue(
                        ResearchSubIssueType.INTERIM_RELIEF,
                        "$query interim injunction Order 39 CPC Section 175(4) BNSS",
                        listOf("Code of Civil Procedure 1908", "BNSS 2023")
                    )
                )
            }
        }
    }

    suspend fun executeResearch(
        matterTitle: String,
        query: String,
        domain: String = "general_dispute",
        jurisdictionState: String? = null
    ): LegalAssessmentReport {
        val subIssues = decomposeQuery(query, domain)
        val findings = mutableListOf<SubIssueFinding>()
        val allStatutes = mutableSetOf<String>()
        val allPrecedents = mutableSetOf<String>()

        for (issue in subIssues) {
            val retrievalResult = retrievalService.retrieve(
                query = issue.subQuery,
                targetJurisdiction = jurisdictionState
            )
            val docs = retrievalResult.documents
            val examples = retrievalResult.uniqueTrainingMatches
            issue.targetStatutes.forEach { allStatutes.add(it) }
            issue.supportingPrecedents.forEach { allPrecedents.add(it) }

            val findingText = synthesizeSubIssueFinding(issue, docs, examples)
            findings.add(
                SubIssueFinding(
                    subIssue = issue,
                    retrievedDocs = docs,
                    retrievedExamples = examples,
                    synthesisText = findingText,
                    supportingPrecedents = issue.supportingPrecedents
                )
            )
        }

        val memo = buildSynthesizedMemo(matterTitle, domain, jurisdictionState, findings)
        val verification = citationVerifier.verify(
            responseText = memo,
            retrievedDocuments = findings.flatMap { it.retrievedDocs }
        )

        return LegalAssessmentReport(
            matterTitle = matterTitle,
            domain = domain,
            jurisdiction = jurisdictionState,
            findings = findings,
            synthesizedAssessmentMemo = memo,
            isVerified = verification.isGrounded,
            verifiedStatutes = allStatutes.toList(),
            citedPrecedents = allPrecedents.toList()
        )
    }

    private fun synthesizeSubIssueFinding(
        issue: ResearchSubIssue,
        docs: List<DocumentEntity>,
        examples: List<TrainingExampleEntity>
    ): String {
        return buildString {
            append("• **${issue.issueType.title}:** ")
            if (examples.isNotEmpty()) {
                append(examples.first().answer.take(200).trim())
            } else if (docs.isNotEmpty()) {
                append(docs.first().content.take(200).trim())
            } else {
                append("Governed by ${issue.targetStatutes.joinToString(", ")}. Procedural prerequisites must be strictly adhered to.")
            }
        }
    }

    private fun buildSynthesizedMemo(
        matterTitle: String,
        domain: String,
        jurisdictionState: String?,
        findings: List<SubIssueFinding>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("🏛️ **SENIOR ADVOCATE MULTI-HOP LEGAL ASSESSMENT MEMO**")
        sb.appendLine("**Matter Title:** $matterTitle")
        sb.appendLine("**Legal Domain:** $domain")
        if (jurisdictionState != null) {
            sb.appendLine("**Jurisdiction:** $jurisdictionState")
        }
        sb.appendLine("**Codex Baseline:** BNS 2023 • BNSS 2023 • BSA 2023 • Special Statutory Enactments")
        sb.appendLine()
        sb.appendLine("📋 **MULTI-DIMENSIONAL LEGAL ANALYSIS:**")

        findings.forEach { finding ->
            sb.appendLine("### ${finding.subIssue.issueType.title}")
            sb.appendLine(finding.synthesisText)
            sb.appendLine("**Key Authorities:** ${finding.subIssue.targetStatutes.joinToString(", ")}")
            if (finding.supportingPrecedents.isNotEmpty()) {
                sb.appendLine("**Supporting Precedents:** ${finding.supportingPrecedents.joinToString(" • ")}")
            }
            sb.appendLine()
        }

        sb.appendLine("⚖️ **STRATEGIC ADVOCATE DIRECTIVE:**")
        sb.appendLine("Maintain strict compliance with pre-litigation notice periods and statutory limitation deadlines. Ensure all electronic and postal consignment evidence is preserved under Section 63 BSA 2023.")
        return sb.toString().trim()
    }
}

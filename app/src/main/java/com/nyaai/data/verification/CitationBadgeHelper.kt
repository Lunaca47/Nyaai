package com.nyaai.data.verification

import com.nyaai.data.matter.ApplicableLaw

enum class CitationBadgeType {
    PASSED,
    ANNOTATED_SUPERSEDED_PRECEDENT,
    ANNOTATED_REPEALED,
    REJECTED_UNGROUNDED
}

data class CitationBadge(
    val type: CitationBadgeType,
    val label: String,
    val note: String
)

object CitationBadgeHelper {

    private val VERIFIED_CORPUS_MARKERS = listOf(
        "lalita kumari", "arnesh kumar", "d.k. basu", "dk basu", "satender", "prathvi raj chauhan",
        "common cause", "navtej", "joseph shine", "puttaswamy", "shayara bano",
        "social action", "manav adhikar", "arjun panditrao", "khotkar", "shafhi mohammad",
        "bhajan lal", "meters and instruments", "mohanraj", "laxmi", "aparna bhat",
        "swapnil tripathi", "anuradha bhasin"
    )

    fun evaluatePrecedentBadge(citation: String): CitationBadge {
        val lower = citation.lowercase()
        if (lower.contains("mahajan") || lower.contains("subhash")) {
            return CitationBadge(
                type = CitationBadgeType.ANNOTATED_SUPERSEDED_PRECEDENT,
                label = "ANNOTATED_SUPERSEDED_PRECEDENT",
                note = "Superseded by Parliament via Section 18A SC/ST Act; directions recalled by Supreme Court in (2020) 4 SCC 761."
            )
        }

        if (VERIFIED_CORPUS_MARKERS.any { lower.contains(it) }) {
            return CitationBadge(
                type = CitationBadgeType.PASSED,
                label = "PASSED",
                note = "Good Law • Grounded Supreme Court Landmark Precedent"
            )
        }

        return CitationBadge(
            type = CitationBadgeType.REJECTED_UNGROUNDED,
            label = "REJECTED_UNGROUNDED",
            note = "Ungrounded Citation: Precedent not verified in Supreme Court corpus. Verify independently before filing."
        )
    }

    fun evaluateStatuteBadge(law: ApplicableLaw): CitationBadge {
        return when (law.currentnessStatus.lowercase()) {
            "in_force", "valid", "active" -> CitationBadge(
                type = CitationBadgeType.PASSED,
                label = "PASSED",
                note = "Currentness confirmed as of: ${law.asOf}"
            )
            "repealed" -> CitationBadge(
                type = CitationBadgeType.ANNOTATED_REPEALED,
                label = "ANNOTATED_REPEALED",
                note = "Colonial statute repealed July 1, 2024. Governed by BNS/BNSS/BSA."
            )
            else -> CitationBadge(
                type = CitationBadgeType.REJECTED_UNGROUNDED,
                label = "REJECTED_UNGROUNDED",
                note = "Status: ${law.currentnessStatus} (as of: ${law.asOf})"
            )
        }
    }
}

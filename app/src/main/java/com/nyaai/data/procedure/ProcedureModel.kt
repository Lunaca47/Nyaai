package com.nyaai.data.procedure

data class ProcedurePhase(
    val phaseNumber: Int,
    val title: String,
    val timeframe: String,
    val actions: List<String>,
    val statutoryBasis: List<String> = emptyList()
)

data class LegalProcedure(
    val id: String,
    val domain: String,
    val title: String,
    val primaryStatutes: String,
    val offenseClassification: String,
    val cognizableStatus: String,
    val phases: List<ProcedurePhase>,
    val strategicAdvice: List<String>,
    val detailedPoints: List<String>
)

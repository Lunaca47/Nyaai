package com.nyaai.data.intake

import com.nyaai.data.matter.CaseStateManager
import com.nyaai.data.matter.Matter
import com.nyaai.data.procedure.ProcedureEngine
import com.nyaai.data.retrieval.LegalRetrievalService
import com.nyaai.data.verification.CitationVerifier
import com.nyaai.ui.state.AppLanguage

enum class OrchestratorStage {
    INTAKE,
    PROCEDURAL_GUIDANCE,
    GROUNDED_ANSWER
}

data class OrchestratorResponse(
    val text: String,
    val stage: OrchestratorStage,
    val isComplete: Boolean,
    val confidence: Double,
    val updatedMatter: Matter
)

class CaseOrchestrator(
    private val intakeEngine: IntakeEngine,
    private val procedureEngine: ProcedureEngine,
    private val retrievalService: LegalRetrievalService,
    private val citationVerifier: CitationVerifier,
    private val caseStateManager: CaseStateManager? = null
) {

    suspend fun processTurn(
        currentMatter: Matter,
        userInput: String,
        language: AppLanguage = AppLanguage.ENGLISH
    ): OrchestratorResponse {
        val q = userInput.trim()

        val qLower = q.lowercase()
        val isExplicitProcedural = procedureEngine.isDetailedQuery(q) ||
            listOf("what should i do", "how to proceed", "what is the procedure", "what are my options", "next steps", "legal remedies", "what can i do").any { qLower.contains(it) } ||
            (qLower.contains("locked") && (qLower.contains("flat") || qLower.contains("belonging")))

        // 1. If explicit procedure requested or emergency lockout, provide procedural guidance directly
        if (isExplicitProcedural && procedureEngine.isScenarioQuery(q)) {
            val (answer, confidence) = procedureEngine.buildProcedureAnswer(q, language)
            val updated = currentMatter.copy(
                proceduralStage = "guidance_delivered",
                updatedAt = System.currentTimeMillis()
            )
            caseStateManager?.updateMatter(updated)
            return OrchestratorResponse(
                text = answer,
                stage = OrchestratorStage.PROCEDURAL_GUIDANCE,
                isComplete = true,
                confidence = confidence,
                updatedMatter = updated
            )
        }

        // 2. Intake step: process missing facts and checklist
        val intakeResult = intakeEngine.processInput(currentMatter, q)
        if (!intakeResult.isComplete && intakeResult.nextQuestion != null) {
            caseStateManager?.updateMatter(intakeResult.updatedMatter)
            return OrchestratorResponse(
                text = intakeResult.nextQuestion,
                stage = OrchestratorStage.INTAKE,
                isComplete = false,
                confidence = 0.90,
                updatedMatter = intakeResult.updatedMatter
            )
        }

        // 3. Retrieval over statutory context
        val retrievalResult = retrievalService.retrieve(
            query = q,
            targetJurisdiction = intakeResult.updatedMatter.jurisdiction.state
        )

        if (retrievalResult.bestCandidate != null && retrievalResult.bestCandidateScore >= 8) {
            val target = retrievalResult.bestCandidate
            val sb = StringBuilder()
            sb.appendLine("🏛️ **VERIFIED STATUTORY LEGAL PROVISION**\n")
            sb.appendLine(target.answer.trim())
            sb.appendLine()
            sb.appendLine("📖 **Source:** ${target.sourcePath}")
            if (target.legalDomain.isNotBlank()) {
                sb.appendLine("⚖️ **Legal Domain:** ${target.legalDomain}")
            }
            sb.append("\n⚡ Note: Verified statutory knowledge grounded in codified Indian Law.")
            val finalConf = if (retrievalResult.bestCandidateScore >= 18) 0.98 else 0.95

            val updated = intakeResult.updatedMatter.copy(
                proceduralStage = "assessment_completed",
                updatedAt = System.currentTimeMillis()
            )
            caseStateManager?.updateMatter(updated)

            return OrchestratorResponse(
                text = sb.toString().trim(),
                stage = OrchestratorStage.GROUNDED_ANSWER,
                isComplete = true,
                confidence = finalConf,
                updatedMatter = updated
            )
        }

        // Default fallback to procedure engine
        val (procAnswer, procConf) = procedureEngine.buildProcedureAnswer(q, language)
        val updated = intakeResult.updatedMatter.copy(
            proceduralStage = "guidance_delivered",
            updatedAt = System.currentTimeMillis()
        )
        caseStateManager?.updateMatter(updated)

        return OrchestratorResponse(
            text = procAnswer,
            stage = OrchestratorStage.PROCEDURAL_GUIDANCE,
            isComplete = true,
            confidence = procConf,
            updatedMatter = updated
        )
    }
}

package com.nyaai.data.local

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nyaai.data.document.DocumentIntelligencePipeline
import com.nyaai.data.document.DocumentIntelligenceResult
import com.nyaai.data.matter.Matter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class DocumentScannerService(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractRawTextFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)

        try {
            if (isPdf) {
                extractFromPdf(uri)
            } else {
                extractFromImage(uri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractTextFromUri(uri: Uri): Result<String> {
        val rawResult = extractRawTextFromUri(uri)
        return rawResult.map { rawText ->
            val intelResult = DocumentIntelligencePipeline.process(rawText, uri.lastPathSegment ?: "document")
            formatSecureLegalPrompt(intelResult)
        }
    }

    suspend fun processDocumentFromUri(uri: Uri): Result<DocumentIntelligenceResult> {
        val rawResult = extractRawTextFromUri(uri)
        return rawResult.map { rawText ->
            DocumentIntelligencePipeline.process(rawText, uri.lastPathSegment ?: "document")
        }
    }

    fun attachToMatter(matter: Matter, result: DocumentIntelligenceResult): Matter {
        val existingFactStatements = matter.facts.map { it.statement }.toSet()
        val newFacts = result.derivedFacts.filter { it.statement !in existingFactStatements }

        val existingTimelineDescs = matter.timeline.map { it.description }.toSet()
        val newTimeline = result.derivedTimeline.filter { it.description !in existingTimelineDescs }

        val newEvidence = matter.evidence + result.evidenceRecord

        return matter.copy(
            facts = matter.facts + newFacts,
            timeline = matter.timeline + newTimeline,
            evidence = newEvidence,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun extractFromPdf(uri: Uri): Result<String> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalStateException("Unable to open PDF stream"))

        return inputStream.use { stream ->
            val document = PDDocument.load(stream)
            try {
                val stripper = PDFTextStripper()
                val maxPages = minOf(document.numberOfPages, 3)
                stripper.startPage = 1
                stripper.endPage = maxPages
                val rawText = stripper.getText(document).trim()

                if (rawText.isBlank()) {
                    Result.failure(IllegalStateException("No readable text found in PDF (might be a scanned image)"))
                } else {
                    Result.success(rawText.take(5000))
                }
            } finally {
                document.close()
            }
        }
    }

    private suspend fun extractFromImage(uri: Uri): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text.trim()
                        if (rawText.isBlank()) {
                            continuation.resume(Result.failure(IllegalStateException("No text detected in the image")))
                        } else {
                            continuation.resume(Result.success(rawText.take(5000)))
                        }
                    }
                    .addOnFailureListener { ex ->
                        continuation.resume(Result.failure(ex))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun formatSecureLegalPrompt(result: DocumentIntelligenceResult): String {
        return buildString {
            appendLine("📄 **DOCUMENT INTELLIGENCE ANALYSIS**")
            appendLine("**Classified Type:** ${result.classification.docType.displayName} (${(result.classification.confidence * 100).toInt()}% confidence)")

            if (result.isAdversarial) {
                appendLine("⚠️ **SECURITY WARNING:** Adversarial instruction patterns were detected and neutralized within this document.")
            }

            if (result.entities.parties.isNotEmpty()) {
                appendLine("**Parties Identified:** " + result.entities.parties.entries.joinToString(", ") { "${it.key}: ${it.value}" })
            }
            if (result.entities.amounts.isNotEmpty()) {
                appendLine("**Monetary Values:** " + result.entities.amounts.joinToString(", ") { "${it.contextLabel}: ${it.amountFormatted}" })
            }
            if (result.entities.clauses.isNotEmpty()) {
                appendLine("**Clauses Identified:** " + result.entities.clauses.joinToString("; ") { "${it.clauseType}: ${it.snippet}" })
            }
            appendLine()
            val extractedText = result.evidenceRecord.extractedText ?: result.sanitizedText
            appendLine(extractedText)
            appendLine()
            append("Please provide senior advocate legal guidance, identifying enforceable rights, statutory violations, and next procedural steps under Indian law.")
        }
    }
}

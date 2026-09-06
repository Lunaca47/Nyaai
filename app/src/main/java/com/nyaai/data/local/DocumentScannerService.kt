package com.nyaai.data.local

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class DocumentScannerService(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
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
                    val sanitized = rawText.take(3000)
                    Result.success(formatLegalPrompt(sanitized, "PDF Document"))
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
                            val sanitized = rawText.take(3000)
                            continuation.resume(Result.success(formatLegalPrompt(sanitized, "Scanned Image/Notice")))
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

    private fun formatLegalPrompt(extractedText: String, source: String): String {
        return buildString {
            append("Analyze the following $source under Indian law:\n\n")
            append("\"\"\"\n")
            append(extractedText)
            append("\n\"\"\"\n\n")
            append("Please explain the legal implications, relevant acts (BNS/BNSS/BSA/Constitution), and recommended next steps.")
        }
    }
}

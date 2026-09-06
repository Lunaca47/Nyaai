package com.nyaai.data.local

import android.content.Context
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfExtractorService(private val context: Context, private val dao: RagDao) {
    
    suspend fun initializeDatabaseFromAssets() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("nyaai_db_prefs", Context.MODE_PRIVATE)
        if (dao.getDocumentCount() > 0) {
            prefs.edit().putBoolean("pdf_indexing_completed", true).apply()
            return@withContext
        }

        val assetList = try { context.assets.list("")?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
        val assetFiles = listOf("coi.pdf", "bns.pdf", "bnss.pdf", "bsa.pdf").filter { it in assetList }
        if (assetFiles.isEmpty()) {
            Log.d("PdfExtractor", "Using preloaded database asset; skipping PDF extraction.")
            return@withContext
        }

        PDFBoxResourceLoader.init(context)
        val batch = mutableListOf<DocumentEntity>()

        for (fileName in assetFiles) {
            try {
                context.assets.open(fileName).use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()

                    for (i in 1..document.numberOfPages) {
                        stripper.startPage = i
                        stripper.endPage = i
                        val pageText = stripper.getText(document)
                        val cleanedPage = cleanLegalText(pageText)

                        if (cleanedPage.length > 100) {
                            val chunks = cleanedPage.split(Regex("(?=(?:ARTICLE|SECTION|Part)\\s+\\d+)", RegexOption.IGNORE_CASE))
                                .map { it.trim() }
                                .filter { it.length > 50 }

                            chunks.forEach { chunk ->
                                batch.add(DocumentEntity(
                                    sourcePath = fileName,
                                    content = "Page $i: $chunk"
                                ))
                                if (batch.size >= 50) {
                                    dao.insertAll(batch)
                                    batch.clear()
                                }
                            }
                        }
                    }
                    if (batch.isNotEmpty()) {
                        dao.insertAll(batch)
                        batch.clear()
                    }
                    document.close()
                }
            } catch (e: Exception) {
                Log.e("PdfExtractor", "Error extracting $fileName: ${e.message}")
            }
        }
        if (batch.isNotEmpty()) {
            dao.insertAll(batch)
            batch.clear()
        }
        prefs.edit().putBoolean("pdf_indexing_completed", true).apply()
    }

    private fun cleanLegalText(text: String): String {
        return text.lines()
            .filter { line ->
                val l = line.trim()
                !l.contains("GAZETTE", ignoreCase = true) &&
                !l.contains("PUBLISHED BY AUTHORITY", ignoreCase = true) &&
                l.length > 3
            }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
    }
}

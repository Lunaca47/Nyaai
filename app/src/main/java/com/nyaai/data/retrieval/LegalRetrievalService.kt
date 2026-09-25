package com.nyaai.data.retrieval

import android.util.Log
import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.RagDao
import com.nyaai.data.local.TrainingExampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RetrievalResult(
    val candidates: List<TrainingExampleEntity>,
    val documents: List<DocumentEntity>,
    val bestCandidate: TrainingExampleEntity?,
    val bestCandidateScore: Int,
    val uniqueTrainingMatches: List<TrainingExampleEntity>,
    val substantiveKeywords: List<String>,
    val numbersInQuery: List<String>
)

class LegalRetrievalService(
    private val ragDao: RagDao,
    private val backendBaseUrl: String? = null
) {

    companion object {
        private const val TAG = "LegalRetrievalService"

        private val FTS_STOP_WORDS = setOf(
            "and", "or", "not", "near", "match", "the", "for", "with", "about", "what", "how", "give", "tell", "explain", "overview",
            "can", "could", "should", "would", "does", "have", "been", "that", "this", "there", "they", "them", "from", "into", "also", "your",
            "section", "sections", "article", "articles", "act", "acts", "law", "laws", "provisions", "provision", "rules", "rule", "under",
            "regarding", "punishment", "rights", "right", "india", "indian", "need", "advice", "please", "help"
        )
    }


    suspend fun retrieve(
        query: String,
        targetJurisdiction: String? = null
    ): RetrievalResult {
        val qLower = query.lowercase().trim()
        val allWords = qLower.replace(Regex("[^a-z0-9 ]"), " ").split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 }
        val substantiveKeywords = allWords.filter { it !in FTS_STOP_WORDS }
        val numbersInQuery = Regex("\\d+").findAll(qLower).map { it.value }.toList()

        // 1. Training candidate retrieval
        val trainingCandidates = mutableListOf<TrainingExampleEntity>()
        try {
            for (num in numbersInQuery) {
                val numMatches = ragDao.searchTrainingExamplesByNumber(num)
                trainingCandidates.addAll(numMatches)
            }

            for (kw in substantiveKeywords) {
                if (trainingCandidates.size >= 80) break
                val matches = ragDao.searchTrainingExamples(kw)
                trainingCandidates.addAll(matches)
            }

            if (trainingCandidates.size < 5) {
                val matches = ragDao.searchTrainingExamples(qLower)
                trainingCandidates.addAll(matches)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Training example search failed: ${e.message}")
        }

        val scoredCandidates = trainingCandidates
            .distinctBy { it.id }
            .map { candidate ->
                val score = scoreTrainingCandidate(candidate, qLower, substantiveKeywords, numbersInQuery)
                candidate to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        val bestCandidate = scoredCandidates.firstOrNull()?.first
        val bestCandidateScore = scoredCandidates.firstOrNull()?.second ?: 0
        val uniqueTrainingMatches = scoredCandidates.take(3).map { it.first }

        // 2. Document retrieval (Backend Hybrid Search -> Local Room FTS Fallback)
        val rawContexts = mutableListOf<DocumentEntity>()

        if (!backendBaseUrl.isNullOrBlank()) {
            val remoteDocs = searchRemoteBackend(query, targetJurisdiction)
            rawContexts.addAll(remoteDocs)
        }

        if (rawContexts.isEmpty()) {
            suspend fun safeSearch(ftsQuery: String): List<DocumentEntity> {
                return try {
                    ragDao.search(ftsQuery)
                } catch (e: Exception) {
                    Log.w(TAG, "FTS search failed for '$ftsQuery': ${e.message}")
                    emptyList()
                }
            }

            val searchTerms = if (substantiveKeywords.isNotEmpty()) substantiveKeywords else allWords
            if (searchTerms.isNotEmpty()) {
                val phraseQuery = "\"${searchTerms.joinToString(" ")}\""
                rawContexts.addAll(safeSearch(phraseQuery).take(3))
            }
            if (rawContexts.size < 2 && numbersInQuery.isNotEmpty()) {
                val numSearch = numbersInQuery.joinToString(" OR ") { "article $it" }
                rawContexts.addAll(safeSearch(numSearch).take(3))
            }
            if (rawContexts.isEmpty() && searchTerms.isNotEmpty()) {
                val andQuery = searchTerms.joinToString(" ") { "$it*" }
                rawContexts.addAll(safeSearch(andQuery).take(5))
            }
            if (rawContexts.isEmpty() && searchTerms.isNotEmpty()) {
                for (kw in searchTerms.take(3)) {
                    rawContexts.addAll(safeSearch("$kw*").take(2))
                    if (rawContexts.size >= 3) break
                }
            }
        }


        // 3. Metadata Filtering: Filter out repealed laws & boost user jurisdiction
        val filteredDocuments = rawContexts
            .distinctBy { it.rowid }
            .filter { it.status.lowercase() != "repealed" }
            .sortedByDescending { doc ->
                var docScore = 0
                if (!targetJurisdiction.isNullOrBlank() && doc.jurisdiction.equals(targetJurisdiction, ignoreCase = true)) {
                    docScore += 10
                }
                docScore
            }
            .take(5)

        return RetrievalResult(
            candidates = scoredCandidates.map { it.first },
            documents = filteredDocuments,
            bestCandidate = bestCandidate,
            bestCandidateScore = bestCandidateScore,
            uniqueTrainingMatches = uniqueTrainingMatches,
            substantiveKeywords = substantiveKeywords,
            numbersInQuery = numbersInQuery
        )
    }

    fun scoreTrainingCandidate(
        item: TrainingExampleEntity,
        query: String,
        terms: List<String>,
        numbers: List<String>
    ): Int {
        var score = 0
        val qLower = query.lowercase().trim()
        val itemQLower = item.question.lowercase()
        val itemALower = item.answer.lowercase()
        val itemSLower = item.sourcePath.lowercase()
        val itemDLower = item.legalDomain.lowercase()

        // 1. Exact phrase match
        if (qLower.length >= 6 && (itemQLower.contains(qLower) || itemALower.contains(qLower) || itemSLower.contains(qLower))) {
            score += 35
        }

        // 2. Exact number match
        for (num in numbers) {
            if (itemSLower.contains(num)) score += 30
            else if (itemQLower.contains(num)) score += 15
            else if (itemALower.contains(num)) score += 8
        }

        // 3. Substantive query terms match
        for (term in terms) {
            if (itemSLower.contains(term)) score += 10
            if (itemQLower.contains(term)) score += 8
            if (itemDLower.contains(term)) score += 5
            if (itemALower.contains(term)) score += 3
        }

        return score
    }

    private suspend fun searchRemoteBackend(
        query: String,
        jurisdiction: String?
    ): List<DocumentEntity> = withContext(Dispatchers.IO) {
        val cleanBaseUrl = backendBaseUrl?.trimEnd('/') ?: return@withContext emptyList()
        try {
            val url = URL("$cleanBaseUrl/api/v1/retrieval/search")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("query", query)
                if (!jurisdiction.isNullOrBlank()) {
                    put("jurisdiction", jurisdiction)
                }
                put("limit", 5)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(responseText)
                val resultsArray = json.optJSONArray("results")
                if (resultsArray != null && resultsArray.length() > 0) {
                    val remoteDocs = mutableListOf<DocumentEntity>()
                    for (i in 0 until resultsArray.length()) {
                        val obj = resultsArray.getJSONObject(i)
                        remoteDocs.add(
                            DocumentEntity(
                                sourcePath = obj.optString("act", "statute.pdf"),
                                content = "${obj.optString("section", "")} - ${obj.optString("title", "")}: ${obj.optString("content", "")}",
                                jurisdiction = obj.optString("jurisdiction", "central"),
                                act = obj.optString("act", ""),
                                section = obj.optString("section", ""),
                                status = obj.optString("status", "in_force")
                            ).apply {
                                rowid = i + 1
                            }
                        )

                    }
                    Log.d(TAG, "Retrieved ${remoteDocs.size} documents from backend hybrid retrieval")
                    return@withContext remoteDocs
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend retrieval failed (${e.message}), falling back to local Room FTS4")
        }
        emptyList()
    }
}


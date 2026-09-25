package com.example.lab1.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HeuristicDetector : AlertDetector {
    private val attackKeywords = mapOf(
        "freezing" to 20,
        "frozen" to 20,
        "valve failure" to 30,
        "electrical issue" to 25,
        "structural crack" to 30,
        "emergency" to 15,
        "urgent" to 15,
        "malfunction" to 20,
        "critical" to 20,
        "warning" to 10
    )

    override suspend fun analyzeText(text: String): DetectionResult = withContext(Dispatchers.Default) {
        var score = 0

        attackKeywords.forEach { (keyword, weight) ->
            if (text.contains(keyword, ignoreCase = true)) {
                score += weight
            }
        }

        val confidenceScore = score.coerceAtMost(100)

        DetectionResult(
            confidenceScore = confidenceScore,
            isAttack = confidenceScore >= 50,
            rawText = text,
        )
    }
}

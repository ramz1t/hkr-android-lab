package com.example.lab1.domain

enum class DetectorType(val label: String) {
    HEURISTIC("Heuristic"),
    JEV("JEV")
}

interface AlertDetector {
    suspend fun analyzeText(text: String): DetectionResult
}

data class DetectionResult(
    val confidenceScore: Int,
    val isAttack: Boolean,
    val rawText: String,
    val pullRequestNumber: Int? = null,
)

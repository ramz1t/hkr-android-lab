package com.example.lab1.domain

import com.example.lab1.BuildConfig
import com.example.lab1.data.api.OneModelClient
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class JevDetector(
    private val client: OneModelClient = OneModelClient(
        apiKey = BuildConfig.OPENROUTER_API_KEY,
    ),
    private val confidenceThreshold: Double = 0.75,
) : AlertDetector {
    private val json = Json

    override suspend fun analyzeText(text: String): DetectionResult {
        val response = client.send(
            buildJsonObject {
                put("model", BuildConfig.OPENROUTER_MODEL)
                put("state", text)
                putJsonObject("questions") {
                    putJsonObject("is_attack") {
                        put("type", "noul")
                        put("instructions", ATTACK_INSTRUCTIONS)
                    }
                }
            }.toString(),
        )
        val confidence = parseConfidence(response)

        return DetectionResult(
            confidenceScore = (confidence * 100).toInt(),
            isAttack = confidence >= confidenceThreshold,
            rawText = text,
        )
    }

    private fun parseConfidence(response: String): Double {
        val confidence = try {
            json.parseToJsonElement(response)
                .jsonObject["answers"]
                ?.jsonObject
                ?.get("is_attack")
                ?.jsonObject
                ?.get("noul")
                ?.jsonPrimitive
                ?.doubleOrNull
        } catch (error: SerializationException) {
            throw IOException("OpenRouter returned invalid JSON.", error)
        } catch (error: IllegalArgumentException) {
            throw IOException("OpenRouter returned an invalid response shape.", error)
        } ?: throw IOException("OpenRouter response did not contain 'is_attack'.")

        if (!confidence.isFinite() || confidence !in 0.0..1.0) {
            throw IOException("OpenRouter returned an invalid confidence score.")
        }

        return confidence
    }

    private companion object {
        const val ATTACK_INSTRUCTIONS =
            "Does this text message report an urgent or critical physical house problem, " +
                "such as freezing equipment, frozen equipment, valve failure, an electrical issue, " +
                "a structural crack, an emergency, an intrusion, a malfunction, a critical condition, or a warning?"
    }
}

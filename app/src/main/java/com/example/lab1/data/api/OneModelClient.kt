package com.example.lab1.data.api

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OneModelClient(
    private val apiKey: String,
) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
                val response = chain.proceed(chain.request())
                val body = response.peekBody(MAX_LOG_BODY_BYTES).string()
                Log.d(TAG, "HTTP ${response.code}: $body")
                response
            },
        )
        .build()

    suspend fun send(body: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OpenRouter request failed with HTTP ${response.code}.")
            }

            response.body?.string()
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("OpenRouter returned an empty response.")
        }
    }

    private companion object {
        const val ENDPOINT = "https://openrouter.ai/api/v1/systemone"
        const val TAG = "OneModelClient"
        const val MAX_LOG_BODY_BYTES = 1024L * 1024L
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

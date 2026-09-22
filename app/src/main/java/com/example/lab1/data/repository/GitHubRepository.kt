package com.example.lab1.data.repository

import android.util.Base64
import com.example.lab1.data.api.GitHubApi
import com.example.lab1.data.model.GithubComment
import com.example.lab1.data.model.PullRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class GitHubRepository(
    private val api: GitHubApi,
    private val token: String,
    private val owner: String,
    private val repo: String
) {
    private val gson = Gson()
    private val authToken = "Bearer $token"

    suspend fun getHouseConfig(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHouseConfig(authToken, owner, repo)
            val decodedBytes = Base64.decode(response.content.replace("\n", ""), Base64.DEFAULT)
            val json = String(decodedBytes, StandardCharsets.UTF_8)
            val type = object : TypeToken<Map<String, Any>>() {}.type
            Result.success(gson.fromJson(json, type))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPullRequests(): Result<List<PullRequest>> = withContext(Dispatchers.IO) {
        try {
            val prs = api.getPullRequests(authToken, owner, repo)
            Result.success(prs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(issueNumber: Int): Result<List<GithubComment>> = withContext(Dispatchers.IO) {
        try {
            val comments = api.getComments(authToken, owner, repo, issueNumber)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

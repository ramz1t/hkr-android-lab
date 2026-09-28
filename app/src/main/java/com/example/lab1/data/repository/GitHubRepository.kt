package com.example.lab1.data.repository

import android.util.Base64
import com.example.lab1.data.api.GitHubApi
import com.example.lab1.data.model.GitHubFileResponse
import com.example.lab1.data.model.GitHubFileUpdateRequest
import com.example.lab1.data.model.PullRequest
import com.example.lab1.data.model.PullRequestState
import com.example.lab1.data.model.PullRequestStateUpdate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class GitHubRepository(
    private val api: GitHubApi,
    private val token: String,
    private val owner: String,
    private val repo: String,
) {
    private val gson = Gson()
    private val authToken = "Bearer $token"

    suspend fun getHouseConfig(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Result.success(readHouseConfig(api.getHouseConfig(authToken, owner, repo, MAIN_BRANCH)))
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getPullRequests(): Result<List<PullRequest>> = withContext(Dispatchers.IO) {
        try {
            val pullRequests = api.getPullRequests(authToken, owner, repo, PullRequestState.open)
                .map { pullRequest ->
                    pullRequest.copy(comments = getAllComments(pullRequest.number))
                }
            Result.success(pullRequests)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun forceReject(pullRequestNumber: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.updatePullRequestState(
                token = authToken,
                owner = owner,
                repo = repo,
                pullNumber = pullRequestNumber,
                update = PullRequestStateUpdate(),
            )
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(writeFailure("Force Reject", error))
        }
    }

    suspend fun forceMerge(pullRequestNumber: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentFile = api.getHouseConfig(authToken, owner, repo, MAIN_BRANCH)
            val config = readHouseConfig(currentFile).toMutableMap()
            config["target_temperature"] = 17.0
            config["last_updated_by"] = "Android-Operator"

            val content = Base64.encodeToString(
                gson.toJson(config).toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP,
            )
            api.updateHouseConfig(
                token = authToken,
                owner = owner,
                repo = repo,
                update = GitHubFileUpdateRequest(
                    message = "Android operator applied energy-saving change",
                    content = content,
                    sha = currentFile.sha,
                    branch = MAIN_BRANCH,
                ),
            )
            api.updatePullRequestState(
                token = authToken,
                owner = owner,
                repo = repo,
                pullNumber = pullRequestNumber,
                update = PullRequestStateUpdate(),
            )
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(writeFailure("Force Merge", error))
        }
    }

    private fun readHouseConfig(response: GitHubFileResponse): Map<String, Any> {
        val decodedBytes = Base64.decode(response.content.replace("\n", ""), Base64.DEFAULT)
        val json = String(decodedBytes, StandardCharsets.UTF_8)
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, type)
    }

    private suspend fun getAllComments(pullRequestNumber: Int): List<String> {
        val comments = mutableListOf<String>()
        var page = 1

        while (true) {
            val issueComments = api.getIssueComments(
                token = authToken,
                owner = owner,
                repo = repo,
                issueNumber = pullRequestNumber,
                page = page,
                perPage = COMMENTS_PAGE_SIZE,
            )
            val reviewComments = api.getReviewComments(
                token = authToken,
                owner = owner,
                repo = repo,
                pullNumber = pullRequestNumber,
                page = page,
                perPage = COMMENTS_PAGE_SIZE,
            )
            comments += (issueComments + reviewComments).mapNotNull { comment ->
                comment.body?.takeIf { it.isNotBlank() }
            }

            if (
                issueComments.size < COMMENTS_PAGE_SIZE &&
                reviewComments.size < COMMENTS_PAGE_SIZE
            ) {
                return comments
            }
            page++
        }
    }

    private fun writeFailure(action: String, error: Exception): Exception {
        val message = when (error) {
            is HttpException -> when (error.code()) {
                403 -> "$action failed: GitHub denied the request or rate limit was reached (HTTP 403)."
                422 -> "$action failed: GitHub rejected the request (HTTP 422)."
                else -> "$action failed: GitHub returned HTTP ${error.code()}."
            }
            else -> "$action failed: ${error.message ?: "Network error."}"
        }
        return IOException(message, error)
    }

    private companion object {
        const val COMMENTS_PAGE_SIZE = 100
        const val MAIN_BRANCH = "main"
    }
}

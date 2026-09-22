package com.example.lab1.data.model

data class PullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String
)

data class GitHubFileResponse(
    val content: String,
    val encoding: String
)

enum class PullRequestState {
    open, closed, all
}

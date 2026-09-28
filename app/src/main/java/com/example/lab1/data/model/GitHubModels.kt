package com.example.lab1.data.model

data class PullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val comments: List<String> = emptyList(),
)

data class GitHubComment(
    val body: String?,
)

data class GitHubFileResponse(
    val content: String,
    val encoding: String,
    val sha: String
)

data class PullRequestStateUpdate(
    val state: String = "closed",
)

data class GitHubFileUpdateRequest(
    val message: String,
    val content: String,
    val sha: String,
    val branch: String,
)

data class GitHubFileWriteResponse(
    val content: GitHubFileMetadata? = null,
)

data class GitHubFileMetadata(
    val sha: String? = null,
)

enum class PullRequestState {
    open, closed, all
}

package com.example.lab1.data.model

import com.google.gson.annotations.SerializedName

data class PullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String
)

data class GithubComment(
    val id: Long,
    val body: String,
    @SerializedName("user") val user: GithubUser
)

data class GithubUser(
    val login: String
)

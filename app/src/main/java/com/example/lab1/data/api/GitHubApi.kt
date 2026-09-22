package com.example.lab1.data.api

import com.example.lab1.data.model.GithubComment
import com.example.lab1.data.model.PullRequest
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface GitHubApi {
    @GET("repos/{owner}/{repo}/contents/house_config.json")
    suspend fun getHouseConfig(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubFileResponse

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<PullRequest>

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int
    ): List<GithubComment>
}

data class GitHubFileResponse(
    val content: String,
    val encoding: String
)

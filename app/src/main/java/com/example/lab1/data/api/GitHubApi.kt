package com.example.lab1.data.api

import com.example.lab1.data.model.GitHubFileResponse
import com.example.lab1.data.model.GitHubFileUpdateRequest
import com.example.lab1.data.model.GitHubFileWriteResponse
import com.example.lab1.data.model.GitHubComment
import com.example.lab1.data.model.PullRequest
import com.example.lab1.data.model.PullRequestState
import com.example.lab1.data.model.PullRequestStateUpdate
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface GitHubApi {
    @GET("repos/{owner}/{repo}/contents/house_config.json")
    suspend fun getHouseConfig(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String? = null,
    ): GitHubFileResponse

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: PullRequestState = PullRequestState.all
    ): List<PullRequest>

    @GET("repos/{owner}/{repo}/issues/{issueNumber}/comments")
    suspend fun getIssueComments(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issueNumber") issueNumber: Int,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): List<GitHubComment>

    @GET("repos/{owner}/{repo}/pulls/{pullNumber}/comments")
    suspend fun getReviewComments(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pullNumber") pullNumber: Int,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): List<GitHubComment>

    @PATCH("repos/{owner}/{repo}/pulls/{pullNumber}")
    suspend fun updatePullRequestState(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pullNumber") pullNumber: Int,
        @Body update: PullRequestStateUpdate,
    ): PullRequest

    @PUT("repos/{owner}/{repo}/contents/house_config.json")
    suspend fun updateHouseConfig(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body update: GitHubFileUpdateRequest,
    ): GitHubFileWriteResponse
}
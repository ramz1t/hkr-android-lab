package com.example.lab1.data.api

import com.example.lab1.data.model.GitHubFileResponse
import com.example.lab1.data.model.PullRequest
import com.example.lab1.data.model.PullRequestState
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

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
        @Path("repo") repo: String,
        @Query("state") state: PullRequestState = PullRequestState.all
    ): List<PullRequest>
}
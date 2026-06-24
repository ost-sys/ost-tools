package com.ost.application.util

import retrofit2.Call
import retrofit2.http.GET

interface GitHubApiService {
    @GET("repos/ost-sys/ost-program-android/releases")
    fun getReleases(): Call<List<GitHubRelease>>
}
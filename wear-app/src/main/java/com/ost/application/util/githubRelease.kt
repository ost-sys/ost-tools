package com.ost.application.util

import androidx.annotation.Keep

@Keep
data class GitHubRelease(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val published_at: String? = null,
    val prerelease: Boolean? = null,
    val draft: Boolean? = null
)
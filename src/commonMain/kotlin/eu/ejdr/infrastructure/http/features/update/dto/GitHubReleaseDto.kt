package eu.ejdr.infrastructure.http.features.update.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList(),
)

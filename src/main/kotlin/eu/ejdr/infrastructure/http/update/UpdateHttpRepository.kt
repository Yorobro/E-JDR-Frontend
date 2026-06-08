package eu.ejdr.infrastructure.http.update

import eu.ejdr.BuildConfig
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.application.update.abstraction.repository.UpdateRepository
import eu.ejdr.infrastructure.http.update.dto.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

private const val GITHUB_API = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"

class UpdateHttpRepository(private val client: HttpClient) : UpdateRepository {
    override suspend fun fetchLatestRelease(): UpdateInfo? = runCatching {
        val response = client.get(GITHUB_API)
        if (!response.status.isSuccess()) return null
        val dto = response.body<GitHubReleaseDto>()
        UpdateInfo(version = dto.tagName, releaseUrl = dto.htmlUrl)
    }.getOrNull()
}

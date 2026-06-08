package eu.ejdr.infrastructure.http.update

import eu.ejdr.BuildConfig
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.application.update.abstraction.repository.UpdateRepository
import eu.ejdr.infrastructure.http.update.dto.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File

private const val GITHUB_API = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"

class UpdateHttpRepository(private val client: HttpClient) : UpdateRepository {

    override suspend fun fetchLatestRelease(): UpdateInfo? = runCatching {
        val response = client.get(GITHUB_API)
        if (!response.status.isSuccess()) return null
        val dto = response.body<GitHubReleaseDto>()
        val asset = dto.assets.firstOrNull { it.name.endsWith(".exe") }
            ?: dto.assets.firstOrNull { it.name.endsWith(".msi") }
        UpdateInfo(version = dto.tagName, releaseUrl = dto.htmlUrl, downloadUrl = asset?.browserDownloadUrl)
    }.getOrNull()

    override suspend fun downloadUpdate(url: String, onProgress: (Float?) -> Unit): File {
        val file = File(System.getProperty("java.io.tmpdir"), "E-JDR-update.exe")
        client.prepareGet(url).execute { response ->
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLong()
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8 * 1024)
            var downloaded = 0L
            file.outputStream().buffered().use { output ->
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(contentLength?.let { downloaded.toFloat() / it })
                    }
                }
            }
        }
        return file
    }
}

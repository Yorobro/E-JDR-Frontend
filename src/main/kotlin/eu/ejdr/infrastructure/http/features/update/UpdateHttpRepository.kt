package eu.ejdr.infrastructure.http.features.update

import eu.ejdr.BuildConfig
import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.infrastructure.http.features.update.dto.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.security.MessageDigest

private const val GITHUB_API = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"

/** Hôtes GitHub autorisés à servir un installeur : tout autre hôte est refusé avant exécution. */
private val ALLOWED_DOWNLOAD_HOSTS = setOf("github.com", "objects.githubusercontent.com")

class UpdateHttpRepository(private val client: HttpClient) : UpdateRepository {

    override suspend fun fetchLatestRelease(): UpdateInfoDto? = runCatchingCancellable {
        val response = client.get(GITHUB_API)
        if (!response.status.isSuccess()) return null
        val dto = response.body<GitHubReleaseDto>()
        val installer = dto.assets.firstOrNull { it.name.endsWith(".exe") }
            ?: dto.assets.firstOrNull { it.name.endsWith(".msi") }
        // Empreinte publiée à côté de l'installeur : "<nom-installeur>.sha256".
        val checksum = installer?.let { asset ->
            dto.assets.firstOrNull { it.name == "${asset.name}.sha256" }
        }
        UpdateInfoDto(
            version = dto.tagName,
            releaseUrl = dto.htmlUrl,
            downloadUrl = installer?.browserDownloadUrl,
            sha256Url = checksum?.browserDownloadUrl,
        )
    }.getOrNull()

    /**
     * Télécharge l'installeur puis **vérifie son intégrité avant tout lancement** :
     * 1. l'URL doit pointer vers un hôte GitHub de confiance ([ALLOWED_DOWNLOAD_HOSTS}) ;
     * 2. une empreinte SHA-256 doit être publiée dans la release ([sha256Url] non nul) ;
     * 3. l'empreinte du fichier reçu doit être identique à l'empreinte publiée.
     *
     * Toute condition non remplie lève [SecurityException] : aucun binaire non vérifié n'est
     * jamais rendu à l'appelant (donc jamais exécuté). Cette défense complète HTTPS contre une
     * release piégée ou un MITM derrière une CA d'entreprise interceptante.
     */
    override suspend fun downloadUpdate(
        url: String,
        sha256Url: String?,
        onProgress: (Float?) -> Unit,
    ): File {
        requireTrustedHost(url)
        if (sha256Url == null) {
            throw SecurityException("Aucune empreinte SHA-256 publiée pour cette mise à jour.")
        }
        requireTrustedHost(sha256Url)

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

        val expected = fetchExpectedSha256(sha256Url)
        val actual = sha256Of(file)
        if (expected == null || !actual.equals(expected, ignoreCase = true)) {
            file.delete()
            throw SecurityException("Empreinte SHA-256 invalide : binaire rejeté.")
        }
        return file
    }

    /** Récupère l'empreinte publiée et n'en garde que le digest hexadécimal (format `sha256  nom`). */
    private suspend fun fetchExpectedSha256(sha256Url: String): String? {
        val response = client.get(sha256Url)
        if (!response.status.isSuccess()) return null
        val token = response.bodyAsText().trim().substringBefore(' ').trim()
        return token.takeIf { it.length == 64 && it.all { c -> c.isHexDigit() } }
    }

    private fun requireTrustedHost(rawUrl: String) {
        val host = runCatching { Url(rawUrl).host }.getOrNull()
        if (host == null || host !in ALLOWED_DOWNLOAD_HOSTS) {
            throw SecurityException("Hôte de téléchargement non autorisé : $host")
        }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

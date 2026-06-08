package eu.ejdr.application.update

import eu.ejdr.application.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import java.io.File
import kotlin.system.exitProcess

class DownloadAndInstallUpdateUseCaseImpl(private val client: HttpClient) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(downloadUrl: String, onProgress: (Float?) -> Unit) {
        val file = File(System.getProperty("java.io.tmpdir"), "E-JDR-update.exe")
        client.prepareGet(downloadUrl).execute { response ->
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
        ProcessBuilder("cmd", "/c", "start", "", file.absolutePath).start()
        exitProcess(0)
    }
}

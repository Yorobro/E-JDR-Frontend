package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import kotlin.system.exitProcess

class DownloadAndInstallUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(downloadUrl: String, onProgress: (Float?) -> Unit) {
        val file = updateRepository.downloadUpdate(downloadUrl, onProgress)
        ProcessBuilder("cmd", "/c", "start", "", file.absolutePath).start()
        exitProcess(0)
    }
}

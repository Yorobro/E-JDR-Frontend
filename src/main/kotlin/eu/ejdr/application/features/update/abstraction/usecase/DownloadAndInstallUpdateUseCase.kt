package eu.ejdr.application.features.update.abstraction.usecase

fun interface DownloadAndInstallUpdateUseCase {
    suspend operator fun invoke(downloadUrl: String, onProgress: (Float?) -> Unit)
}

package eu.ejdr.application.features.update.abstraction.repository

import eu.ejdr.application.features.update.dto.UpdateInfoDto

interface UpdateRepository {
    suspend fun fetchLatestRelease(): UpdateInfoDto?
    suspend fun downloadUpdate(url: String, onProgress: (Float?) -> Unit): java.io.File
}

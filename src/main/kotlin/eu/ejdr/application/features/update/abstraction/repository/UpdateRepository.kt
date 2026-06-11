package eu.ejdr.application.features.update.abstraction.repository

import eu.ejdr.application.features.update.abstraction.UpdateInfo

interface UpdateRepository {
    suspend fun fetchLatestRelease(): UpdateInfo?
    suspend fun downloadUpdate(url: String, onProgress: (Float?) -> Unit): java.io.File
}

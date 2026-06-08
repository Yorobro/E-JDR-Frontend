package eu.ejdr.application.update.abstraction.repository

import eu.ejdr.application.update.abstraction.UpdateInfo

interface UpdateRepository {
    suspend fun fetchLatestRelease(): UpdateInfo?
    suspend fun downloadUpdate(url: String, onProgress: (Float?) -> Unit): java.io.File
}

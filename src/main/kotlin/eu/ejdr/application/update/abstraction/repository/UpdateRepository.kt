package eu.ejdr.application.update.abstraction.repository

import eu.ejdr.application.update.abstraction.UpdateInfo

fun interface UpdateRepository {
    suspend fun fetchLatestRelease(): UpdateInfo?
}

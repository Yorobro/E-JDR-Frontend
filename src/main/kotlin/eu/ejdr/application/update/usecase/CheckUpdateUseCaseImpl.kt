package eu.ejdr.application.update.usecase

import eu.ejdr.BuildConfig
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.application.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.update.abstraction.usecase.CheckUpdateUseCase

class CheckUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val currentVersion: String = BuildConfig.APP_VERSION,
) : CheckUpdateUseCase {

    override suspend fun invoke(): UpdateInfo? {
        val latest = updateRepository.fetchLatestRelease() ?: return null
        return if (isNewer(latest.version, currentVersion)) latest else null
    }

    private fun isNewer(latest: String, current: String): Boolean {
        fun parse(v: String) = v.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val l = parse(latest)
        val c = parse(current)
        for (i in 0..2) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}

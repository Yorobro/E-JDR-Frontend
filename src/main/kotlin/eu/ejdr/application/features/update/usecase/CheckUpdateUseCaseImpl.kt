package eu.ejdr.application.features.update.usecase

import eu.ejdr.BuildConfig
import eu.ejdr.application.features.update.abstraction.UpdateInfo
import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.domain.shared.version.SemanticVersion

class CheckUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val currentVersion: String = BuildConfig.APP_VERSION,
) : CheckUpdateUseCase {

    override suspend fun invoke(): UpdateInfo? {
        val latest = updateRepository.fetchLatestRelease() ?: return null
        val isNewer = SemanticVersion.parse(latest.version)
            .isNewerThan(SemanticVersion.parse(currentVersion))
        return if (isNewer) latest else null
    }
}

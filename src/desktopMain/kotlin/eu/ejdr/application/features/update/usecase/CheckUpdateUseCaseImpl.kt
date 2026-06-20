package eu.ejdr.application.features.update.usecase

import eu.ejdr.BuildConfig
import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.update.error.UpdateError
import eu.ejdr.domain.shared.version.SemanticVersion

class CheckUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val currentVersion: String = BuildConfig.APP_VERSION,
    private val isDev: Boolean = BuildConfig.IS_DEV,
) : CheckUpdateUseCase {

    override suspend fun invoke(): Result<UpdateInfoDto?, UpdateError> {
        // En développement, la version locale est volontairement en retard sur les releases
        // publiées : on ne propose donc jamais de mise à jour (et on n'interroge pas GitHub).
        if (isDev) return Result.Success(null)

        return runCatchingCancellable {
            val latest = updateRepository.fetchLatestRelease()
            if (latest == null) {
                null
            } else {
                val isNewer = SemanticVersion.parse(latest.version)
                    .isNewerThan(SemanticVersion.parse(currentVersion))
                if (isNewer) latest else null
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(UpdateError.CheckFailed) },
        )
    }
}

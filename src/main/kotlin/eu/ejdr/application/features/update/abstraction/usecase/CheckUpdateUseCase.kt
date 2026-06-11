package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.features.update.abstraction.UpdateInfo

fun interface CheckUpdateUseCase {
    suspend operator fun invoke(): UpdateInfo?
}

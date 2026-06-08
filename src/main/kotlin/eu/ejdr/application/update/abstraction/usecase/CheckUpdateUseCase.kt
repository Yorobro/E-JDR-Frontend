package eu.ejdr.application.update.abstraction.usecase

import eu.ejdr.application.update.abstraction.UpdateInfo

fun interface CheckUpdateUseCase {
    suspend operator fun invoke(): UpdateInfo?
}

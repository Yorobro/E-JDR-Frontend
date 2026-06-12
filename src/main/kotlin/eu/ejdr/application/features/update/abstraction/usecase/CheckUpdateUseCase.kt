package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.features.update.dto.UpdateInfoDto

fun interface CheckUpdateUseCase {
    suspend operator fun invoke(): UpdateInfoDto?
}

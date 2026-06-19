package eu.ejdr.application.features.friendgroup.abstraction.usecase

fun interface GetActiveGroupIdUseCase {
    suspend operator fun invoke(): String?
}

fun interface SetActiveGroupIdUseCase {
    suspend operator fun invoke(id: String?)
}

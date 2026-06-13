package eu.ejdr.application.features.charactersheet.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError

/** Use case : liste les fiches de l'utilisateur courant. */
fun interface ListCharacterSheetsUseCase {
    suspend operator fun invoke(): Result<List<CharacterSheet>, CharacterSheetError>
}

/** Use case : crée une fiche pour l'utilisateur courant. */
fun interface CreateCharacterSheetUseCase {
    suspend operator fun invoke(name: String): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : supprime une fiche de l'utilisateur courant. */
fun interface DeleteCharacterSheetUseCase {
    suspend operator fun invoke(id: String): Result<Unit, CharacterSheetError>
}

/** Use case : liste les fiches rattachées à une campagne. */
fun interface ListCampaignCharactersUseCase {
    suspend operator fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError>
}

/** Use case : rattache une fiche à une campagne. */
fun interface LinkCharacterToCampaignUseCase {
    suspend operator fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>
}

/** Use case : détache une fiche d'une campagne. */
fun interface UnlinkCharacterFromCampaignUseCase {
    suspend operator fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError>
}

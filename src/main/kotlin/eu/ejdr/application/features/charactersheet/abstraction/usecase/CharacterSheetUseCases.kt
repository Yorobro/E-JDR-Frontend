package eu.ejdr.application.features.charactersheet.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError

/** Use case : liste les fiches de l'utilisateur courant. */
fun interface ListCharacterSheetsUseCase {
    suspend operator fun invoke(): Result<List<CharacterSheet>, CharacterSheetError>
}

/** Use case : crée une fiche pour l'utilisateur courant. */
fun interface CreateCharacterSheetUseCase {
    suspend operator fun invoke(name: String): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : récupère le détail complet d'une fiche par son identifiant. */
fun interface GetCharacterSheetUseCase {
    suspend operator fun invoke(id: String): Result<CharacterSheet, CharacterSheetError>
}

/** Use case : met à jour une fiche (nom + champs détaillés). */
fun interface UpdateCharacterSheetUseCase {
    suspend operator fun invoke(sheet: CharacterSheet): Result<CharacterSheet, CharacterSheetError>
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

/** Use case : liste les fiches rattachables à une campagne (MJ uniquement, côté back). */
fun interface ListLinkableCharactersUseCase {
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

/** Use case : liste les campagnes d'une fiche (lecture seule, avec pseudo MJ). */
fun interface GetSheetCampaignsUseCase {
    suspend operator fun invoke(id: String): Result<List<SheetCampaign>, CharacterSheetError>
}

/** Use case : récupère le PDF (binaire) de la fiche courante. */
fun interface ExportCharacterSheetPdfUseCase {
    suspend operator fun invoke(id: String): Result<ByteArray, CharacterSheetError>
}

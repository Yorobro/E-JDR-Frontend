package eu.ejdr.application.features.charactersheet.usecase

import eu.ejdr.application.features.charactersheet.abstraction.repository.CharacterSheetRepository
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError

/** Implémentations triviales déléguant au [CharacterSheetRepository]. */

class ListCharacterSheetsUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ListCharacterSheetsUseCase {
    override suspend fun invoke(): Result<List<CharacterSheet>, CharacterSheetError> =
        repository.list()
}

class CreateCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : CreateCharacterSheetUseCase {
    override suspend fun invoke(name: String): Result<CharacterSheet, CharacterSheetError> =
        repository.create(name)
}

class GetCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : GetCharacterSheetUseCase {
    override suspend fun invoke(id: String): Result<CharacterSheet, CharacterSheetError> =
        repository.getById(id)
}

class UpdateCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : UpdateCharacterSheetUseCase {
    override suspend fun invoke(
        sheet: CharacterSheet,
    ): Result<CharacterSheet, CharacterSheetError> = repository.update(sheet)
}

class DeleteCharacterSheetUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : DeleteCharacterSheetUseCase {
    override suspend fun invoke(id: String): Result<Unit, CharacterSheetError> =
        repository.delete(id)
}

class ListCampaignCharactersUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ListCampaignCharactersUseCase {
    override suspend fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError> = repository.listForCampaign(campaignId)
}

class ListLinkableCharactersUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : ListLinkableCharactersUseCase {
    override suspend fun invoke(
        campaignId: String,
    ): Result<List<CharacterSheet>, CharacterSheetError> =
        repository.listLinkableForCampaign(campaignId)
}

class LinkCharacterToCampaignUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : LinkCharacterToCampaignUseCase {
    override suspend fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError> = repository.linkToCampaign(campaignId, characterSheetId)
}

class UnlinkCharacterFromCampaignUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : UnlinkCharacterFromCampaignUseCase {
    override suspend fun invoke(
        campaignId: String,
        characterSheetId: String,
    ): Result<Unit, CharacterSheetError> =
        repository.unlinkFromCampaign(campaignId, characterSheetId)
}

class GetSheetCampaignsUseCaseImpl(
    private val repository: CharacterSheetRepository,
) : GetSheetCampaignsUseCase {
    override suspend fun invoke(id: String): Result<List<SheetCampaign>, CharacterSheetError> =
        repository.getCampaignsForSheet(id)
}

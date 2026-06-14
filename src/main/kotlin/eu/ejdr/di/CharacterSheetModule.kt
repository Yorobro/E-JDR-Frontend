package eu.ejdr.di

import eu.ejdr.application.features.charactersheet.abstraction.repository.CharacterSheetRepository
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.usecase.CreateCharacterSheetUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.DeleteCharacterSheetUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.GetCharacterSheetUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.GetSheetCampaignsUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.LinkCharacterToCampaignUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.ListCampaignCharactersUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.ListLinkableCharactersUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.ListCharacterSheetsUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.UnlinkCharacterFromCampaignUseCaseImpl
import eu.ejdr.application.features.charactersheet.usecase.UpdateCharacterSheetUseCaseImpl
import eu.ejdr.infrastructure.http.features.charactersheet.CharacterSheetHttpRepository
import org.koin.dsl.module

/**
 * Module Koin de la feature fiches de personnage : port application (use cases) + adaptateur
 * infrastructure (repository HTTP). Le `HttpClient` et l'`AppConfig` viennent du socle
 * transverse [infrastructureModule].
 */
val characterSheetModule = module {
    single<CharacterSheetRepository> { CharacterSheetHttpRepository(get(), get()) }
    single<ListCharacterSheetsUseCase> { ListCharacterSheetsUseCaseImpl(get()) }
    single<CreateCharacterSheetUseCase> { CreateCharacterSheetUseCaseImpl(get()) }
    single<GetCharacterSheetUseCase> { GetCharacterSheetUseCaseImpl(get()) }
    single<UpdateCharacterSheetUseCase> { UpdateCharacterSheetUseCaseImpl(get()) }
    single<DeleteCharacterSheetUseCase> { DeleteCharacterSheetUseCaseImpl(get()) }
    single<ListCampaignCharactersUseCase> { ListCampaignCharactersUseCaseImpl(get()) }
    single<ListLinkableCharactersUseCase> { ListLinkableCharactersUseCaseImpl(get()) }
    single<LinkCharacterToCampaignUseCase> { LinkCharacterToCampaignUseCaseImpl(get()) }
    single<UnlinkCharacterFromCampaignUseCase> { UnlinkCharacterFromCampaignUseCaseImpl(get()) }
    single<GetSheetCampaignsUseCase> { GetSheetCampaignsUseCaseImpl(get()) }
}

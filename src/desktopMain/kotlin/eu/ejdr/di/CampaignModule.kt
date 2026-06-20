package eu.ejdr.di

import eu.ejdr.application.features.campaign.abstraction.repository.CampaignRepository
import eu.ejdr.application.features.campaign.abstraction.usecase.CreateCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.DeleteCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.campaign.usecase.CreateCampaignUseCaseImpl
import eu.ejdr.application.features.campaign.usecase.DeleteCampaignUseCaseImpl
import eu.ejdr.application.features.campaign.usecase.ListCampaignsUseCaseImpl
import eu.ejdr.infrastructure.http.features.campaign.CampaignHttpRepository
import org.koin.dsl.module

/**
 * Module Koin de la feature campagnes : port application (use cases) + adaptateur
 * infrastructure (repository HTTP). Le `HttpClient` et l'`AppConfig` viennent du socle
 * transverse [infrastructureModule].
 */
val campaignModule = module {
    single<CampaignRepository> { CampaignHttpRepository(get(), get()) }
    single<ListCampaignsUseCase> { ListCampaignsUseCaseImpl(get()) }
    single<CreateCampaignUseCase> { CreateCampaignUseCaseImpl(get()) }
    single<DeleteCampaignUseCase> { DeleteCampaignUseCaseImpl(get()) }
}

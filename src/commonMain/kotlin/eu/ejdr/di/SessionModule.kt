package eu.ejdr.di

import eu.ejdr.application.features.session.abstraction.repository.SessionRepository
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.features.session.usecase.CreateSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.DeleteSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.GetSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.ListCampaignSessionsUseCaseImpl
import eu.ejdr.application.features.session.usecase.UpdateSessionUseCaseImpl
import eu.ejdr.infrastructure.http.features.session.SessionHttpRepository
import org.koin.dsl.module

/**
 * Module Koin de la feature sessions : port application (use cases) + adaptateur infrastructure
 * (repository HTTP). Le `HttpClient` et l'`AppConfig` viennent du socle transverse
 * [infrastructureModule].
 */
val sessionModule = module {
    single<SessionRepository> { SessionHttpRepository(get(), get()) }
    single<ListCampaignSessionsUseCase> { ListCampaignSessionsUseCaseImpl(get()) }
    single<CreateSessionUseCase> { CreateSessionUseCaseImpl(get()) }
    single<GetSessionUseCase> { GetSessionUseCaseImpl(get()) }
    single<UpdateSessionUseCase> { UpdateSessionUseCaseImpl(get()) }
    single<DeleteSessionUseCase> { DeleteSessionUseCaseImpl(get()) }
}

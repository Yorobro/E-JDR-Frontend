package eu.ejdr.di

import eu.ejdr.application.features.session.abstraction.repository.SessionRepository
import eu.ejdr.application.features.session.abstraction.usecase.CreateLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.InviteToLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListMySessionInvitationsUseCase
import eu.ejdr.application.features.session.abstraction.usecase.RespondToInvitationUseCase
import eu.ejdr.application.features.session.abstraction.usecase.StartSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.features.session.usecase.CreateLobbyUseCaseImpl
import eu.ejdr.application.features.session.usecase.CreateSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.DeleteSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.GetSessionLobbyUseCaseImpl
import eu.ejdr.application.features.session.usecase.GetSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.InviteToLobbyUseCaseImpl
import eu.ejdr.application.features.session.usecase.ListCampaignSessionsUseCaseImpl
import eu.ejdr.application.features.session.usecase.ListMySessionInvitationsUseCaseImpl
import eu.ejdr.application.features.session.usecase.RespondToInvitationUseCaseImpl
import eu.ejdr.application.features.session.usecase.StartSessionUseCaseImpl
import eu.ejdr.application.features.session.usecase.UpdateSessionUseCaseImpl
import eu.ejdr.infrastructure.http.features.session.SessionHttpRepository
import eu.ejdr.presentation.features.session.SessionLobbyState
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
    single<CreateLobbyUseCase> { CreateLobbyUseCaseImpl(get()) }
    single<InviteToLobbyUseCase> { InviteToLobbyUseCaseImpl(get()) }
    single<GetSessionUseCase> { GetSessionUseCaseImpl(get()) }
    single<UpdateSessionUseCase> { UpdateSessionUseCaseImpl(get()) }
    single<DeleteSessionUseCase> { DeleteSessionUseCaseImpl(get()) }
    single<RespondToInvitationUseCase> { RespondToInvitationUseCaseImpl(get()) }
    single<StartSessionUseCase> { StartSessionUseCaseImpl(get()) }
    single<ListMySessionInvitationsUseCase> { ListMySessionInvitationsUseCaseImpl(get()) }
    single<GetSessionLobbyUseCase> { GetSessionLobbyUseCaseImpl(get()) }
    // État partagé du lobby : hand-off détail → lobby (et futur point d'entrée temps réel).
    single { SessionLobbyState() }
}

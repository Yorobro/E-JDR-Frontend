package eu.ejdr.di

import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import eu.ejdr.application.features.friendgroup.abstraction.repository.FriendGroupRepository
import eu.ejdr.application.features.friendgroup.abstraction.usecase.AcceptInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ChangeMemberRoleUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.CreateGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeclineInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeleteGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.InviteMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyGroupsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyInvitationsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.RemoveMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.SetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.usecase.AcceptInvitationUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.ChangeMemberRoleUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.CreateGroupUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.DeclineInvitationUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.DeleteGroupUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.GetActiveGroupIdUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.GetGroupUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.InviteMemberUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.ListMyGroupsUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.ListMyInvitationsUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.RemoveMemberUseCaseImpl
import eu.ejdr.application.features.friendgroup.usecase.SetActiveGroupIdUseCaseImpl
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.features.friendgroup.FriendGroupHttpRepository
import eu.ejdr.infrastructure.settings.ActiveGroupFileRepository
import eu.ejdr.presentation.features.friendgroup.ActiveGroupState
import org.koin.dsl.module
import java.io.File

val friendGroupModule = module {
    single<FriendGroupRepository> { FriendGroupHttpRepository(get(), get()) }
    single<ActiveGroupRepository> { ActiveGroupFileRepository(File(get<AppConfig>().dataDir)) }

    single<ListMyGroupsUseCase> { ListMyGroupsUseCaseImpl(get()) }
    single<GetGroupUseCase> { GetGroupUseCaseImpl(get()) }
    single<CreateGroupUseCase> { CreateGroupUseCaseImpl(get()) }
    single<DeleteGroupUseCase> { DeleteGroupUseCaseImpl(get()) }
    single<InviteMemberUseCase> { InviteMemberUseCaseImpl(get()) }
    single<ListMyInvitationsUseCase> { ListMyInvitationsUseCaseImpl(get()) }
    single<AcceptInvitationUseCase> { AcceptInvitationUseCaseImpl(get()) }
    single<DeclineInvitationUseCase> { DeclineInvitationUseCaseImpl(get()) }
    single<RemoveMemberUseCase> { RemoveMemberUseCaseImpl(get()) }
    single<ChangeMemberRoleUseCase> { ChangeMemberRoleUseCaseImpl(get()) }

    single<GetActiveGroupIdUseCase> { GetActiveGroupIdUseCaseImpl(get()) }
    single<SetActiveGroupIdUseCase> { SetActiveGroupIdUseCaseImpl(get()) }

    single { ActiveGroupState(get(), get(), get()) }
}

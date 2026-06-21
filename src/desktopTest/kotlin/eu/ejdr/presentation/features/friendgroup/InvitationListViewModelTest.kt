package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.AcceptInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeclineInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyInvitationsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class InvitationListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun invitation(id: String) = GroupInvitation(
        id = id,
        groupId = "group-1",
        groupName = "Mon Groupe",
        invitedBy = "user-a",
        invitedByPseudo = "Alice",
        createdAt = "2026-06-18T10:00:00.000Z",
    )

    @Test
    fun `loads invitations at init`() = runTest {
        val vm = InvitationListViewModel(
            listMyInvitations = ListMyInvitationsUseCase { Result.Success(listOf(invitation("inv-1"))) },
            acceptInvitation = AcceptInvitationUseCase { Result.Success(Unit) },
            declineInvitation = DeclineInvitationUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(1, vm.invitations.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `accept success reloads the list`() = runTest {
        var stored = listOf(invitation("inv-1"))
        val vm = InvitationListViewModel(
            listMyInvitations = ListMyInvitationsUseCase { Result.Success(stored) },
            acceptInvitation = AcceptInvitationUseCase { _ ->
                stored = emptyList()
                Result.Success(Unit)
            },
            declineInvitation = DeclineInvitationUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.accept("inv-1")
        advanceUntilIdle()

        assertEquals(0, vm.invitations.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `decline success reloads the list`() = runTest {
        var stored = listOf(invitation("inv-1"))
        val vm = InvitationListViewModel(
            listMyInvitations = ListMyInvitationsUseCase { Result.Success(stored) },
            acceptInvitation = AcceptInvitationUseCase { Result.Success(Unit) },
            declineInvitation = DeclineInvitationUseCase { _ ->
                stored = emptyList()
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.decline("inv-1")
        advanceUntilIdle()

        assertEquals(0, vm.invitations.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `accept failure exposes error message`() = runTest {
        val vm = InvitationListViewModel(
            listMyInvitations = ListMyInvitationsUseCase { Result.Success(listOf(invitation("inv-1"))) },
            acceptInvitation = AcceptInvitationUseCase { Result.Failure(FriendGroupError.InvitationAlreadyResolved) },
            declineInvitation = DeclineInvitationUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.accept("inv-1")
        advanceUntilIdle()

        assertEquals(FriendGroupError.InvitationAlreadyResolved.message, vm.error.value)
    }
}

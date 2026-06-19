package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.ChangeMemberRoleUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.InviteMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.RemoveMemberUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun member(userId: String, role: String) =
        GroupMember(userId = userId, role = role, createdAt = "2026-06-18T10:00:00.000Z")

    private fun detail(vararg members: GroupMember) = FriendGroupDetail(
        id = "group-1",
        name = "Mon Groupe",
        myRole = "ADMIN",
        createdAt = "2026-06-18T10:00:00.000Z",
        members = members.toList(),
    )

    private fun viewModel(
        getGroup: GetGroupUseCase,
        inviteMember: InviteMemberUseCase = InviteMemberUseCase { _, _ -> Result.Success("inv-1") },
        removeMember: RemoveMemberUseCase = RemoveMemberUseCase { _, _ -> Result.Success(Unit) },
        changeMemberRole: ChangeMemberRoleUseCase = ChangeMemberRoleUseCase { _, _, _ -> Result.Success(Unit) },
    ) = GroupDetailViewModel("group-1", getGroup, inviteMember, removeMember, changeMemberRole)

    @Test
    fun `loads group detail at init`() = runTest {
        val vm = viewModel(
            getGroup = GetGroupUseCase { Result.Success(detail(member("user-a", "ADMIN"))) },
        )
        advanceUntilIdle()

        assertEquals(1, vm.detail.value?.members?.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `load failure exposes error message`() = runTest {
        val vm = viewModel(
            getGroup = GetGroupUseCase { Result.Failure(FriendGroupError.NotFound) },
        )
        advanceUntilIdle()

        assertNull(vm.detail.value)
        assertEquals(FriendGroupError.NotFound.message, vm.error.value)
    }

    @Test
    fun `changeRole success reloads the detail with the new role`() = runTest {
        // Le premier chargement renvoie un membre MEMBER ; après promotion, le rechargement le voit ADMIN.
        var stored = detail(member("user-a", "ADMIN"), member("user-b", "MEMBER"))
        val vm = viewModel(
            getGroup = GetGroupUseCase { Result.Success(stored) },
            changeMemberRole = ChangeMemberRoleUseCase { _, userId, role ->
                stored = detail(member("user-a", "ADMIN"), member(userId, role))
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.changeRole("user-b", "ADMIN")
        advanceUntilIdle()

        assertEquals("ADMIN", vm.detail.value?.members?.first { it.userId == "user-b" }?.role)
        assertNull(vm.error.value)
    }

    @Test
    fun `changeRole failure exposes error message`() = runTest {
        val vm = viewModel(
            getGroup = GetGroupUseCase { Result.Success(detail(member("user-a", "ADMIN"))) },
            changeMemberRole = ChangeMemberRoleUseCase { _, _, _ ->
                Result.Failure(FriendGroupError.CannotRemoveLastAdmin)
            },
        )
        advanceUntilIdle()

        vm.changeRole("user-a", "MEMBER")
        advanceUntilIdle()

        assertEquals(FriendGroupError.CannotRemoveLastAdmin.message, vm.error.value)
    }

    @Test
    fun `removeMember success reloads the detail`() = runTest {
        var stored = detail(member("user-a", "ADMIN"), member("user-b", "MEMBER"))
        val vm = viewModel(
            getGroup = GetGroupUseCase { Result.Success(stored) },
            removeMember = RemoveMemberUseCase { _, userId ->
                stored = detail(*stored.members.filterNot { it.userId == userId }.toTypedArray())
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.removeMember("user-b")
        advanceUntilIdle()

        assertEquals(1, vm.detail.value?.members?.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `invite success raises inviteSuccess flag`() = runTest {
        val vm = viewModel(
            getGroup = GetGroupUseCase { Result.Success(detail(member("user-a", "ADMIN"))) },
            inviteMember = InviteMemberUseCase { _, _ -> Result.Success("inv-42") },
        )
        advanceUntilIdle()

        vm.invite("friend@example.com")
        advanceUntilIdle()

        assertTrue(vm.inviteSuccess.value)
        assertNull(vm.error.value)
    }
}

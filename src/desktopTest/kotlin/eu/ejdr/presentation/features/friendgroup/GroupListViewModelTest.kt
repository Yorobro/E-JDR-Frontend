package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.CreateGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeleteGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyGroupsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
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
class GroupListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun group(id: String, name: String = "Groupe $id") =
        FriendGroup(id = id, name = name, myRole = "ADMIN", createdAt = "2026-06-18T10:00:00.000Z")

    @Test
    fun `loads groups at init`() = runTest {
        val vm = GroupListViewModel(
            listMyGroups = ListMyGroupsUseCase { Result.Success(listOf(group("g1"))) },
            createGroup = CreateGroupUseCase { Result.Success(group("g2")) },
            deleteGroup = DeleteGroupUseCase { Result.Success(Unit) },
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()

        assertEquals(1, vm.groups.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create success reloads the list`() = runTest {
        var stored = emptyList<FriendGroup>()
        val vm = GroupListViewModel(
            listMyGroups = ListMyGroupsUseCase { Result.Success(stored) },
            createGroup = CreateGroupUseCase { name ->
                stored = listOf(group("g1", name))
                Result.Success(group("g1", name))
            },
            deleteGroup = DeleteGroupUseCase { Result.Success(Unit) },
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()

        vm.create("Mon groupe")
        advanceUntilIdle()

        assertEquals(1, vm.groups.value.size)
        assertEquals("Mon groupe", vm.groups.value.first().name)
        assertNull(vm.error.value)
    }

    @Test
    fun `create failure exposes error message`() = runTest {
        val vm = GroupListViewModel(
            listMyGroups = ListMyGroupsUseCase { Result.Success(emptyList()) },
            createGroup = CreateGroupUseCase { Result.Failure(FriendGroupError.InvalidGroupName) },
            deleteGroup = DeleteGroupUseCase { Result.Success(Unit) },
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()

        vm.create("   ")
        advanceUntilIdle()

        assertEquals(FriendGroupError.InvalidGroupName.message, vm.error.value)
    }

    @Test
    fun `delete success reloads the list`() = runTest {
        var stored = listOf(group("g1"))
        val vm = GroupListViewModel(
            listMyGroups = ListMyGroupsUseCase { Result.Success(stored) },
            createGroup = CreateGroupUseCase { Result.Success(group("g2")) },
            deleteGroup = DeleteGroupUseCase { _ ->
                stored = emptyList()
                Result.Success(Unit)
            },
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()

        vm.delete("g1")
        advanceUntilIdle()

        assertEquals(0, vm.groups.value.size)
        assertNull(vm.error.value)
    }
}
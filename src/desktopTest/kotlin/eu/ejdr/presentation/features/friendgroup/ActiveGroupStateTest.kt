package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.SetActiveGroupIdUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveGroupStateTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun state(
        getActiveGroupId: GetActiveGroupIdUseCase = GetActiveGroupIdUseCase { null },
        setActiveGroupId: SetActiveGroupIdUseCase = SetActiveGroupIdUseCase { },
        getGroup: GetGroupUseCase,
    ) = ActiveGroupState(getActiveGroupId, setActiveGroupId, getGroup)

    private fun detail(role: String) = FriendGroupDetail(
        id = "g1",
        name = "G",
        myRole = role,
        createdAt = "2026-01-01",
        members = emptyList(),
    )

    @Test
    fun `select charge le role et canEdit vrai pour MJ`() = runTest {
        val vm = state(getGroup = GetGroupUseCase { Result.Success(detail("MJ")) })
        vm.select("g1")
        advanceUntilIdle()
        assertEquals("MJ", vm.activeGroupRole.value)
        assertTrue(vm.canEdit.value)
    }

    @Test
    fun `canEdit faux pour MEMBER`() = runTest {
        val vm = state(getGroup = GetGroupUseCase { Result.Success(detail("MEMBER")) })
        vm.select("g1")
        advanceUntilIdle()
        assertEquals("MEMBER", vm.activeGroupRole.value)
        assertFalse(vm.canEdit.value)
    }

    @Test
    fun `canEdit vrai pour ADMIN`() = runTest {
        val vm = state(getGroup = GetGroupUseCase { Result.Success(detail("ADMIN")) })
        vm.select("g1")
        advanceUntilIdle()
        assertTrue(vm.canEdit.value)
    }

    @Test
    fun `select null remet le role a null`() = runTest {
        val vm = state(getGroup = GetGroupUseCase { Result.Success(detail("ADMIN")) })
        vm.select("g1")
        advanceUntilIdle()
        vm.select(null)
        advanceUntilIdle()
        assertNull(vm.activeGroupRole.value)
        assertFalse(vm.canEdit.value)
    }
}

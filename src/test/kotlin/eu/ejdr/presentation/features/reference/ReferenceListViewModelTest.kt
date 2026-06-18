package eu.ejdr.presentation.features.reference

import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class ReferenceListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String) = ReferenceItem(id, "N-$id", "2026-06-13T10:00:00.000Z")

    @Test
    fun `loads the items of the given type and active group at init`() = runTest {
        var requestedType: ReferenceType? = null
        var requestedGroup: String? = null
        val vm = ReferenceListViewModel(
            ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { type, groupId ->
                requestedType = type
                requestedGroup = groupId
                Result.Success(listOf(item("a")))
            },
            createItem = CreateReferenceItemUseCase { _, _, _ -> Result.Success(item("a")) },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(ReferenceType.ARME, requestedType)
        assertEquals("g-1", requestedGroup)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
        assertEquals(false, vm.needsGroup.value)
    }

    @Test
    fun `no active group flags onboarding and does not call the use case`() = runTest {
        var called = false
        val vm = ReferenceListViewModel(
            ReferenceType.ARME,
            activeGroupId = MutableStateFlow(null),
            listItems = ListReferenceItemsUseCase { _, _ -> called = true; Result.Success(emptyList()) },
            createItem = CreateReferenceItemUseCase { _, _, _ -> Result.Success(item("a")) },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertTrue(vm.needsGroup.value)
        assertTrue(vm.items.value.isEmpty())
        assertEquals(false, called)
    }

    @Test
    fun `reloads when the active group changes`() = runTest {
        val active = MutableStateFlow<String?>("g-1")
        val vm = ReferenceListViewModel(
            ReferenceType.ARME,
            activeGroupId = active,
            listItems = ListReferenceItemsUseCase { _, groupId -> Result.Success(listOf(item(groupId))) },
            createItem = CreateReferenceItemUseCase { _, _, _ -> Result.Success(item("a")) },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()
        assertEquals("g-1", vm.items.value.first().id)

        active.value = "g-2"
        advanceUntilIdle()

        assertEquals("g-2", vm.items.value.first().id)
    }

    @Test
    fun `create uses the active group and reloads`() = runTest {
        var createdGroup: String? = null
        var stored = emptyList<ReferenceItem>()
        val vm = ReferenceListViewModel(
            ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, groupId ->
                createdGroup = groupId
                stored = listOf(item("f"))
                Result.Success(item("f"))
            },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.create("Rôdeur")
        advanceUntilIdle()

        assertEquals("g-1", createdGroup)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create failure exposes the error`() = runTest {
        val vm = ReferenceListViewModel(
            ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(emptyList()) },
            createItem = CreateReferenceItemUseCase { _, _, _ -> Result.Failure(ReferenceError.NameAlreadyUsed) },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.create("Doublon")
        advanceUntilIdle()

        assertEquals(ReferenceError.NameAlreadyUsed.message, vm.error.value)
    }

    @Test
    fun `delete success reloads`() = runTest {
        var stored = listOf(item("a"))
        val vm = ReferenceListViewModel(
            ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, _ -> Result.Success(item("a")) },
            deleteItem = DeleteReferenceItemUseCase { _, _ ->
                stored = emptyList()
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.delete("a")
        advanceUntilIdle()

        assertEquals(0, vm.items.value.size)
        assertNull(vm.error.value)
    }
}

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
class ReferenceListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String) = ReferenceItem(id, "N-$id", "2026-06-13T10:00:00.000Z")

    @Test
    fun `loads the items of the given type at init`() = runTest {
        var requestedType: ReferenceType? = null
        val vm = ReferenceListViewModel(
            ReferenceType.ARME,
            listItems = ListReferenceItemsUseCase { type ->
                requestedType = type
                Result.Success(listOf(item("a")))
            },
            createItem = CreateReferenceItemUseCase { _, _ -> Result.Success(item("a")) },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(ReferenceType.ARME, requestedType)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create success reloads`() = runTest {
        var stored = emptyList<ReferenceItem>()
        val vm = ReferenceListViewModel(
            ReferenceType.FORMATION,
            listItems = ListReferenceItemsUseCase { Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _ ->
                stored = listOf(item("f"))
                Result.Success(item("f"))
            },
            deleteItem = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.create("Rôdeur")
        advanceUntilIdle()

        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create failure exposes the error`() = runTest {
        val vm = ReferenceListViewModel(
            ReferenceType.FORMATION,
            listItems = ListReferenceItemsUseCase { Result.Success(emptyList()) },
            createItem = CreateReferenceItemUseCase { _, _ -> Result.Failure(ReferenceError.NameAlreadyUsed) },
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
            listItems = ListReferenceItemsUseCase { Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _ -> Result.Success(item("a")) },
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

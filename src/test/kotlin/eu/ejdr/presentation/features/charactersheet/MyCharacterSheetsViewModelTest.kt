package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
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
class MyCharacterSheetsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun sheet(id: String, name: String = "Sheet $id") =
        CharacterSheet(id = id, ownerId = "u-1", name = name, createdAt = "2026-06-13T10:00:00.000Z")

    @Test
    fun `loads sheets at init`() = runTest {
        val vm = MyCharacterSheetsViewModel(
            listSheets = ListCharacterSheetsUseCase { Result.Success(listOf(sheet("s-1"))) },
            createSheet = CreateCharacterSheetUseCase { Result.Success(sheet("x")) },
            deleteSheet = DeleteCharacterSheetUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(1, vm.sheets.value.size)
        assertNull(vm.error.value)
        assertEquals(false, vm.isLoading.value)
    }

    @Test
    fun `exposes error when listing fails`() = runTest {
        val vm = MyCharacterSheetsViewModel(
            listSheets = ListCharacterSheetsUseCase { Result.Failure(CharacterSheetError.Network) },
            createSheet = CreateCharacterSheetUseCase { Result.Success(sheet("x")) },
            deleteSheet = DeleteCharacterSheetUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(CharacterSheetError.Network.message, vm.error.value)
    }

    @Test
    fun `create success reloads the list`() = runTest {
        var listing = listOf(sheet("s-1"))
        val vm = MyCharacterSheetsViewModel(
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
            createSheet = CreateCharacterSheetUseCase { name ->
                listing = listing + sheet("s-2", name)
                Result.Success(sheet("s-2", name))
            },
            deleteSheet = DeleteCharacterSheetUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.create("Nouvelle")
        advanceUntilIdle()

        assertEquals(2, vm.sheets.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `delete success reloads the list`() = runTest {
        var listing = listOf(sheet("s-1"), sheet("s-2"))
        val vm = MyCharacterSheetsViewModel(
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
            createSheet = CreateCharacterSheetUseCase { Result.Success(sheet("x")) },
            deleteSheet = DeleteCharacterSheetUseCase { id ->
                listing = listing.filterNot { it.id == id }
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.delete("s-1")
        advanceUntilIdle()

        assertEquals(1, vm.sheets.value.size)
        assertEquals("s-2", vm.sheets.value.first().id)
    }
}

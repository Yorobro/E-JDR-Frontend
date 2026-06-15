package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.SheetCampaign
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterSheetDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun sheet(id: String = "s-1", name: String = "Aragorn", vigueur: Int? = null) =
        CharacterSheet(
            id = id,
            ownerId = "u-1",
            name = name,
            createdAt = "2026-06-13T10:00:00.000Z",
            vigueur = vigueur,
        )

    @Test
    fun `loads the sheet at init`() = runTest {
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet(vigueur = 6)) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()

        assertEquals("Aragorn", vm.sheet.value?.name)
        assertEquals(6, vm.sheet.value?.vigueur)
        assertNull(vm.error.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `exposes error when loading fails`() = runTest {
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Failure(CharacterSheetError.NotFound) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()

        assertNull(vm.sheet.value)
        assertEquals(CharacterSheetError.NotFound.message, vm.error.value)
    }

    @Test
    fun `startEdit and cancelEdit toggle editing`() = runTest {
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()

        vm.startEdit()
        assertTrue(vm.isEditing.value)

        vm.cancelEdit()
        assertFalse(vm.isEditing.value)
    }

    @Test
    fun `save success stores the returned sheet and exits edit mode`() = runTest {
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()
        vm.startEdit()

        vm.save(sheet(name = "Strider", vigueur = 7))
        advanceUntilIdle()

        assertEquals("Strider", vm.sheet.value?.name)
        assertEquals(7, vm.sheet.value?.vigueur)
        assertFalse(vm.isEditing.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `save failure surfaces the error and stays in edit mode`() = runTest {
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Failure(CharacterSheetError.Network) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()
        vm.startEdit()

        vm.save(sheet(name = "Strider"))
        advanceUntilIdle()

        assertEquals(CharacterSheetError.Network.message, vm.error.value)
        assertTrue(vm.isEditing.value)
    }

    @Test
    fun `loads the linked campaigns at init`() = runTest {
        val campaigns = listOf(SheetCampaign("c-1", "Donjon", "MJ"))
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(campaigns) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()

        assertEquals(campaigns, vm.campaigns.value)
    }

    @Test
    fun `campaigns failure leaves the list empty without overwriting the sheet`() = runTest {
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Failure(CharacterSheetError.Network) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()

        assertTrue(vm.campaigns.value.isEmpty())
        assertEquals("Aragorn", vm.sheet.value?.name)
        assertNull(vm.error.value)
    }

    @Test
    fun `export fetches the pdf and saves it as fiche-name pdf`() = runTest {
        val savedNames = mutableListOf<String>()
        val savedBytes = mutableListOf<ByteArray>()
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet(name = "Aragorn")) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf(37, 80, 68, 70)) },
            fileSaver = FileSaver { name, bytes -> savedNames += name; savedBytes += bytes; true },
        )
        advanceUntilIdle()

        vm.export()
        advanceUntilIdle()

        assertEquals(listOf("fiche-Aragorn.pdf"), savedNames)
        assertContentEquals(byteArrayOf(37, 80, 68, 70), savedBytes.single())
        assertNull(vm.error.value)
        assertFalse(vm.isExporting.value)
    }

    @Test
    fun `export failure surfaces the error and does not save`() = runTest {
        var saveCalls = 0
        val vm = CharacterSheetDetailViewModel(
            sheetId = "s-1",
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Failure(CharacterSheetError.Network) },
            fileSaver = FileSaver { _, _ -> saveCalls++; true },
        )
        advanceUntilIdle()

        vm.export()
        advanceUntilIdle()

        assertEquals(0, saveCalls)
        assertEquals(CharacterSheetError.Network.message, vm.error.value)
    }
}

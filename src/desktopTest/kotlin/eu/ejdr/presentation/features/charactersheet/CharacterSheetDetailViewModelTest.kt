package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.reference.abstraction.usecase.LinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListSheetReferencesUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UnlinkSheetReferenceUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.charactersheet.entities.ResolvedReference
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.domain.features.reference.entities.ReferenceItem
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

    private fun sheet(id: String = "s-1", name: String = "Aragorn", vigueur: Int? = null, ownerId: String = "u-owner") =
        CharacterSheet(
            id = id,
            ownerId = ownerId,
            name = name,
            createdAt = "2026-06-13T10:00:00.000Z",
            vigueur = vigueur,
        )

    /**
     * Construit le ViewModel avec des défauts neutres pour les use cases de référence (listes vides,
     * link/unlink en succès). Les tests qui ne testent pas la référence n'ont qu'à fournir les use
     * cases de fiche ; ceux qui la testent surchargent les paramètres nommés correspondants.
     */
    private fun buildVm(
        getById: GetCharacterSheetUseCase,
        activeGroupId: String? = "g-1",
        update: UpdateCharacterSheetUseCase = UpdateCharacterSheetUseCase { Result.Success(it) },
        exportPdf: ExportCharacterSheetPdfUseCase = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
        fileSaver: FileSaver = FileSaver { _, _ -> true },
        listReferenceItems: ListReferenceItemsUseCase = ListReferenceItemsUseCase { _, _ -> Result.Success(emptyList<ReferenceItem>()) },
        listSheetReferences: ListSheetReferencesUseCase = ListSheetReferencesUseCase { _, _ -> Result.Success(emptyList<ReferenceItem>()) },
        linkSheetReference: LinkSheetReferenceUseCase = LinkSheetReferenceUseCase { _, _, _ -> Result.Success(Unit) },
        unlinkSheetReference: UnlinkSheetReferenceUseCase = UnlinkSheetReferenceUseCase { _, _, _ -> Result.Success(Unit) },
        getCurrentUser: GetCurrentUserUseCase = GetCurrentUserUseCase { Result.Success(User("u-owner", "owner@test.com", "owner")) },
    ) = CharacterSheetDetailViewModel(
        sheetId = "s-1",
        activeGroupId = MutableStateFlow(activeGroupId),
        getById = getById,
        update = update,
        exportPdf = exportPdf,
        fileSaver = fileSaver,
        listReferenceItems = listReferenceItems,
        listSheetReferences = listSheetReferences,
        linkSheetReference = linkSheetReference,
        unlinkSheetReference = unlinkSheetReference,
        getCurrentUser = getCurrentUser,
        invalidationBus = InMemoryInvalidationBus(),
        subscriptions = object : RealtimeSubscriptions {
            override fun subscribe(channel: String) = Unit
            override fun unsubscribe(channel: String) = Unit
            override suspend fun resubscribeAll() = Unit
        },
    )

    @Test
    fun `loads the sheet at init`() = runTest {
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet(vigueur = 6)) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
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
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Failure(CharacterSheetError.NotFound) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
            fileSaver = FileSaver { _, _ -> true },
        )
        advanceUntilIdle()

        assertNull(vm.sheet.value)
        assertEquals(CharacterSheetError.NotFound.message, vm.error.value)
    }

    @Test
    fun `startEdit and cancelEdit toggle editing`() = runTest {
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
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
    fun `save success reloads the sheet via GET and exits edit mode`() = runTest {
        // Le PUT (update) ne renvoie PAS les blocs résolus formation/peuple (calculés côté GET
        // uniquement). Après save, l'écran doit refléter le GET, pas la réponse du PUT — sinon
        // les bonus de stat et compétences dérivées disparaissent jusqu'au prochain chargement.
        val getResult = sheet(name = "Strider", vigueur = 7).copy(
            formation = ResolvedFormation(id = "f-1", name = "Mage", stat = "intelligence", bonus = 3),
            peuple = ResolvedReference(
                id = "p-1",
                name = "Elfe",
                statBonuses = listOf(ReferenceStatBonus("dexterite", 1)),
            ),
        )
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(getResult) },
            // Le PUT renvoie une fiche SANS blocs résolus (comportement réel du back).
            update = UpdateCharacterSheetUseCase { Result.Success(it.copy(formation = null, peuple = null)) },
        )
        advanceUntilIdle()
        vm.startEdit()

        vm.save(sheet(name = "Strider", vigueur = 7))
        advanceUntilIdle()

        assertEquals("Strider", vm.sheet.value?.name)
        assertEquals(7, vm.sheet.value?.vigueur)
        // Les blocs résolus sont présents car save a rechargé via GET.
        assertEquals("Mage", vm.sheet.value?.formation?.name)
        assertEquals(3, vm.sheet.value?.formation?.bonus)
        assertEquals("Elfe", vm.sheet.value?.peuple?.name)
        assertFalse(vm.isEditing.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `save failure surfaces the error and stays in edit mode`() = runTest {
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Failure(CharacterSheetError.Network) },
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
    fun `title carries the campaign on the sheet (campaignName + linkStatus exposed)`() = runTest {
        val accepted = sheet().copy(campaignName = "Donjon", linkStatus = "ACCEPTED")
        val vm = buildVm(getById = GetCharacterSheetUseCase { Result.Success(accepted) })
        advanceUntilIdle()

        assertEquals("Donjon", vm.sheet.value?.campaignName)
        assertEquals("ACCEPTED", vm.sheet.value?.linkStatus)
    }

    @Test
    fun `export fetches the pdf and saves it as fiche-name pdf`() = runTest {
        val savedNames = mutableListOf<String>()
        val savedBytes = mutableListOf<ByteArray>()
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet(name = "Aragorn")) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
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
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            update = UpdateCharacterSheetUseCase { Result.Success(it) },
            exportPdf = ExportCharacterSheetPdfUseCase { Result.Failure(CharacterSheetError.Network) },
            fileSaver = FileSaver { _, _ -> saveCalls++; true },
        )
        advanceUntilIdle()

        vm.export()
        advanceUntilIdle()

        assertEquals(0, saveCalls)
        assertEquals(CharacterSheetError.Network.message, vm.error.value)
    }

    @Test
    fun `loads reference catalogues and linked items at init`() = runTest {
        val item = ReferenceItem("ref-1", "Épée", "2026-06-13T10:00:00.000Z")
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            listReferenceItems = ListReferenceItemsUseCase { _, _ -> Result.Success(listOf(item)) },
            listSheetReferences = ListSheetReferencesUseCase { _, _ -> Result.Success(listOf(item)) },
        )
        advanceUntilIdle()

        assertContentEquals(listOf(item), vm.formations.value)
        // Chaque type liable a sa liste rattachée chargée.
        assertTrue(vm.linked.value.values.all { it == listOf(item) })
    }

    @Test
    fun `linkRef reloads the linked items for that type`() = runTest {
        val arme = ReferenceItem("a-1", "Andúril", "2026-06-13T10:00:00.000Z")
        var linkedNow = emptyList<ReferenceItem>()
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet()) },
            listSheetReferences = ListSheetReferencesUseCase { _, _ -> Result.Success(linkedNow) },
            linkSheetReference = LinkSheetReferenceUseCase { _, _, _ ->
                linkedNow = listOf(arme)
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.linkRef(eu.ejdr.domain.features.reference.entities.ReferenceType.ARME, "a-1")
        advanceUntilIdle()

        assertEquals(
            listOf(arme),
            vm.linked.value[eu.ejdr.domain.features.reference.entities.ReferenceType.ARME],
        )
        assertNull(vm.error.value)
    }

    @Test
    fun `isOwner vrai quand l'utilisateur courant possède la fiche`() = runTest {
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet(ownerId = "u-owner")) },
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User("u-owner", "owner@test.com", "owner")) },
        )
        advanceUntilIdle()

        assertTrue(vm.isOwner.value)
    }

    @Test
    fun `isOwner faux quand un autre utilisateur`() = runTest {
        val vm = buildVm(
            getById = GetCharacterSheetUseCase { Result.Success(sheet(ownerId = "u-owner")) },
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User("autre", "autre@test.com", "autre")) },
        )
        advanceUntilIdle()

        assertFalse(vm.isOwner.value)
    }
}

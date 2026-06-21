package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ExportCharacterSheetPdfUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.GetSheetCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UpdateCharacterSheetUseCase
import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.reference.abstraction.usecase.LinkSheetReferenceUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListSheetReferencesUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UnlinkSheetReferenceUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterSheetDetailRealtimeTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class RecordingSubscriptions : RealtimeSubscriptions {
        val subscribed = mutableListOf<String>()
        val unsubscribed = mutableListOf<String>()
        override fun subscribe(channel: String) { subscribed.add(channel) }
        override fun unsubscribe(channel: String) { unsubscribed.add(channel) }
        override suspend fun resubscribeAll() = Unit
    }

    private fun sheet(id: String = "s-1") = CharacterSheet(
        id = id,
        ownerId = "u-owner",
        name = "Aragorn",
        createdAt = "2026-06-13T10:00:00.000Z",
    )

    private fun buildVm(
        sheetId: String = "s-1",
        bus: InMemoryInvalidationBus = InMemoryInvalidationBus(),
        subscriptions: RealtimeSubscriptions = RecordingSubscriptions(),
        onGetById: () -> Unit = {},
    ) = CharacterSheetDetailViewModel(
        sheetId = sheetId,
        activeGroupId = MutableStateFlow("g-1"),
        getById = GetCharacterSheetUseCase { onGetById(); Result.Success(sheet(sheetId)) },
        update = UpdateCharacterSheetUseCase { Result.Success(it) },
        getCampaigns = GetSheetCampaignsUseCase { Result.Success(emptyList()) },
        exportPdf = ExportCharacterSheetPdfUseCase { Result.Success(byteArrayOf()) },
        fileSaver = FileSaver { _, _ -> true },
        listReferenceItems = ListReferenceItemsUseCase { _, _ -> Result.Success(emptyList<ReferenceItem>()) },
        listSheetReferences = ListSheetReferencesUseCase { _, _ -> Result.Success(emptyList<ReferenceItem>()) },
        linkSheetReference = LinkSheetReferenceUseCase { _, _, _ -> Result.Success(Unit) },
        unlinkSheetReference = UnlinkSheetReferenceUseCase { _, _, _ -> Result.Success(Unit) },
        getCurrentUser = GetCurrentUserUseCase { Result.Success(User("u-owner", "owner@test.com", "owner")) },
        invalidationBus = bus,
        subscriptions = subscriptions,
    )

    @Test
    fun `s'abonne au canal sheet à l'init`() = runTest {
        val subs = RecordingSubscriptions()
        buildVm(sheetId = "s-1", subscriptions = subs)
        advanceUntilIdle()
        assertEquals(listOf("sheet:s-1"), subs.subscribed)
    }

    @Test
    fun `recharge sur invalidation character-sheet-detail hors édition`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        val before = loadCount
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        assertTrue(loadCount > before, "doit recharger")
    }

    @Test
    fun `lève le bandeau sans recharger pendant l'édition`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        vm.startEdit()
        val before = loadCount
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        assertEquals(before, loadCount, "ne doit PAS recharger en édition")
        assertTrue(vm.sheetChangedRemotely.value)
    }

    @Test
    fun `ignore une invalidation d'un autre scopeId ou resource`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        val before = loadCount
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "autre"))
        bus.emit(Invalidation(resource = "character-sheets", scopeId = "s-1"))
        advanceUntilIdle()
        assertEquals(before, loadCount)
    }

    @Test
    fun `reloadFromRemote recharge, baisse le flag et sort de l'édition`() = runTest {
        val bus = InMemoryInvalidationBus()
        val vm = buildVm(sheetId = "s-1", bus = bus)
        advanceUntilIdle()
        vm.startEdit()
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        vm.reloadFromRemote()
        advanceUntilIdle()
        assertEquals(false, vm.sheetChangedRemotely.value)
        assertEquals(false, vm.isEditing.value)
    }

    @Test
    fun `dismissRemoteChange baisse le flag sans recharger`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        vm.startEdit()
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        val before = loadCount
        vm.dismissRemoteChange()
        advanceUntilIdle()
        assertEquals(false, vm.sheetChangedRemotely.value)
        assertEquals(before, loadCount)
    }
}

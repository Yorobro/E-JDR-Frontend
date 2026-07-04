package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CopyCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
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

@OptIn(ExperimentalCoroutinesApi::class)
class MyCharacterSheetsRealtimeTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun sheet(id: String) =
        CharacterSheet(
            id = id,
            ownerId = "u1",
            name = "Fiche $id",
            createdAt = "2026-06-21T10:00:00.000Z",
        )

    @Test
    fun `recharge la liste quand une invalidation character-sheets arrive`() = runTest {
        val bus = InMemoryInvalidationBus()
        // 1re lecture : 1 fiche ; après invalidation : 2 fiches.
        var listing = listOf(sheet("s-1"))
        val vm = MyCharacterSheetsViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
            createSheet = CreateCharacterSheetUseCase { _, _, _ -> Result.Success(sheet("x")) },
            deleteSheet = DeleteCharacterSheetUseCase { Result.Success(Unit) },
            copySheet = CopyCharacterSheetUseCase { _, _ -> Result.Success(sheet("copy")) },
            listCampaigns = ListCampaignsUseCase { Result.Success(emptyList()) },
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User(id = "u1", email = "a@b.c", pseudo = "A")) },
            invalidationBus = bus,
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()
        assertEquals(1, vm.sheets.value.size)

        // Une invalidation « character-sheets » doit déclencher un rechargement.
        listing = listOf(sheet("s-1"), sheet("s-2"))
        bus.emit(Invalidation(resource = "character-sheets", scopeId = "u1"))
        advanceUntilIdle()

        assertEquals(2, vm.sheets.value.size)
    }

    @Test
    fun `ignore une invalidation d'une autre ressource`() = runTest {
        val bus = InMemoryInvalidationBus()
        var listing = listOf(sheet("s-1"))
        val vm = MyCharacterSheetsViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
            createSheet = CreateCharacterSheetUseCase { _, _, _ -> Result.Success(sheet("x")) },
            deleteSheet = DeleteCharacterSheetUseCase { Result.Success(Unit) },
            copySheet = CopyCharacterSheetUseCase { _, _ -> Result.Success(sheet("copy")) },
            listCampaigns = ListCampaignsUseCase { Result.Success(emptyList()) },
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User(id = "u1", email = "a@b.c", pseudo = "A")) },
            invalidationBus = bus,
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()

        listing = listOf(sheet("s-1"), sheet("s-2"))
        bus.emit(Invalidation(resource = "campaigns", scopeId = "g-1"))
        advanceUntilIdle()

        // Pas de rechargement : la ressource ne concerne pas cet écran.
        assertEquals(1, vm.sheets.value.size)
    }
}
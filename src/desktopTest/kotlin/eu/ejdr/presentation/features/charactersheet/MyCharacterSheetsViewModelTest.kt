package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CopyCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.CreateCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.DeleteCharacterSheetUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCharacterSheetsUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyCharacterSheetsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun sheet(id: String, name: String = "Sheet $id") =
        CharacterSheet(id = id, ownerId = "u-1", name = name, createdAt = "2026-06-13T10:00:00.000Z")

    private fun campaign(id: String, gameMasterId: String) =
        Campaign(id = id, name = "Campagne $id", gameMasterId = gameMasterId, createdAt = "2026-06-13T10:00:00.000Z")

    private fun buildVm(
        activeGroupId: String? = "g-1",
        listSheets: ListCharacterSheetsUseCase = ListCharacterSheetsUseCase { Result.Success(emptyList()) },
        createSheet: CreateCharacterSheetUseCase = CreateCharacterSheetUseCase { _, _, _ -> Result.Success(sheet("x")) },
        deleteSheet: DeleteCharacterSheetUseCase = DeleteCharacterSheetUseCase { Result.Success(Unit) },
        copySheet: CopyCharacterSheetUseCase = CopyCharacterSheetUseCase { _, _ -> Result.Success(sheet("copy")) },
        listCampaigns: ListCampaignsUseCase = ListCampaignsUseCase { Result.Success(emptyList()) },
        getCurrentUser: GetCurrentUserUseCase = GetCurrentUserUseCase { Result.Success(User("u-current", "current@test.com", "current")) },
        invalidationBus: InvalidationBus = InMemoryInvalidationBus(),
    ) = MyCharacterSheetsViewModel(
        activeGroupId = MutableStateFlow(activeGroupId),
        listSheets = listSheets,
        createSheet = createSheet,
        deleteSheet = deleteSheet,
        copySheet = copySheet,
        listCampaigns = listCampaigns,
        getCurrentUser = getCurrentUser,
        invalidationBus = invalidationBus,
        uiMessageBus = io.mockk.mockk(relaxed = true),
    )

    @Test
    fun `loads sheets of the active group at init`() = runTest {
        var requestedGroup: String? = null
        val vm = buildVm(
            activeGroupId = "g-1",
            listSheets = ListCharacterSheetsUseCase { groupId ->
                requestedGroup = groupId
                Result.Success(listOf(sheet("s-1")))
            },
        )
        advanceUntilIdle()

        assertEquals("g-1", requestedGroup)
        assertEquals(1, vm.sheets.value.size)
        assertNull(vm.error.value)
        assertEquals(false, vm.isLoading.value)
        assertEquals(false, vm.needsGroup.value)
    }

    @Test
    fun `no active group flags onboarding and does not call the use case`() = runTest {
        var called = false
        val vm = buildVm(
            activeGroupId = null,
            listSheets = ListCharacterSheetsUseCase { called = true; Result.Success(emptyList()) },
        )
        advanceUntilIdle()

        assertTrue(vm.needsGroup.value)
        assertTrue(vm.sheets.value.isEmpty())
        assertEquals(false, called)
    }

    @Test
    fun `reloads when the active group changes`() = runTest {
        val active = MutableStateFlow<String?>("g-1")
        val vm = MyCharacterSheetsViewModel(
            activeGroupId = active,
            listSheets = ListCharacterSheetsUseCase { groupId -> Result.Success(listOf(sheet(groupId))) },
            createSheet = CreateCharacterSheetUseCase { _, _, _ -> Result.Success(sheet("x")) },
            deleteSheet = DeleteCharacterSheetUseCase { Result.Success(Unit) },
            copySheet = CopyCharacterSheetUseCase { _, _ -> Result.Success(sheet("copy")) },
            listCampaigns = ListCampaignsUseCase { Result.Success(emptyList()) },
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User("u-current", "current@test.com", "current")) },
            invalidationBus = InMemoryInvalidationBus(),
            uiMessageBus = io.mockk.mockk(relaxed = true),
        )
        advanceUntilIdle()
        assertEquals("g-1", vm.sheets.value.first().id)

        active.value = "g-2"
        advanceUntilIdle()

        assertEquals("g-2", vm.sheets.value.first().id)
    }

    @Test
    fun `exposes error when listing fails`() = runTest {
        val vm = buildVm(
            listSheets = ListCharacterSheetsUseCase { Result.Failure(CharacterSheetError.Network) },
        )
        advanceUntilIdle()

        assertEquals(CharacterSheetError.Network.message, vm.error.value)
    }

    @Test
    fun `create uses the active group and the chosen campaign then reloads the list`() = runTest {
        var createdGroup: String? = null
        var createdCampaign: String? = null
        var listing = listOf(sheet("s-1"))
        val vm = buildVm(
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
            createSheet = CreateCharacterSheetUseCase { name, groupId, campaignId ->
                createdGroup = groupId
                createdCampaign = campaignId
                listing = listing + sheet("s-2", name)
                Result.Success(sheet("s-2", name))
            },
        )
        advanceUntilIdle()

        vm.create("Nouvelle", "c-1")
        advanceUntilIdle()

        assertEquals("g-1", createdGroup)
        assertEquals("c-1", createdCampaign)
        assertEquals(2, vm.sheets.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `copy delegates to the copy use case then reloads the list`() = runTest {
        var copiedSheet: String? = null
        var copiedCampaign: String? = null
        var listing = listOf(sheet("s-1"))
        val vm = buildVm(
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
            copySheet = CopyCharacterSheetUseCase { sheetId, campaignId ->
                copiedSheet = sheetId
                copiedCampaign = campaignId
                listing = listing + sheet("s-2")
                Result.Success(sheet("s-2"))
            },
        )
        advanceUntilIdle()

        vm.copy("s-1", "c-2")
        advanceUntilIdle()

        assertEquals("s-1", copiedSheet)
        assertEquals("c-2", copiedCampaign)
        assertEquals(2, vm.sheets.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `eligibleCampaigns excludes campaigns where the user is the game master`() = runTest {
        val vm = buildVm(
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User("u-42", "u42@test.com", "u42")) },
            listCampaigns = ListCampaignsUseCase {
                Result.Success(listOf(campaign("c-own", "u-42"), campaign("c-other", "u-99")))
            },
        )
        advanceUntilIdle()

        assertEquals(listOf("c-other"), vm.eligibleCampaigns.value.map { it.id })
    }

    @Test
    fun `delete success reloads the list`() = runTest {
        var listing = listOf(sheet("s-1"), sheet("s-2"))
        val vm = buildVm(
            listSheets = ListCharacterSheetsUseCase { Result.Success(listing) },
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

    @Test
    fun `currentUserId expose l'id de l'utilisateur courant après chargement`() = runTest {
        val vm = buildVm(
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User("u-42", "u42@test.com", "u42")) },
        )
        advanceUntilIdle()

        assertEquals("u-42", vm.currentUserId.value)
    }
}
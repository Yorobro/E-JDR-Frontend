package eu.ejdr.presentation.features.campaign

import eu.ejdr.application.features.campaign.abstraction.usecase.CreateCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.DeleteCampaignUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.campaign.error.CampaignError
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
class CampaignListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun campaign(id: String, name: String = "Campagne $id") =
        Campaign(id = id, name = name, createdAt = "2026-06-13T10:00:00.000Z")

    @Test
    fun `loads campaigns of the active group at init`() = runTest {
        var requestedGroup: String? = null
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listCampaigns = ListCampaignsUseCase { groupId ->
                requestedGroup = groupId
                Result.Success(listOf(campaign("c-1")))
            },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Success(campaign("x")) },
            deleteCampaign = DeleteCampaignUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals("g-1", requestedGroup)
        assertEquals(1, vm.campaigns.value.size)
        assertEquals("c-1", vm.campaigns.value.first().id)
        assertNull(vm.error.value)
        assertEquals(false, vm.isLoading.value)
        assertEquals(false, vm.needsGroup.value)
    }

    @Test
    fun `no active group flags onboarding and does not call the use case`() = runTest {
        var called = false
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow(null),
            listCampaigns = ListCampaignsUseCase { called = true; Result.Success(emptyList()) },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Success(campaign("x")) },
            deleteCampaign = DeleteCampaignUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertTrue(vm.needsGroup.value)
        assertTrue(vm.campaigns.value.isEmpty())
        assertEquals(false, called)
    }

    @Test
    fun `reloads campaigns when the active group changes`() = runTest {
        val active = MutableStateFlow<String?>("g-1")
        val vm = CampaignListViewModel(
            activeGroupId = active,
            listCampaigns = ListCampaignsUseCase { groupId ->
                Result.Success(listOf(campaign("c-$groupId")))
            },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Success(campaign("x")) },
            deleteCampaign = DeleteCampaignUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()
        assertEquals("c-g-1", vm.campaigns.value.first().id)

        active.value = "g-2"
        advanceUntilIdle()

        assertEquals("c-g-2", vm.campaigns.value.first().id)
    }

    @Test
    fun `exposes error message when listing fails`() = runTest {
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listCampaigns = ListCampaignsUseCase { Result.Failure(CampaignError.Network) },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Success(campaign("x")) },
            deleteCampaign = DeleteCampaignUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(CampaignError.Network.message, vm.error.value)
        assertTrue(vm.campaigns.value.isEmpty())
    }

    @Test
    fun `create uses the active group and reloads the list`() = runTest {
        var createdGroup: String? = null
        var listing = listOf(campaign("c-1"))
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listCampaigns = ListCampaignsUseCase { Result.Success(listing) },
            createCampaign = CreateCampaignUseCase { name, groupId ->
                createdGroup = groupId
                listing = listing + campaign("c-2", name)
                Result.Success(campaign("c-2", name))
            },
            deleteCampaign = DeleteCampaignUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.create("Nouvelle")
        advanceUntilIdle()

        assertEquals("g-1", createdGroup)
        assertEquals(2, vm.campaigns.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create failure exposes error and keeps the list`() = runTest {
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listCampaigns = ListCampaignsUseCase { Result.Success(listOf(campaign("c-1"))) },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Failure(CampaignError.InvalidName) },
            deleteCampaign = DeleteCampaignUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.create("   ")
        advanceUntilIdle()

        assertEquals(CampaignError.InvalidName.message, vm.error.value)
        assertEquals(1, vm.campaigns.value.size)
    }

    @Test
    fun `delete success reloads the list`() = runTest {
        var listing = listOf(campaign("c-1"), campaign("c-2"))
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listCampaigns = ListCampaignsUseCase { Result.Success(listing) },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Success(campaign("x")) },
            deleteCampaign = DeleteCampaignUseCase { id ->
                listing = listing.filterNot { it.id == id }
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.delete("c-1")
        advanceUntilIdle()

        assertEquals(1, vm.campaigns.value.size)
        assertEquals("c-2", vm.campaigns.value.first().id)
        assertNull(vm.error.value)
    }

    @Test
    fun `delete failure exposes error`() = runTest {
        val vm = CampaignListViewModel(
            activeGroupId = MutableStateFlow("g-1"),
            listCampaigns = ListCampaignsUseCase { Result.Success(listOf(campaign("c-1"))) },
            createCampaign = CreateCampaignUseCase { _, _ -> Result.Success(campaign("x")) },
            deleteCampaign = DeleteCampaignUseCase { Result.Failure(CampaignError.AccessDenied) },
        )
        advanceUntilIdle()

        vm.delete("c-1")
        advanceUntilIdle()

        assertEquals(CampaignError.AccessDenied.message, vm.error.value)
    }
}

package eu.ejdr.presentation.features.campaign

import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
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
class CampaignDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun sheet(id: String) =
        CharacterSheet(id = id, ownerId = "u-1", name = "S-$id", createdAt = "2026-06-13T10:00:00.000Z")

    @Test
    fun `loads campaign characters and linkable sheets at init`() = runTest {
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(listOf(sheet("a"))) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(listOf(sheet("a"), sheet("b"))) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, _ -> Result.Success(Unit) },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals(1, vm.characters.value.size)
        assertEquals(2, vm.linkableSheets.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `link success reloads`() = runTest {
        var linked = emptyList<CharacterSheet>()
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(linked) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(listOf(sheet("a"))) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, id ->
                linked = listOf(sheet(id))
                Result.Success(Unit)
            },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.link("a")
        advanceUntilIdle()

        assertEquals(1, vm.characters.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `link failure (GM rule) exposes the error message`() = runTest {
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(emptyList()) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(listOf(sheet("a"))) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, _ ->
                Result.Failure(CharacterSheetError.GmCannotJoinOwnCampaign)
            },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.link("a")
        advanceUntilIdle()

        assertEquals(CharacterSheetError.GmCannotJoinOwnCampaign.message, vm.error.value)
    }
}

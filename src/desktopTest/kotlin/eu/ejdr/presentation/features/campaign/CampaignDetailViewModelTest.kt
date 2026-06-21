package eu.ejdr.presentation.features.campaign

import eu.ejdr.application.features.charactersheet.abstraction.usecase.LinkCharacterToCampaignUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListLinkableCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.UnlinkCharacterFromCampaignUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.ListCampaignSessionsUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.error.CharacterSheetError
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError
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

    private fun session(id: String) =
        Session(id = id, campaignId = "camp-1", title = "T-$id", date = "2026-06-20", createdAt = "2026-06-13T10:00:00.000Z")

    /** Use case sessions par défaut : liste vide. */
    private val emptySessions = ListCampaignSessionsUseCase { Result.Success(emptyList<Session>()) }

    /** Use case création de session par défaut : succès. */
    private val createSessionOk = CreateSessionUseCase { _, _, _ -> Result.Success(session("new")) }

    @Test
    fun `loads campaign characters and linkable sheets at init`() = runTest {
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(listOf(sheet("a"))) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(listOf(sheet("a"), sheet("b"))) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, _ -> Result.Success(Unit) },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
            listCampaignSessions = emptySessions,
            createSession = createSessionOk,
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
            listCampaignSessions = emptySessions,
            createSession = createSessionOk,
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
            listCampaignSessions = emptySessions,
            createSession = createSessionOk,
        )
        advanceUntilIdle()

        vm.link("a")
        advanceUntilIdle()

        assertEquals(CharacterSheetError.GmCannotJoinOwnCampaign.message, vm.error.value)
    }

    @Test
    fun `loads sessions at init`() = runTest {
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(emptyList()) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(emptyList()) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, _ -> Result.Success(Unit) },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
            listCampaignSessions = ListCampaignSessionsUseCase { Result.Success(listOf(session("a"), session("b"))) },
            createSession = createSessionOk,
        )
        advanceUntilIdle()

        assertEquals(2, vm.sessions.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `createSession success reloads the sessions`() = runTest {
        var stored = emptyList<Session>()
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(emptyList()) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(emptyList()) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, _ -> Result.Success(Unit) },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
            listCampaignSessions = ListCampaignSessionsUseCase { Result.Success(stored) },
            createSession = CreateSessionUseCase { _, _, _ ->
                stored = listOf(session("new"))
                Result.Success(session("new"))
            },
        )
        advanceUntilIdle()

        vm.createSession("Titre", "2026-06-20")
        advanceUntilIdle()

        assertEquals(1, vm.sessions.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `createSession failure exposes the error message`() = runTest {
        val vm = CampaignDetailViewModel(
            campaignId = "camp-1",
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(emptyList()) },
            listLinkable = ListLinkableCharactersUseCase { Result.Success(emptyList()) },
            linkCharacter = LinkCharacterToCampaignUseCase { _, _ -> Result.Success(Unit) },
            unlinkCharacter = UnlinkCharacterFromCampaignUseCase { _, _ -> Result.Success(Unit) },
            listCampaignSessions = emptySessions,
            createSession = CreateSessionUseCase { _, _, _ -> Result.Failure(SessionError.InvalidDate) },
        )
        advanceUntilIdle()

        vm.createSession("Titre", "bad")
        advanceUntilIdle()

        assertEquals(SessionError.InvalidDate.message, vm.error.value)
    }
}

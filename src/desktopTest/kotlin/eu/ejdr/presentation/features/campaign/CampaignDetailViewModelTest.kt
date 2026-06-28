package eu.ejdr.presentation.features.campaign

import eu.ejdr.application.features.charactersheet.abstraction.usecase.AcceptCharacterUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListCampaignCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.ListPendingCharactersUseCase
import eu.ejdr.application.features.charactersheet.abstraction.usecase.RefuseCharacterUseCase
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

    private fun buildVm(
        listCampaignCharacters: ListCampaignCharactersUseCase = ListCampaignCharactersUseCase { Result.Success(emptyList()) },
        listPendingCharacters: ListPendingCharactersUseCase = ListPendingCharactersUseCase { Result.Success(emptyList()) },
        acceptCharacter: AcceptCharacterUseCase = AcceptCharacterUseCase { _, _ -> Result.Success(Unit) },
        refuseCharacter: RefuseCharacterUseCase = RefuseCharacterUseCase { _, _ -> Result.Success(Unit) },
        listCampaignSessions: ListCampaignSessionsUseCase = emptySessions,
        createSession: CreateSessionUseCase = createSessionOk,
    ) = CampaignDetailViewModel(
        campaignId = "camp-1",
        listCampaignCharacters = listCampaignCharacters,
        listPendingCharacters = listPendingCharacters,
        acceptCharacter = acceptCharacter,
        refuseCharacter = refuseCharacter,
        listCampaignSessions = listCampaignSessions,
        createSession = createSession,
        uiMessageBus = io.mockk.mockk(relaxed = true),
    )

    @Test
    fun `loads accepted characters and pending requests at init`() = runTest {
        val vm = buildVm(
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(listOf(sheet("a"))) },
            listPendingCharacters = ListPendingCharactersUseCase { Result.Success(listOf(sheet("b"), sheet("c"))) },
        )
        advanceUntilIdle()

        assertEquals(1, vm.characters.value.size)
        assertEquals(2, vm.pendingCharacters.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `accept success reloads`() = runTest {
        var accepted: List<CharacterSheet> = emptyList()
        var pending = listOf(sheet("a"))
        val vm = buildVm(
            listCampaignCharacters = ListCampaignCharactersUseCase { Result.Success(accepted) },
            listPendingCharacters = ListPendingCharactersUseCase { Result.Success(pending) },
            acceptCharacter = AcceptCharacterUseCase { _, id ->
                accepted = listOf(sheet(id))
                pending = emptyList()
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.accept("a")
        advanceUntilIdle()

        assertEquals(1, vm.characters.value.size)
        assertEquals(0, vm.pendingCharacters.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `refuse success reloads and removes the pending request`() = runTest {
        var pending = listOf(sheet("a"))
        val vm = buildVm(
            listPendingCharacters = ListPendingCharactersUseCase { Result.Success(pending) },
            refuseCharacter = RefuseCharacterUseCase { _, _ ->
                pending = emptyList()
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()
        assertEquals(1, vm.pendingCharacters.value.size)

        vm.refuse("a")
        advanceUntilIdle()

        assertEquals(0, vm.pendingCharacters.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `accept failure exposes the error message`() = runTest {
        val vm = buildVm(
            listPendingCharacters = ListPendingCharactersUseCase { Result.Success(listOf(sheet("a"))) },
            acceptCharacter = AcceptCharacterUseCase { _, _ -> Result.Failure(CharacterSheetError.Network) },
        )
        advanceUntilIdle()

        vm.accept("a")
        advanceUntilIdle()

        assertEquals(CharacterSheetError.Network.message, vm.error.value)
    }

    @Test
    fun `loads sessions at init`() = runTest {
        val vm = buildVm(
            listCampaignSessions = ListCampaignSessionsUseCase { Result.Success(listOf(session("a"), session("b"))) },
        )
        advanceUntilIdle()

        assertEquals(2, vm.sessions.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `createSession success reloads the sessions`() = runTest {
        var stored = emptyList<Session>()
        val vm = buildVm(
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
        val vm = buildVm(
            createSession = CreateSessionUseCase { _, _, _ -> Result.Failure(SessionError.InvalidDate) },
        )
        advanceUntilIdle()

        vm.createSession("Titre", "bad")
        advanceUntilIdle()

        assertEquals(SessionError.InvalidDate.message, vm.error.value)
    }
}

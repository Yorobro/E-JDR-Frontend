package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.campaign.abstraction.usecase.ListCampaignsUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.session.abstraction.usecase.CreateLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import eu.ejdr.domain.features.friendgroup.entities.GroupMember
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class SessionDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun session(
        id: String = "s-1",
        title: String = "Intro",
        date: String = "2026-06-20",
        status: String = "PLANNED",
    ) = Session(
        id = id,
        campaignId = "camp-1",
        title = title,
        date = date,
        status = status,
        createdAt = "2026-06-13T10:00:00.000Z",
    )

    /**
     * Construit le ViewModel avec des doublures par défaut ; chaque test surcharge ce qui l'intéresse.
     * Le MJ courant a l'id `u-mj` ; le groupe par défaut le contient avec un autre membre `u-player`.
     */
    private fun buildViewModel(
        getById: GetSessionUseCase = GetSessionUseCase { Result.Success(session()) },
        update: UpdateSessionUseCase = UpdateSessionUseCase { _, _, _ -> Result.Success(session()) },
        deleteSession: DeleteSessionUseCase = DeleteSessionUseCase { Result.Success(Unit) },
        createLobby: CreateLobbyUseCase = CreateLobbyUseCase { _, _ ->
            Result.Success(SessionLobby("s-1", "LOBBY", emptyList()))
        },
        getGroup: GetGroupUseCase = GetGroupUseCase {
            Result.Success(
                FriendGroupDetail(
                    id = "g-1",
                    name = "Groupe",
                    myRole = "MJ",
                    createdAt = "2026-06-13T10:00:00.000Z",
                    members = listOf(
                        GroupMember("u-mj", "MJ", "MJ", "2026-06-13T10:00:00.000Z"),
                        GroupMember("u-player", "Joueur", "MEMBER", "2026-06-13T10:00:00.000Z"),
                    ),
                ),
            )
        },
        getCurrentUser: GetCurrentUserUseCase = GetCurrentUserUseCase {
            Result.Success(User("u-mj", "mj@test.com", "MJ"))
        },
        // Par défaut, l'utilisateur courant (u-mj) est le MJ de la campagne parente (camp-1).
        listCampaigns: ListCampaignsUseCase = ListCampaignsUseCase {
            Result.Success(listOf(Campaign("camp-1", "Campagne", gameMasterId = "u-mj", createdAt = "2026-06-13T10:00:00.000Z")))
        },
        activeGroupId: StateFlow<String?> = MutableStateFlow("g-1"),
        lobbyState: SessionLobbyState = SessionLobbyState(),
    ) = SessionDetailViewModel(
        sessionId = "s-1",
        activeGroupId = activeGroupId,
        getById = getById,
        update = update,
        deleteSession = deleteSession,
        createLobby = createLobby,
        getGroup = getGroup,
        getCurrentUser = getCurrentUser,
        listCampaigns = listCampaigns,
        uiMessageBus = io.mockk.mockk(relaxed = true),
        lobbyState = lobbyState,
    )

    @Test
    fun `loads the session at init`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("Intro", vm.session.value?.title)
        assertNull(vm.error.value)
    }

    @Test
    fun `isGameMaster is true when the current user owns the parent campaign`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.isGameMaster.value)
    }

    @Test
    fun `isGameMaster is false when the current user is not the campaign MJ`() = runTest {
        val vm = buildViewModel(
            getCurrentUser = GetCurrentUserUseCase { Result.Success(User("u-player", "p@test.com", "Joueur")) },
        )
        advanceUntilIdle()

        assertEquals(false, vm.isGameMaster.value)
    }

    @Test
    fun `save success updates the session`() = runTest {
        val vm = buildViewModel(
            update = UpdateSessionUseCase { _, title, date -> Result.Success(session(title = title, date = date)) },
        )
        advanceUntilIdle()

        vm.save("Après", "2026-07-01")
        advanceUntilIdle()

        assertEquals("Après", vm.session.value?.title)
        assertEquals("2026-07-01", vm.session.value?.date)
        assertNull(vm.error.value)
    }

    @Test
    fun `save failure exposes the error message`() = runTest {
        val vm = buildViewModel(
            update = UpdateSessionUseCase { _, _, _ -> Result.Failure(SessionError.InvalidTitle) },
        )
        advanceUntilIdle()

        vm.save("", "2026-07-01")
        advanceUntilIdle()

        assertEquals(SessionError.InvalidTitle.message, vm.error.value)
    }

    @Test
    fun `delete success flags deleted`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertTrue(vm.deleted.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `delete failure exposes the error and does not flag deleted`() = runTest {
        val vm = buildViewModel(
            deleteSession = DeleteSessionUseCase { Result.Failure(SessionError.AccessDenied) },
        )
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertEquals(SessionError.AccessDenied.message, vm.error.value)
        assertEquals(false, vm.deleted.value)
    }

    @Test
    fun `loadSelectableMembers excludes the current MJ`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.loadSelectableMembers("g-1")
        advanceUntilIdle()

        assertEquals(listOf("u-player"), vm.selectableMembers.value.map { it.userId })
    }

    @Test
    fun `openLobby success flags lobbyOpened`() = runTest {
        val vm = buildViewModel(
            createLobby = CreateLobbyUseCase { _, _ -> Result.Success(SessionLobby("s-1", "LOBBY", emptyList())) },
        )
        advanceUntilIdle()

        vm.openLobby(listOf("u-player"))
        advanceUntilIdle()

        assertTrue(vm.lobbyOpened.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `openLobby success stores the lobby in the shared state`() = runTest {
        val lobbyState = SessionLobbyState()
        val vm = buildViewModel(
            createLobby = CreateLobbyUseCase { _, _ ->
                Result.Success(
                    SessionLobby(
                        sessionId = "s-1",
                        status = "LOBBY",
                        participants = listOf(LobbyParticipant("u-player", "INVITED", null)),
                    ),
                )
            },
            lobbyState = lobbyState,
        )
        advanceUntilIdle()
        vm.loadSelectableMembers("g-1")
        advanceUntilIdle()

        vm.openLobby(listOf("u-player"))
        advanceUntilIdle()

        assertEquals("s-1", lobbyState.lobby.value?.sessionId)
        assertEquals(listOf("u-player"), lobbyState.lobby.value?.participants?.map { it.userId })
        // Les membres conviables (hors MJ) sont transmis au lobby pour résoudre les pseudos.
        assertEquals(listOf("u-player"), lobbyState.members.value.map { it.userId })
    }

    @Test
    fun `consumeLobbyOpened resets the flag`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.openLobby(listOf("u-player"))
        advanceUntilIdle()
        assertTrue(vm.lobbyOpened.value)

        vm.consumeLobbyOpened()
        assertEquals(false, vm.lobbyOpened.value)
    }

    @Test
    fun `openLobby failure exposes the error and does not flag lobbyOpened`() = runTest {
        val vm = buildViewModel(
            createLobby = CreateLobbyUseCase { _, _ -> Result.Failure(SessionError.SessionNotLaunchable) },
        )
        advanceUntilIdle()

        vm.openLobby(listOf("u-player"))
        advanceUntilIdle()

        assertEquals(SessionError.SessionNotLaunchable.message, vm.error.value)
        assertEquals(false, vm.lobbyOpened.value)
    }
}

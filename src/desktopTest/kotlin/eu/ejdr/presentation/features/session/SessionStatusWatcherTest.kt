package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.domain.features.session.error.SessionError
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du [SessionStatusWatcher] : la veille globale qui fait basculer le raccourci de session de
 * « Retour au salon » à « Rejoindre la partie » quand le MJ démarre, **y compris** chez un
 * utilisateur qui a quitté la page du salon — le seul, justement, à en avoir besoin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionStatusWatcherTest {

    /** Lecture REST du salon, enregistrant les identifiants demandés. */
    private class FakeGetSessionLobby(private val status: String) : GetSessionLobbyUseCase {
        val calls = mutableListOf<String>()
        override suspend fun invoke(sessionId: String): Result<SessionLobby, SessionError> {
            calls.add(sessionId)
            return Result.Success(lobby(status))
        }
    }

    /** Lecture REST en échec, pour vérifier qu'on conserve l'état courant. */
    private class FailingGetSessionLobby : GetSessionLobbyUseCase {
        override suspend fun invoke(sessionId: String): Result<SessionLobby, SessionError> =
            Result.Failure(SessionError.Network)
    }

    /**
     * Construit la veille sur un dispatcher **non confiné** : son abonnement au bus est alors actif
     * dès la construction. Le bus est un `SharedFlow` sans replay — un event émis avant
     * l'abonnement serait perdu, et le test passerait à côté de ce qu'il vérifie.
     */
    private fun TestScope.watcherOn(
        bus: InMemoryInvalidationBus,
        lobbyState: SessionLobbyState,
        getSessionLobby: GetSessionLobbyUseCase,
    ) = SessionStatusWatcher(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        bus,
        lobbyState,
        getSessionLobby,
    )

    /** État partagé tel qu'il est chez un joueur ayant rejoint le salon puis quitté la page. */
    private fun stateInLobby() = SessionLobbyState().apply {
        open(lobby("LOBBY"), title = "La Tour de Guet", members = emptyList(), canManage = false)
    }

    @Test
    fun `le demarrage bascule le raccourci vers l'ecran de jeu`() = runTest {
        val bus = InMemoryInvalidationBus()
        val state = stateInLobby()
        val getLobby = FakeGetSessionLobby(status = "ACTIVE")
        watcherOn(bus, state, getLobby)

        // Canal personnel : c'est celui qui porte jusqu'à un utilisateur parti ailleurs.
        bus.emit(Invalidation(resource = "session-status", scopeId = "player-2"))

        assertEquals(listOf("s-1"), getLobby.calls)
        assertEquals(SessionShortcutTarget.GAME, state.shortcut.value?.target)
        assertEquals("La Tour de Guet", state.shortcut.value?.title)
    }

    @Test
    fun `sans session en cours, l'evenement est ignore`() = runTest {
        val bus = InMemoryInvalidationBus()
        val state = SessionLobbyState()
        val getLobby = FakeGetSessionLobby(status = "ACTIVE")
        watcherOn(bus, state, getLobby)

        // Cas courant : l'utilisateur regarde une page du groupe et reçoit l'event du canal
        // `group:{id}` sans être engagé dans la session. Rien à recharger.
        bus.emit(Invalidation(resource = "session-status", scopeId = "group-1"))

        assertTrue(getLobby.calls.isEmpty())
        assertNull(state.shortcut.value)
    }

    @Test
    fun `une autre ressource ne declenche rien`() = runTest {
        val bus = InMemoryInvalidationBus()
        val state = stateInLobby()
        val getLobby = FakeGetSessionLobby(status = "ACTIVE")
        watcherOn(bus, state, getLobby)

        bus.emit(Invalidation(resource = "session-participants", scopeId = "group-1"))

        assertTrue(getLobby.calls.isEmpty())
        assertEquals(SessionShortcutTarget.LOBBY, state.shortcut.value?.target)
    }

    @Test
    fun `un echec reseau conserve le raccourci existant`() = runTest {
        val bus = InMemoryInvalidationBus()
        val state = stateInLobby()
        watcherOn(bus, state, FailingGetSessionLobby())

        bus.emit(Invalidation(resource = "session-status", scopeId = "player-2"))

        // Best-effort : le raccourci reste sur le salon, et le rattrapage au clic prend le relais
        // (le ViewModel du salon recharge, voit `ACTIVE` et bascule vers l'écran de jeu).
        assertEquals(SessionShortcutTarget.LOBBY, state.shortcut.value?.target)
    }
}

/** Salon renvoyé par le serveur pour la session « s-1 », au statut voulu. */
private fun lobby(status: String) = SessionLobby(
    sessionId = "s-1",
    status = status,
    participants = listOf(
        LobbyParticipant(userId = "player-2", status = "ACCEPTED", characterSheetId = "cs-1"),
    ),
)

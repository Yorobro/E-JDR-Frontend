package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.shared.feedback.UiMessage
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.feedback.UiMessagePlacement
import eu.ejdr.application.shared.feedback.UiMessageTone
import eu.ejdr.domain.features.session.entities.LobbyParticipant
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du [SessionRemovalWatcher] : la veille globale qui avertit le joueur retiré d'une session
 * et signale qu'il doit quitter le salon d'attente s'il s'y trouve.
 *
 * Le signal arrive sur le canal personnel du joueur : le recevoir suffit à savoir qu'il s'agit de
 * soi — d'où l'absence de toute comparaison d'identité ici.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRemovalWatcherTest {

    private class RecordingUiMessageBus : UiMessageBus {
        val emitted = mutableListOf<UiMessage>()
        private val flow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)
        override val messages: Flow<UiMessage> = flow.asSharedFlow()
        override fun emit(message: UiMessage) {
            emitted.add(message)
            flow.tryEmit(message)
        }
    }

    /**
     * Construit le watcher sur un dispatcher **non confiné** : son abonnement au bus est alors
     * actif dès la construction. Le bus d'invalidation est un `SharedFlow` sans replay — un event
     * émis avant l'abonnement serait perdu, et le test passerait à côté de ce qu'il vérifie.
     * La portée reste celle du test (annulée à sa fin), donc aucune coroutine ne survit.
     */
    private fun TestScope.watcherOn(
        bus: InMemoryInvalidationBus,
        messages: UiMessageBus,
        lobbyState: SessionLobbyState = SessionLobbyState(),
    ) = SessionRemovalWatcher(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        bus,
        messages,
        lobbyState,
    )

    /** Salon ouvert au nom du joueur, tel qu'il l'est quand le MJ le retire. */
    private fun openedLobby() = SessionLobbyState().apply {
        open(
            lobby = SessionLobby(
                sessionId = "s-1",
                status = "LOBBY",
                participants = listOf(
                    LobbyParticipant(userId = "player-2", status = "ACCEPTED", characterSheetId = "cs-1"),
                ),
            ),
            title = "La Tour de Guet",
            members = emptyList(),
            canManage = false,
        )
    }

    @Test
    fun `avertit le joueur en haut de l'ecran et signale la sortie du salon`() = runTest {
        val bus = InMemoryInvalidationBus()
        val messages = RecordingUiMessageBus()
        val watcher = watcherOn(bus, messages)

        bus.emit(Invalidation(resource = "session-removed", scopeId = "player-2"))

        assertTrue(watcher.ejected.value, "le joueur retiré doit être sorti du salon")
        assertEquals(1, messages.emitted.size)
        val message = messages.emitted.single()
        assertEquals("Vous avez été retiré de la session", message.text)
        // Message subi (il n'a rien demandé) : il s'affiche en haut, pas près de son dernier geste.
        assertEquals(UiMessagePlacement.TOP, message.placement)
        assertEquals(UiMessageTone.ERROR, message.tone)
    }

    @Test
    fun `le retrait efface le raccourci de retour au salon`() = runTest {
        val bus = InMemoryInvalidationBus()
        val lobbyState = openedLobby()
        val watcher = watcherOn(bus, RecordingUiMessageBus(), lobbyState)

        bus.emit(Invalidation(resource = "session-removed", scopeId = "player-2"))

        assertTrue(watcher.ejected.value)
        // La bulle globale lit ce raccourci : elle doit disparaître dans la même recomposition que
        // l'affichage du message, sinon le joueur garde un bouton vers un salon qu'il a quitté.
        assertNull(lobbyState.shortcut.value)
        assertNull(lobbyState.lobby.value)
    }

    @Test
    fun `ignore les invalidations d'une autre ressource`() = runTest {
        val bus = InMemoryInvalidationBus()
        val messages = RecordingUiMessageBus()
        val lobbyState = openedLobby()
        val watcher = watcherOn(bus, messages, lobbyState)

        bus.emit(Invalidation(resource = "session-participants", scopeId = "group-1"))

        assertFalse(watcher.ejected.value)
        assertTrue(messages.emitted.isEmpty())
        // Une réponse de joueur ne doit surtout pas faire disparaître la bulle de retour.
        assertNotNull(lobbyState.shortcut.value)
    }

    @Test
    fun `consume acquitte le retrait pour ne pas re-naviguer`() = runTest {
        val bus = InMemoryInvalidationBus()
        val watcher = watcherOn(bus, RecordingUiMessageBus())

        bus.emit(Invalidation(resource = "session-removed", scopeId = "player-2"))
        assertTrue(watcher.ejected.value)

        watcher.consume()

        assertFalse(watcher.ejected.value)
    }
}

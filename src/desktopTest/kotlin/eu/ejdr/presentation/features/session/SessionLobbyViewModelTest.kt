package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.InviteToLobbyUseCase
import eu.ejdr.application.features.session.abstraction.usecase.StartSessionUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.feedback.UiMessage
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.domain.features.session.entities.SessionLobby
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Tests du [SessionLobbyViewModel] centrés sur l'**abonnement temps réel**, dont dépend la
 * bascule automatique vers l'écran de jeu quand le MJ démarre la partie.
 *
 * Cas critique : un joueur qui rejoint le salon **depuis une invitation** voit son groupe actif
 * sélectionné juste avant la navigation, et `ActiveGroupState.select()` est **asynchrone**. Le
 * ViewModel peut donc naître avant que `activeGroupId` ne porte une valeur.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionLobbyViewModelTest {

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

    private class RecordingUiMessageBus : UiMessageBus {
        private val flow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)
        override val messages: Flow<UiMessage> = flow.asSharedFlow()
        override fun emit(message: UiMessage) { flow.tryEmit(message) }
    }

    private fun lobby(status: String) =
        SessionLobby(sessionId = "sess-1", status = status, participants = emptyList())

    private fun buildVm(
        activeGroupId: MutableStateFlow<String?>,
        bus: InMemoryInvalidationBus = InMemoryInvalidationBus(),
        subscriptions: RealtimeSubscriptions = RecordingSubscriptions(),
        lobbyStatus: () -> String = { "LOBBY" },
    ) = SessionLobbyViewModel(
        sessionId = "sess-1",
        activeGroupId = activeGroupId,
        getSessionLobby = GetSessionLobbyUseCase { Result.Success(lobby(lobbyStatus())) },
        inviteToLobby = InviteToLobbyUseCase { _, _ -> Result.Success(lobby("LOBBY")) },
        startSession = StartSessionUseCase { Result.Success(Unit) },
        lobbyState = SessionLobbyState(),
        invalidationBus = bus,
        subscriptions = subscriptions,
        uiMessageBus = RecordingUiMessageBus(),
    )

    @Test
    fun `s'abonne au canal du groupe quand il est déjà actif`() = runTest {
        val subs = RecordingSubscriptions()
        buildVm(activeGroupId = MutableStateFlow("g-1"), subscriptions = subs)
        advanceUntilIdle()

        assertContains(subs.subscribed, "group:g-1")
    }

    @Test
    fun `s'abonne aussi quand le groupe actif arrive APRES la création (joueur venant d'une invitation)`() =
        runTest {
            val activeGroupId = MutableStateFlow<String?>(null)
            val subs = RecordingSubscriptions()
            buildVm(activeGroupId = activeGroupId, subscriptions = subs)
            advanceUntilIdle()

            // `ActiveGroupState.select()` persiste puis publie : la valeur arrive après coup.
            activeGroupId.value = "g-1"
            advanceUntilIdle()

            assertContains(subs.subscribed, "group:g-1")
        }

    @Test
    fun `bascule vers le jeu quand la session passe ACTIVE, groupe actif arrivé après coup`() =
        runTest {
            val activeGroupId = MutableStateFlow<String?>(null)
            val bus = InMemoryInvalidationBus()
            val subs = RecordingSubscriptions()
            var status = "LOBBY"
            val vm = buildVm(
                activeGroupId = activeGroupId,
                bus = bus,
                subscriptions = subs,
                lobbyStatus = { status },
            )
            advanceUntilIdle()

            // `ActiveGroupState.select()` persiste puis publie : la valeur arrive après coup.
            activeGroupId.value = "g-1"
            advanceUntilIdle()

            // Le MJ démarre. Comme le vrai serveur, on ne notifie QUE les canaux abonnés : sans
            // abonnement au canal du groupe, le joueur ne reçoit rien et reste bloqué au salon.
            status = "ACTIVE"
            if ("group:g-1" in subs.subscribed) {
                bus.emit(Invalidation(resource = "session-status", scopeId = "g-1"))
            }
            advanceUntilIdle()

            assertTrue(vm.sessionStarted.value, "le joueur doit basculer vers l'écran de jeu")
        }
}

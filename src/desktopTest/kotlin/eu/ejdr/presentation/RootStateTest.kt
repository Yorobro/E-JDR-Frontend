package eu.ejdr.presentation

import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.realtime.RealtimeCoordinator
import eu.ejdr.application.features.realtime.abstraction.ConnectionState
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RootStateTest {

    /**
     * Connexion temps réel inerte : le test de RootState ne teste pas le réseau.
     * `incoming` est un flux **complétant** (vide) pour que la collecte du coordinator
     * se termine immédiatement et ne laisse pas de coroutine en suspens dans `runTest`.
     */
    private class NoopConnection : RealtimeConnection {
        override val state: StateFlow<ConnectionState> =
            MutableStateFlow(ConnectionState.Disconnected)
        override val incoming = emptyFlow<RealtimeMessage>()
        override suspend fun connect() = Unit
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun disconnect() = Unit
    }

    /**
     * Crée le [RootState] sur un scope **enfant** du scope de test : il partage l'ordonnanceur
     * virtuel (donc `advanceUntilIdle()` le pilote), mais possède son propre [Job] que le test
     * annule en fin de course via [RootStateFixture.dispose]. Sans cela, le collecteur de session
     * de `init` (sur un `MutableStateFlow`, donc infini) laisserait une coroutine en suspens et
     * ferait échouer `runTest` avec `UncompletedCoroutinesError`.
     */
    private class RootStateFixture(val state: RootState, private val job: Job) {
        fun dispose() = job.cancel()
    }

    private fun TestScope.rootState(
        theme: ThemeVariant = ThemeVariant.LIGHT,
        restore: Result<User, AuthError> = Result.Failure(AuthError.SessionExpired),
    ): RootStateFixture {
        val job = Job()
        val scope = CoroutineScope(coroutineContext + job)
        val state = RootState(
            scope = scope,
            getTheme = GetThemeUseCase { theme },
            restoreSession = RestoreSessionUseCase { restore },
            realtimeCoordinator = RealtimeCoordinator(NoopConnection(), InMemoryInvalidationBus(), scope),
        )
        return RootStateFixture(state, job)
    }

    @Test
    fun `loads persisted theme on init`() = runTest {
        val fixture = rootState(theme = ThemeVariant.DARK)
        advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, fixture.state.theme.value)
        fixture.dispose()
    }

    @Test
    fun `setTheme updates the exposed theme`() = runTest {
        val fixture = rootState()
        advanceUntilIdle()
        fixture.state.setTheme(ThemeVariant.DARK)
        assertEquals(ThemeVariant.DARK, fixture.state.theme.value)
        fixture.dispose()
    }

    @Test
    fun `restoreSession exposes Authenticated on success`() = runTest {
        val fixture = rootState(restore = Result.Success(User(id = "u1", email = "a@b.c", pseudo = "user1")))
        fixture.state.restoreSession()
        advanceUntilIdle()
        assertEquals(SessionStatus.Authenticated, fixture.state.sessionStatus.value)
        fixture.dispose()
    }

    @Test
    fun `restoreSession exposes Unauthenticated on failure`() = runTest {
        val fixture = rootState(restore = Result.Failure(AuthError.SessionExpired))
        fixture.state.restoreSession()
        advanceUntilIdle()
        assertEquals(SessionStatus.Unauthenticated, fixture.state.sessionStatus.value)
        fixture.dispose()
    }
}

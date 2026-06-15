package eu.ejdr.presentation

import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RootStateTest {

    private fun rootState(
        scope: kotlinx.coroutines.CoroutineScope,
        theme: ThemeVariant = ThemeVariant.LIGHT,
        restore: Result<User, AuthError> = Result.Failure(AuthError.SessionExpired),
    ) = RootState(
        scope = scope,
        getTheme = GetThemeUseCase { theme },
        restoreSession = RestoreSessionUseCase { restore },
    )

    @Test
    fun `loads persisted theme on init`() = runTest {
        val state = rootState(this, theme = ThemeVariant.DARK)
        testScheduler.advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, state.theme.value)
    }

    @Test
    fun `setTheme updates the exposed theme`() = runTest {
        val state = rootState(this)
        testScheduler.advanceUntilIdle()
        state.setTheme(ThemeVariant.DARK)
        assertEquals(ThemeVariant.DARK, state.theme.value)
    }

    @Test
    fun `restoreSession exposes Authenticated on success`() = runTest {
        val state = rootState(this, restore = Result.Success(User(id = "u1", email = "a@b.c")))
        state.restoreSession()
        testScheduler.advanceUntilIdle()
        assertEquals(SessionStatus.Authenticated, state.sessionStatus.value)
    }

    @Test
    fun `restoreSession exposes Unauthenticated on failure`() = runTest {
        val state = rootState(this, restore = Result.Failure(AuthError.SessionExpired))
        state.restoreSession()
        testScheduler.advanceUntilIdle()
        assertEquals(SessionStatus.Unauthenticated, state.sessionStatus.value)
    }
}

package eu.ejdr.presentation.features.auth

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests unitaires du [AuthViewModel] — possibles précisément parce que la logique a
 * quitté le composable pour un ViewModel testable sans Compose.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val user = User(id = "1", email = "a@b.com")

    @Test
    fun `empty fields produce a validation error without calling the use case`() = runTest {
        var called = false
        val vm = AuthViewModel { _ -> called = true; Result.Success(user) }

        vm.onSubmit()
        advanceUntilIdle()

        assertFalse(called)
        assertEquals("Veuillez remplir tous les champs.", vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `successful submit emits the authenticated event`() = runTest {
        val vm = AuthViewModel { _ -> Result.Success(user) }
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("secret")

        vm.onSubmit()
        advanceUntilIdle()

        val authenticated = withTimeout(1_000) { vm.authenticated.first() }
        assertEquals(user, authenticated)
    }

    @Test
    fun `failed submit exposes the domain error message and stops loading`() = runTest {
        val vm = AuthViewModel { _ -> Result.Failure(AuthError.InvalidCredentials) }
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("wrong")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthError.InvalidCredentials.message, vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `editing a field clears the previous error`() = runTest {
        val vm = AuthViewModel { _ -> Result.Failure(AuthError.InvalidCredentials) }
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("wrong")
        vm.onSubmit()
        advanceUntilIdle()
        assertTrue(vm.state.value.error != null)

        vm.onEmailChange("c@d.com")

        assertNull(vm.state.value.error)
    }
}

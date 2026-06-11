package eu.ejdr.presentation.features.user

import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val user = User(id = "1", email = "a@b.com")

    private fun useCase(result: Result<User, AuthError>) =
        GetCurrentUserUseCase { result }

    @Test
    fun `loads the profile on init`() = runTest {
        val vm = UserViewModel(useCase(Result.Success(user)))
        advanceUntilIdle()
        assertEquals(user, vm.profile.value)
    }

    @Test
    fun `emits sessionExpired when the session is no longer valid`() = runTest {
        val vm = UserViewModel(useCase(Result.Failure(AuthError.SessionExpired)))
        advanceUntilIdle()
        val event = withTimeout(1_000) { vm.sessionExpired.first() }
        assertEquals(Unit, event)
        assertNull(vm.profile.value)
    }

    @Test
    fun `keeps a null profile on a network error without expiring the session`() = runTest {
        val vm = UserViewModel(useCase(Result.Failure(AuthError.Network)))
        advanceUntilIdle()
        assertNull(vm.profile.value)
    }
}

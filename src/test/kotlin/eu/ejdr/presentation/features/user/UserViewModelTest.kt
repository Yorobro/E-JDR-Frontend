package eu.ejdr.presentation.features.user

import eu.ejdr.application.features.auth.abstraction.usecase.ChangeEmailUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.ChangePasswordUseCase
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
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val user = User(id = "1", email = "a@b.com")

    private fun vm(
        getCurrentUser: GetCurrentUserUseCase = GetCurrentUserUseCase { Result.Success(user) },
        changeEmail: ChangeEmailUseCase = ChangeEmailUseCase { Result.Success(Unit) },
        changePassword: ChangePasswordUseCase = ChangePasswordUseCase { _, _ -> Result.Success(Unit) },
    ) = UserViewModel(getCurrentUser, changeEmail, changePassword)

    @Test
    fun `loads the profile on init`() = runTest {
        val viewModel = vm(getCurrentUser = GetCurrentUserUseCase { Result.Success(user) })
        advanceUntilIdle()
        assertEquals(user, viewModel.profile.value)
    }

    @Test
    fun `emits sessionExpired when the session is no longer valid`() = runTest {
        val viewModel = vm(getCurrentUser = GetCurrentUserUseCase { Result.Failure(AuthError.SessionExpired) })
        advanceUntilIdle()
        val event = withTimeout(1_000) { viewModel.sessionExpired.first() }
        assertEquals(Unit, event)
        assertNull(viewModel.profile.value)
    }

    @Test
    fun `keeps a null profile on a network error without expiring the session`() = runTest {
        val viewModel = vm(getCurrentUser = GetCurrentUserUseCase { Result.Failure(AuthError.Network) })
        advanceUntilIdle()
        assertNull(viewModel.profile.value)
    }

    @Test
    fun `changeEmail success sets editState to Success`() = runTest {
        val viewModel = vm(changeEmail = ChangeEmailUseCase { Result.Success(Unit) })
        advanceUntilIdle()
        viewModel.changeEmail("new@b.com")
        advanceUntilIdle()
        assertIs<EditState.Success>(viewModel.editState.value)
    }

    @Test
    fun `changeEmail EmailAlreadyUsed sets editState to Error`() = runTest {
        val viewModel = vm(changeEmail = ChangeEmailUseCase { Result.Failure(AuthError.EmailAlreadyUsed) })
        advanceUntilIdle()
        viewModel.changeEmail("taken@b.com")
        advanceUntilIdle()
        val state = viewModel.editState.value
        assertIs<EditState.Error>(state)
        assertEquals(AuthError.EmailAlreadyUsed.message, state.message)
    }

    @Test
    fun `changePassword InvalidCredentials sets editState to Error`() = runTest {
        val viewModel = vm(changePassword = ChangePasswordUseCase { _, _ -> Result.Failure(AuthError.InvalidCredentials) })
        advanceUntilIdle()
        viewModel.changePassword("old", "newStrong1")
        advanceUntilIdle()
        assertIs<EditState.Error>(viewModel.editState.value)
    }

    @Test
    fun `resetEditState resets editState to Idle`() = runTest {
        val viewModel = vm(changeEmail = ChangeEmailUseCase { Result.Success(Unit) })
        advanceUntilIdle()
        viewModel.changeEmail("new@b.com")
        advanceUntilIdle()
        viewModel.resetEditState()
        assertIs<EditState.Idle>(viewModel.editState.value)
    }

    @Test
    fun `editState starts as Idle`() = runTest {
        val viewModel = vm()
        assertIs<EditState.Idle>(viewModel.editState.value)
    }
}

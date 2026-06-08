package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegisterUseCaseImplTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = RegisterUseCaseImpl(repository)

    private val creds = Credentials("new@user.com", "pw")

    @Test
    fun `returns user on successful registration`() = runTest {
        coEvery { repository.register(creds) } returns Result.Success(User("42", "new@user.com"))

        val result = useCase(creds)

        assertIs<Result.Success<User>>(result)
        assertEquals("new@user.com", result.value.email)
    }

    @Test
    fun `propagates EmailAlreadyUsed failure`() = runTest {
        coEvery { repository.register(creds) } returns Result.Failure(AuthError.EmailAlreadyUsed)

        val result = useCase(creds)

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.EmailAlreadyUsed, result.error)
    }
}

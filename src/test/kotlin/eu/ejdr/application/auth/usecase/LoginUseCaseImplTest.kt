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

class LoginUseCaseImplTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = LoginUseCaseImpl(repository)

    private val creds = Credentials("a@b.c", "pw")

    @Test
    fun `returns user on success`() = runTest {
        coEvery { repository.login(creds) } returns Result.Success(User("1", "a@b.c"))

        val result = useCase(creds)

        assertIs<Result.Success<User>>(result)
        assertEquals("a@b.c", result.value.email)
    }

    @Test
    fun `propagates InvalidCredentials failure`() = runTest {
        coEvery { repository.login(creds) } returns Result.Failure(AuthError.InvalidCredentials)

        val result = useCase(creds)

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.InvalidCredentials, result.error)
    }
}

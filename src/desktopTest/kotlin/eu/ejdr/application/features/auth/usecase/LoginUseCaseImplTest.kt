package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
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
        coEvery { repository.login(creds) } returns Result.Success(User("1", "a@b.c", "user1"))

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

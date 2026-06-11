package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetCurrentUserUseCaseImplTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = GetCurrentUserUseCaseImpl(repository)

    @Test
    fun `returns user on success`() = runTest {
        coEvery { repository.me() } returns Result.Success(User("1", "a@b.c"))

        val result = useCase()

        assertIs<Result.Success<User>>(result)
        assertEquals("a@b.c", result.value.email)
    }

    @Test
    fun `propagates SessionExpired failure`() = runTest {
        coEvery { repository.me() } returns Result.Failure(AuthError.SessionExpired)

        val result = useCase()

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.SessionExpired, result.error)
    }
}

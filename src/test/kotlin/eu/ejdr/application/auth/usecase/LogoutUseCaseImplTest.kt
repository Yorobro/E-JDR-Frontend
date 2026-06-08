package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LogoutUseCaseImplTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = LogoutUseCaseImpl(repository)

    @Test
    fun `returns success on logout`() = runTest {
        coEvery { repository.logout() } returns Result.Success(Unit)

        val result = useCase()

        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `propagates failure`() = runTest {
        coEvery { repository.logout() } returns Result.Failure(AuthError.Network)

        val result = useCase()

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.Network, result.error)
    }
}

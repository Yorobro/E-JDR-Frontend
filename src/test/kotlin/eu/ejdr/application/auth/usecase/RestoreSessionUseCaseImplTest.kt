package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.service.SessionService
import eu.ejdr.application.common.Result
import eu.ejdr.domain.error.entities.auth.AuthError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RestoreSessionUseCaseImplTest {

    private val service = mockk<SessionService>()
    private val useCase = RestoreSessionUseCaseImpl(service)

    @Test
    fun `delegates to session service`() = runTest {
        coEvery { service.restore() } returns Result.Success(Unit)

        assertIs<Result.Success<Unit>>(useCase())
    }

    @Test
    fun `propagates failure`() = runTest {
        coEvery { service.restore() } returns Result.Failure(AuthError.SessionExpired)

        val result = useCase()

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.SessionExpired, result.error)
    }
}

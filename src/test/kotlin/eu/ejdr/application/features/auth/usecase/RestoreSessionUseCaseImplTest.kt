package eu.ejdr.application.features.auth.usecase

import eu.ejdr.application.features.auth.abstraction.service.SessionService
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
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
    fun `delegates to session service and returns user on success`() = runTest {
        coEvery { service.restore() } returns Result.Success(User("u-1", "a@b.c"))

        val result = useCase()

        assertIs<Result.Success<User>>(result)
        assertEquals("a@b.c", result.value.email)
    }

    @Test
    fun `propagates failure`() = runTest {
        coEvery { service.restore() } returns Result.Failure(AuthError.SessionExpired)

        val result = useCase()

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.SessionExpired, result.error)
    }
}

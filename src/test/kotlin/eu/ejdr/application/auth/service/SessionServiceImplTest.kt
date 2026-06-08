package eu.ejdr.application.auth.service

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionServiceImplTest {

    private val repository = mockk<AuthRepository>()
    private val service = SessionServiceImpl(repository)

    @Test
    fun `restore returns NoPersistedSession when nothing persisted`() = runTest {
        every { repository.hasPersistedSession() } returns false

        val result = service.restore()

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.NoPersistedSession, result.error)
    }

    @Test
    fun `restore delegates to repository refresh when session persisted`() = runTest {
        every { repository.hasPersistedSession() } returns true
        coEvery { repository.refresh() } returns Result.Success(User("u-1", "a@b.c"))

        val result = service.restore()

        assertIs<Result.Success<User>>(result)
        assertEquals("a@b.c", result.value.email)
    }
}

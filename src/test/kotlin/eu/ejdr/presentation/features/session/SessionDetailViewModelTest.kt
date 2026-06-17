package eu.ejdr.presentation.features.session

import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.domain.features.session.error.SessionError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun session(id: String = "s-1", title: String = "Intro", date: String = "2026-06-20") =
        Session(id = id, campaignId = "camp-1", title = title, date = date, createdAt = "2026-06-13T10:00:00.000Z")

    @Test
    fun `loads the session at init`() = runTest {
        val vm = SessionDetailViewModel(
            sessionId = "s-1",
            getById = GetSessionUseCase { Result.Success(session()) },
            update = UpdateSessionUseCase { _, _, _ -> Result.Success(session()) },
            deleteSession = DeleteSessionUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        assertEquals("Intro", vm.session.value?.title)
        assertNull(vm.error.value)
    }

    @Test
    fun `save success updates the session`() = runTest {
        val vm = SessionDetailViewModel(
            sessionId = "s-1",
            getById = GetSessionUseCase { Result.Success(session()) },
            update = UpdateSessionUseCase { _, title, date -> Result.Success(session(title = title, date = date)) },
            deleteSession = DeleteSessionUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.save("Après", "2026-07-01")
        advanceUntilIdle()

        assertEquals("Après", vm.session.value?.title)
        assertEquals("2026-07-01", vm.session.value?.date)
        assertNull(vm.error.value)
    }

    @Test
    fun `save failure exposes the error message`() = runTest {
        val vm = SessionDetailViewModel(
            sessionId = "s-1",
            getById = GetSessionUseCase { Result.Success(session()) },
            update = UpdateSessionUseCase { _, _, _ -> Result.Failure(SessionError.InvalidTitle) },
            deleteSession = DeleteSessionUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.save("", "2026-07-01")
        advanceUntilIdle()

        assertEquals(SessionError.InvalidTitle.message, vm.error.value)
    }

    @Test
    fun `delete success flags deleted`() = runTest {
        val vm = SessionDetailViewModel(
            sessionId = "s-1",
            getById = GetSessionUseCase { Result.Success(session()) },
            update = UpdateSessionUseCase { _, _, _ -> Result.Success(session()) },
            deleteSession = DeleteSessionUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertTrue(vm.deleted.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `delete failure exposes the error and does not flag deleted`() = runTest {
        val vm = SessionDetailViewModel(
            sessionId = "s-1",
            getById = GetSessionUseCase { Result.Success(session()) },
            update = UpdateSessionUseCase { _, _, _ -> Result.Success(session()) },
            deleteSession = DeleteSessionUseCase { Result.Failure(SessionError.AccessDenied) },
        )
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertEquals(SessionError.AccessDenied.message, vm.error.value)
        assertEquals(false, vm.deleted.value)
    }
}

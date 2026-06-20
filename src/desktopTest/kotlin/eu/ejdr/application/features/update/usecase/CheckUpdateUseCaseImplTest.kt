package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.getOrNull
import eu.ejdr.domain.features.update.error.UpdateError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CheckUpdateUseCaseImplTest {

    private val repository = mockk<UpdateRepository>()

    private fun useCase(current: String) =
        CheckUpdateUseCaseImpl(repository, currentVersion = current, isDev = false)

    private fun release(version: String) = UpdateInfoDto(version, "https://example.com/release", null)

    @Test
    fun `returns null in dev mode without querying the repository`() = runTest {
        // En dev, on ne propose jamais de mise à jour, même si une release plus récente existe.
        coEvery { repository.fetchLatestRelease() } returns release("v99.0.0")
        val devUseCase = CheckUpdateUseCaseImpl(repository, currentVersion = "1.0.0", isDev = true)
        assertNull(devUseCase().getOrNull())
        io.mockk.coVerify(exactly = 0) { repository.fetchLatestRelease() }
    }

    @Test
    fun `returns null when no release available`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns null
        assertNull(useCase("1.0.0")().getOrNull())
    }

    @Test
    fun `returns null when same version`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("1.0.0")().getOrNull())
    }

    @Test
    fun `returns null when latest is older (patch)`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("1.0.1")().getOrNull())
    }

    @Test
    fun `returns null when latest is older (minor)`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("1.1.0")().getOrNull())
    }

    @Test
    fun `returns null when latest is older (major)`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("2.0.0")().getOrNull())
    }

    @Test
    fun `returns update info when newer patch`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.1")
        assertNotNull(useCase("1.0.0")().getOrNull())
    }

    @Test
    fun `returns update info when newer minor`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.1.0")
        assertNotNull(useCase("1.0.9")().getOrNull())
    }

    @Test
    fun `returns update info when newer major`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v2.0.0")
        assertNotNull(useCase("1.9.9")().getOrNull())
    }

    @Test
    fun `handles version without v prefix`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("2.0.0")
        assertNotNull(useCase("1.0.0")().getOrNull())
    }

    @Test
    fun `handles missing patch component`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.1")
        assertNotNull(useCase("1.0.0")().getOrNull())
    }

    @Test
    fun `returns CheckFailed when the repository throws`() = runTest {
        coEvery { repository.fetchLatestRelease() } throws RuntimeException("network down")
        val result = useCase("1.0.0")()
        assertIs<Result.Failure<UpdateError>>(result)
        assertEquals(UpdateError.CheckFailed, result.error)
    }
}

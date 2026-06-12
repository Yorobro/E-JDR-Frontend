package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CheckUpdateUseCaseImplTest {

    private val repository = mockk<UpdateRepository>()

    private fun useCase(current: String) = CheckUpdateUseCaseImpl(repository, currentVersion = current)

    private fun release(version: String) = UpdateInfoDto(version, "https://example.com/release", null)

    @Test
    fun `returns null when no release available`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns null
        assertNull(useCase("1.0.0")())
    }

    @Test
    fun `returns null when same version`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("1.0.0")())
    }

    @Test
    fun `returns null when latest is older (patch)`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("1.0.1")())
    }

    @Test
    fun `returns null when latest is older (minor)`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("1.1.0")())
    }

    @Test
    fun `returns null when latest is older (major)`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.0")
        assertNull(useCase("2.0.0")())
    }

    @Test
    fun `returns update info when newer patch`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.0.1")
        assertNotNull(useCase("1.0.0")())
    }

    @Test
    fun `returns update info when newer minor`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.1.0")
        assertNotNull(useCase("1.0.9")())
    }

    @Test
    fun `returns update info when newer major`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v2.0.0")
        assertNotNull(useCase("1.9.9")())
    }

    @Test
    fun `handles version without v prefix`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("2.0.0")
        assertNotNull(useCase("1.0.0")())
    }

    @Test
    fun `handles missing patch component`() = runTest {
        coEvery { repository.fetchLatestRelease() } returns release("v1.1")
        assertNotNull(useCase("1.0.0")())
    }
}

package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * L'effet de bord système (lancement de l'installeur + exit JVM) passe par le service
 * [SystemLauncherService] mockable.
 *
 * Le use case renvoie désormais un [Result] : un échec **récupérable** du téléchargement est
 * converti en [UpdateError.DownloadFailed] (aucune exception ne fuit). En revanche, le
 * lancement de l'installeur — typé `Nothing` — **quitte la JVM** en production. En test on
 * stubbe le launcher pour lever une sentinelle [ProcessExitedSignal] simulant ce « point de
 * non-retour », et l'on vérifie que l'installeur lui a bien été remis.
 */
class DownloadAndInstallUpdateUseCaseImplTest {

    private val repository = mockk<UpdateRepository>()
    private val launcher = mockk<SystemLauncherService>()
    private val useCase = DownloadAndInstallUpdateUseCaseImpl(repository, launcher)

    /** Sentinelle simulant le fait que `launchInstallerAndExit` ne rend jamais la main. */
    private class ProcessExitedSignal : RuntimeException()

    @Test
    fun `downloads then hands the installer to the system launcher`() = runTest {
        val installer = File("E-JDR-update.exe")
        coEvery { repository.downloadUpdate(any(), any(), any()) } returns installer
        every { launcher.launchInstallerAndExit(installer) } answers { throw ProcessExitedSignal() }

        assertFailsWith<ProcessExitedSignal> {
            useCase("https://example.com/E-JDR-update.exe", "https://example.com/E-JDR-update.exe.sha256") {}
        }

        verify { launcher.launchInstallerAndExit(installer) }
    }

    @Test
    fun `forwards the urls and progress callback to the repository`() = runTest {
        val installer = File("E-JDR-update.exe")
        val url = "https://example.com/E-JDR-update.exe"
        val sha = "https://example.com/E-JDR-update.exe.sha256"
        val seenProgress = mutableListOf<Float?>()
        coEvery { repository.downloadUpdate(url, sha, any()) } answers {
            val onProgress = thirdArg<(Float?) -> Unit>()
            onProgress(0.5f)
            installer
        }
        every { launcher.launchInstallerAndExit(any()) } answers { throw ProcessExitedSignal() }

        assertFailsWith<ProcessExitedSignal> {
            useCase(url, sha) { progress -> seenProgress.add(progress) }
        }

        assertEquals(listOf<Float?>(0.5f), seenProgress)
        verify { launcher.launchInstallerAndExit(installer) }
    }

    @Test
    fun `returns DownloadFailed when the download throws`() = runTest {
        coEvery { repository.downloadUpdate(any(), any(), any()) } throws RuntimeException("network down")

        val result = useCase("https://example.com/E-JDR-update.exe", "https://example.com/x.sha256") {}

        assertIs<Result.Failure<UpdateError>>(result)
        assertEquals(UpdateError.DownloadFailed, result.error)
    }

    @Test
    fun `returns IntegrityCheckFailed when the repository rejects the binary`() = runTest {
        // Le repository lève SecurityException si l'intégrité n'est pas garantie (hash absent,
        // hôte non autorisé, empreinte différente) : aucun installeur ne doit alors être lancé.
        coEvery { repository.downloadUpdate(any(), any(), any()) } throws
            SecurityException("hash mismatch")

        val result = useCase("https://example.com/E-JDR-update.exe", null) {}

        assertIs<Result.Failure<UpdateError>>(result)
        assertEquals(UpdateError.IntegrityCheckFailed, result.error)
        verify(exactly = 0) { launcher.launchInstallerAndExit(any()) }
    }
}

package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Démontre que le use case est désormais **testable** : l'effet de bord système
 * (lancement de l'installeur + exit JVM) passe par le service [SystemLauncherService]
 * mockable, au lieu d'un `ProcessBuilder` + `exitProcess` qui tuerait le JVM de test.
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
        coEvery { repository.downloadUpdate(any(), any()) } returns installer
        // Le launcher réel termine le process ; en test on lève une sentinelle.
        every { launcher.launchInstallerAndExit(installer) } throws ProcessExitedSignal()

        assertFailsWith<ProcessExitedSignal> {
            useCase("https://example.com/E-JDR-update.exe") {}
        }

        verifyOrder {
            launcher.launchInstallerAndExit(installer)
        }
    }

    @Test
    fun `forwards the download url and progress callback to the repository`() = runTest {
        val installer = File("E-JDR-update.exe")
        val url = "https://example.com/E-JDR-update.exe"
        val seenProgress = mutableListOf<Float?>()
        coEvery { repository.downloadUpdate(url, any()) } answers {
            val onProgress = secondArg<(Float?) -> Unit>()
            onProgress(0.5f)
            installer
        }
        every { launcher.launchInstallerAndExit(any()) } throws ProcessExitedSignal()

        assertFailsWith<ProcessExitedSignal> {
            useCase(url) { progress -> seenProgress.add(progress) }
        }

        assertEquals(listOf<Float?>(0.5f), seenProgress)
        verify { launcher.launchInstallerAndExit(installer) }
    }
}

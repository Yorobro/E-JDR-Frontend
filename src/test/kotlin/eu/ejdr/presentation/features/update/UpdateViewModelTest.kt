package eu.ejdr.presentation.features.update

import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateViewModelTest {

    @Test
    fun `starts idle`() = runTest {
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, _ -> Result.Success(Unit) },
            this,
        )
        assertIs<DownloadState.Idle>(vm.state.value)
    }

    @Test
    fun `successful download keeps downloading state (launcher exits app)`() = runTest {
        var progressSeen = false
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, onProgress -> onProgress(0.5f); progressSeen = true; Result.Success(Unit) },
            this,
        )
        vm.download("https://example.com/app.exe")
        testScheduler.advanceUntilIdle()
        assertEquals(true, progressSeen)
        assertIs<DownloadState.Downloading>(vm.state.value)
    }

    @Test
    fun `failed download moves to Error`() = runTest {
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, _ -> Result.Failure(UpdateError.DownloadFailed) },
            this,
        )
        vm.download("https://example.com/app.exe")
        testScheduler.advanceUntilIdle()
        assertIs<DownloadState.Error>(vm.state.value)
    }
}

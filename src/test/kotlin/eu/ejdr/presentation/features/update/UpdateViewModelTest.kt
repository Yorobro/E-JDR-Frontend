package eu.ejdr.presentation.features.update

import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts idle`() {
        val vm = UpdateViewModel(DownloadAndInstallUpdateUseCase { _, _ -> Result.Success(Unit) })
        assertIs<DownloadState.Idle>(vm.state.value)
    }

    @Test
    fun `successful download keeps downloading state (launcher exits app)`() = runTest {
        var progressSeen = false
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, onProgress -> onProgress(0.5f); progressSeen = true; Result.Success(Unit) },
        )
        vm.download("https://example.com/app.exe")
        advanceUntilIdle()
        assertEquals(true, progressSeen)
        assertIs<DownloadState.Downloading>(vm.state.value)
    }

    @Test
    fun `failed download moves to Error`() = runTest {
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, _ -> Result.Failure(UpdateError.DownloadFailed) },
        )
        vm.download("https://example.com/app.exe")
        advanceUntilIdle()
        assertIs<DownloadState.Error>(vm.state.value)
    }
}

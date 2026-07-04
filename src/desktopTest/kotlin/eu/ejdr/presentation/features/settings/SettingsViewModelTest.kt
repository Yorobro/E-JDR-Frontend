package eu.ejdr.presentation.features.settings

import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
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
import kotlin.test.assertNull

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        initial: ThemeVariant = ThemeVariant.PARCHEMIN,
        setResult: Result<Unit, SettingsError> = Result.Success(Unit),
        persisted: MutableList<ThemeVariant> = mutableListOf(),
    ) = SettingsViewModel(
        getTheme = GetThemeUseCase { initial },
        setTheme = SetThemeUseCase { theme -> persisted.add(theme); setResult },
    )

    @Test
    fun `exposes the initial theme from the get use case`() = runTest {
        val vm = viewModel(initial = ThemeVariant.GRIMOIRE)
        advanceUntilIdle()
        assertEquals(ThemeVariant.GRIMOIRE, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `successful selection persists, updates state and notifies`() = runTest {
        val persisted = mutableListOf<ThemeVariant>()
        val vm = viewModel(initial = ThemeVariant.PARCHEMIN, persisted = persisted)
        advanceUntilIdle()

        var applied: ThemeVariant? = null
        vm.onThemeSelected(ThemeVariant.GRIMOIRE) { applied = it }
        advanceUntilIdle()

        assertEquals(ThemeVariant.GRIMOIRE, applied)
        assertEquals(listOf(ThemeVariant.GRIMOIRE), persisted)
        assertEquals(ThemeVariant.GRIMOIRE, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `failed persistence keeps state unchanged and exposes an error`() = runTest {
        val vm = viewModel(
            initial = ThemeVariant.PARCHEMIN,
            setResult = Result.Failure(SettingsError.ThemePersistenceFailed),
        )
        advanceUntilIdle()

        var applied: ThemeVariant? = null
        vm.onThemeSelected(ThemeVariant.GRIMOIRE) { applied = it }
        advanceUntilIdle()

        assertNull(applied)
        assertEquals(ThemeVariant.PARCHEMIN, vm.currentTheme.value)
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)
    }

    @Test
    fun `a successful selection clears a previous error`() = runTest {
        var nextResult: Result<Unit, SettingsError> = Result.Failure(SettingsError.ThemePersistenceFailed)
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { ThemeVariant.PARCHEMIN },
            setTheme = SetThemeUseCase { nextResult },
        )
        advanceUntilIdle()

        vm.onThemeSelected(ThemeVariant.GRIMOIRE) {}
        advanceUntilIdle()
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)

        nextResult = Result.Success(Unit)
        vm.onThemeSelected(ThemeVariant.GRIMOIRE) {}
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertEquals(ThemeVariant.GRIMOIRE, vm.currentTheme.value)
    }
}

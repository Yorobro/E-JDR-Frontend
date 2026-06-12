package eu.ejdr.presentation.features.settings

import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.error.SettingsError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private fun viewModel(
        initial: ThemeVariant = ThemeVariant.LIGHT,
        setResult: Result<Unit, SettingsError> = Result.Success(Unit),
    ): Pair<SettingsViewModel, MutableList<ThemeVariant>> {
        val persisted = mutableListOf<ThemeVariant>()
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { initial },
            setTheme = SetThemeUseCase { theme -> persisted.add(theme); setResult },
        )
        return vm to persisted
    }

    @Test
    fun `exposes the initial theme from the get use case`() {
        val (vm, _) = viewModel(initial = ThemeVariant.DARK)
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `successful selection persists, updates state and reports success`() {
        val (vm, persisted) = viewModel(initial = ThemeVariant.LIGHT)

        val applied = vm.onThemeSelected(ThemeVariant.DARK)

        assertTrue(applied)
        assertEquals(listOf(ThemeVariant.DARK), persisted)
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `failed persistence keeps state unchanged and exposes an error`() {
        val (vm, _) = viewModel(
            initial = ThemeVariant.LIGHT,
            setResult = Result.Failure(SettingsError.ThemePersistenceFailed),
        )

        val applied = vm.onThemeSelected(ThemeVariant.DARK)

        assertFalse(applied)
        assertEquals(ThemeVariant.LIGHT, vm.currentTheme.value)
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)
    }

    @Test
    fun `a successful selection clears a previous error`() {
        val persisted = mutableListOf<ThemeVariant>()
        var nextResult: Result<Unit, SettingsError> = Result.Failure(SettingsError.ThemePersistenceFailed)
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { ThemeVariant.LIGHT },
            setTheme = SetThemeUseCase { theme -> persisted.add(theme); nextResult },
        )

        vm.onThemeSelected(ThemeVariant.DARK)
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)

        nextResult = Result.Success(Unit)
        vm.onThemeSelected(ThemeVariant.DARK)

        assertNull(vm.error.value)
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
    }
}

package eu.ejdr.presentation.shared.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.currentKoinScope
import org.koin.core.scope.Scope

/**
 * Crée un [ViewModel] **retenu par la destination de navigation** dont les dépendances
 * sont résolues par Koin.
 *
 * Factorise le duo répété `koinInject<X>()` + `viewModel { VM(x) }` des pages : le bloc
 * [factory] reçoit le [Scope] Koin courant et y résout ce dont le ViewModel a besoin via
 * `get()`. La rétention (un seul ViewModel par destination, survivant à la recomposition)
 * reste assurée par `viewModel { }`.
 *
 * ```
 * val vm = koinViewModel { SettingsViewModel(get(), get()) }
 * ```
 *
 * @param factory Construit le ViewModel à partir du scope Koin (récepteur), appelé une
 * seule fois par destination retenue.
 */
@Composable
inline fun <reified VM : ViewModel> koinViewModel(
    crossinline factory: Scope.() -> VM,
): VM {
    val scope = currentKoinScope()
    return viewModel { scope.factory() }
}

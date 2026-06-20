package eu.ejdr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * Décorateur de rétention des ViewModels par destination (Android).
 *
 * Contrairement au desktop (décorateur maison), Android dispose du décorateur officiel
 * `rememberViewModelStoreNavEntryDecorator()` de `lifecycle-viewmodel-navigation3`, adossé au
 * `ViewModelStoreOwner` de l'`Activity`.
 */
@Composable
fun rememberEjdrViewModelStoreNavEntryDecorator(): NavEntryDecorator<Any> =
    rememberViewModelStoreNavEntryDecorator()

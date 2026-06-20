package eu.ejdr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * Décorateur Navigation 3 **maison** assurant la rétention d'un ViewModel **par
 * destination** du back-stack.
 *
 * ## Pourquoi maison
 *
 * Le décorateur officiel `rememberViewModelStoreNavEntryDecorator` exige à l'exécution
 * un `SavedStateNavEntryDecorator` qui **n'est pas publié pour la cible desktop** aux
 * versions stables actuelles (navigation3 1.1.1 / lifecycle-viewmodel-navigation3
 * 2.10.0). Plutôt que d'attendre que JetBrains comble ce trou de portage, on fournit
 * ici l'essentiel : un [ViewModelStore] propre à chaque entrée, exposé via
 * [LocalViewModelStoreOwner] pour que `viewModel { }` y trouve sa portée.
 *
 * Nos ViewModels prennent leurs dépendances **par constructeur** (pas de
 * `SavedStateHandle`), donc la partie SavedState du décorateur officiel ne nous
 * manque pas. À surveiller : si une feature future a besoin d'un `SavedStateHandle`,
 * il faudra réévaluer (et revérifier si le décorateur officiel desktop est enfin
 * disponible).
 *
 * ## Comportement
 *
 * - Un [ViewModelStore] est créé/réutilisé par `contentKey` d'entrée.
 * - Il est fourni dans la composition de l'entrée via [LocalViewModelStoreOwner] :
 *   `viewModel { }` scope donc son ViewModel à la destination.
 * - À la **dépile** de l'entrée (`onPop`), le store est `clear()` (les `onCleared`
 *   des ViewModels s'exécutent, `viewModelScope` est annulé) puis retiré du cache —
 *   pas de fuite mémoire.
 */
@Composable
fun rememberEjdrViewModelStoreNavEntryDecorator(): NavEntryDecorator<Any> {
    val stores = remember { mutableMapOf<Any, ViewModelStore>() }

    DisposableEffect(Unit) {
        onDispose {
            // L'app se ferme : on vide tout pour exécuter les onCleared restants.
            stores.values.forEach { it.clear() }
            stores.clear()
        }
    }

    return remember {
        NavEntryDecorator<Any>(
            onPop = { contentKey -> stores.remove(contentKey)?.clear() },
        ) { entry -> DecorateEntry(entry, stores) }
    }
}

@Composable
private fun DecorateEntry(entry: NavEntry<Any>, stores: MutableMap<Any, ViewModelStore>) {
    val store = stores.getOrPut(entry.contentKey) { ViewModelStore() }
    val owner = remember(store) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        entry.Content()
    }
}

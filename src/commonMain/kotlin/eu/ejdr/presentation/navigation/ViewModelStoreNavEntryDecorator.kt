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
 * destination** du back-stack. Commun aux deux cibles (desktop et Android).
 *
 * ## Pourquoi maison
 *
 * Le décorateur officiel `rememberViewModelStoreNavEntryDecorator` exige à l'exécution
 * un `SavedStateNavEntryDecorator` qui n'est **pas** dans le `commonMain` du fork
 * JetBrains de navigation3 (1.1.1 / lifecycle-viewmodel-navigation3 2.10.0). Il n'est
 * donc utilisable sur **aucune** de nos deux cibles. Plutôt que d'attendre que ce trou
 * soit comblé, on fournit ici l'essentiel : un [ViewModelStore] propre à chaque entrée,
 * exposé via [LocalViewModelStoreOwner] pour que `viewModel { }` y trouve sa portée.
 *
 * Nos ViewModels prennent leurs dépendances **par constructeur** (pas de
 * `SavedStateHandle`), donc la partie SavedState du décorateur officiel ne nous
 * manque pas. À surveiller : si une feature future a besoin d'un `SavedStateHandle`,
 * il faudra réévaluer (et revérifier si le décorateur officiel est enfin disponible).
 *
 * ## Ce qu'il empêche
 *
 * Sans lui, `viewModel { }` (appelé sans `key`) résout contre le `ViewModelStore` de
 * l'hôte — l'Activity sur Android — avec une clé dérivée de la **classe** du ViewModel,
 * jamais de ses arguments. Un ViewModel paramétré par un id devient alors un singleton
 * de fait : le premier ouvert gagne pour toute la vie de l'app. Ouvrir « Armes » puis
 * « Armures » affichait le titre « Armures » et le contenu « Armes ». Idem pour les
 * détails de campagne, fiche, groupe et session.
 *
 * ## Comportement
 *
 * - Un [ViewModelStore] est créé/réutilisé par `contentKey` d'entrée. Les routes étant
 *   des `data class` (`Route.ReferenceList(type)`, `Route.CampaignDetail(id)`…), deux
 *   destinations d'arguments différents ont des `contentKey` **distincts** — donc deux
 *   ViewModels distincts, chacun avec ses bons arguments.
 * - Le store est fourni dans la composition de l'entrée via [LocalViewModelStoreOwner].
 * - À la **dépile** de l'entrée (`onPop`), le store est `clear()` (les `onCleared` des
 *   ViewModels s'exécutent, `viewModelScope` est annulé, les abonnements temps réel sont
 *   résiliés) puis retiré du cache — pas de fuite.
 *
 * ⚠️ `entryDecorators` **remplace** la liste par défaut de `NavDisplay`. Il faut donc
 * toujours le passer **avec** `rememberSaveableStateHolderNavEntryDecorator()`, sinon on
 * perd silencieusement la rétention d'état (`rememberSaveable`, positions de défilement).
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

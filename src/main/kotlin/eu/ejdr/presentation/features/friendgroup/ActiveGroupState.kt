package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.SetActiveGroupIdUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * État global du groupe actif (sélecteur workspace, D9).
 *
 * Singleton Koin avec sa propre portée de coroutines ; charge le groupe persisté au démarrage
 * et propage les changements via [activeGroupId]. NE PAS en faire un ViewModel (pas de
 * ViewModelStoreOwner à la racine).
 */
class ActiveGroupState(
    private val getActiveGroupId: GetActiveGroupIdUseCase,
    private val setActiveGroupId: SetActiveGroupIdUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    init {
        scope.launch { _activeGroupId.value = getActiveGroupId() }
    }

    fun select(id: String?) {
        scope.launch {
            setActiveGroupId(id)
            _activeGroupId.value = id
        }
    }
}

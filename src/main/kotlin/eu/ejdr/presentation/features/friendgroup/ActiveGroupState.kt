package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.SetActiveGroupIdUseCase
import eu.ejdr.application.shared.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val getGroup: GetGroupUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    private val _activeGroupRole = MutableStateFlow<String?>(null)
    val activeGroupRole: StateFlow<String?> = _activeGroupRole.asStateFlow()

    val canEdit: StateFlow<Boolean> =
        _activeGroupRole
            .map { it == "ADMIN" || it == "MJ" }
            .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            val id = getActiveGroupId()
            _activeGroupId.value = id
            loadRole(id)
        }
    }

    fun select(id: String?) {
        scope.launch {
            setActiveGroupId(id)
            _activeGroupId.value = id
            loadRole(id)
        }
    }

    private suspend fun loadRole(id: String?) {
        _activeGroupRole.value =
            if (id == null) {
                null
            } else {
                getGroup(id).fold(onSuccess = { it.myRole }, onFailure = { null })
            }
    }
}

package eu.ejdr.presentation.features.friendgroup

import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetActiveGroupIdUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.SetActiveGroupIdUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.friendgroup.error.FriendGroupError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Nombre de réessais et délai (ms) du chargement initial du rôle, le temps que la session se restaure. */
private const val RoleRetries = 3
private const val RoleRetryDelayMs = 700L

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
            if (id != null) {
                // Au démarrage, l'auto-login (/auth/refresh) peut ne pas être terminé : le premier
                // chargement du rôle peut alors échouer en 401 (transitoire). On réessaie quelques
                // fois avant de conclure, pour laisser la session se restaurer.
                var outcome = loadRole(id)
                var attempts = 0
                while (outcome == RoleOutcome.Transient && attempts < RoleRetries) {
                    attempts++
                    delay(RoleRetryDelayMs)
                    outcome = loadRole(id)
                }
                // N'effacer le groupe persisté QUE s'il est réellement invalide (supprimé, ou on
                // n'en est plus membre). Un échec transitoire ne doit PAS détruire le groupe actif.
                if (outcome == RoleOutcome.Invalid) {
                    _activeGroupId.value = null
                    setActiveGroupId(null)
                }
            }
        }
    }

    fun select(id: String?) {
        scope.launch {
            setActiveGroupId(id)
            _activeGroupId.value = id
            loadRole(id)
        }
    }

    /** Issue du chargement du rôle, pour décider si le groupe persisté doit être conservé. */
    private enum class RoleOutcome { Ok, Invalid, Transient }

    private suspend fun loadRole(id: String?): RoleOutcome {
        if (id == null) {
            _activeGroupRole.value = null
            return RoleOutcome.Ok
        }
        return getGroup(id).fold(
            onSuccess = { detail ->
                _activeGroupRole.value = detail.myRole
                RoleOutcome.Ok
            },
            onFailure = { error ->
                _activeGroupRole.value = null
                // Groupe réellement invalide (supprimé / plus membre) vs panne transitoire.
                when (error) {
                    is FriendGroupError.NotFound, is FriendGroupError.NotMember -> RoleOutcome.Invalid
                    else -> RoleOutcome.Transient
                }
            },
        )
    }
}

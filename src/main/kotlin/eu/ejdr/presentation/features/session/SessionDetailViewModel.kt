package eu.ejdr.presentation.features.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.session.abstraction.usecase.DeleteSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.GetSessionUseCase
import eu.ejdr.application.features.session.abstraction.usecase.UpdateSessionUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.session.entities.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la page détail d'une session.
 *
 * Charge la session par son identifiant ([session]), gère la sauvegarde (titre + date) et la
 * suppression. Après une suppression réussie, [deleted] passe à `true` pour que la page
 * revienne en arrière.
 *
 * @param sessionId Identifiant de la session affichée.
 * @property getById Use case de récupération du détail d'une session.
 * @property update Use case de mise à jour d'une session.
 * @property deleteSession Use case de suppression d'une session.
 */
class SessionDetailViewModel(
    private val sessionId: String,
    private val getById: GetSessionUseCase,
    private val update: UpdateSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
) : ViewModel() {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        load()
    }

    /** Recharge la session depuis le serveur. */
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            getById(sessionId).fold(
                onSuccess = { _session.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /** Sauvegarde le titre et la date édités ; met à jour l'état en cas de succès. */
    fun save(title: String, date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            update(sessionId, title, date).fold(
                onSuccess = { _session.value = it; _error.value = null },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }

    /** Supprime la session ; en cas de succès, signale [deleted] pour revenir en arrière. */
    fun delete() {
        viewModelScope.launch {
            _isLoading.value = true
            deleteSession(sessionId).fold(
                onSuccess = { _error.value = null; _deleted.value = true },
                onFailure = { _error.value = it.message },
            )
            _isLoading.value = false
        }
    }
}

package eu.ejdr.presentation.features.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.ChangeEmailUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.ChangePasswordUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** État d'une opération d'édition (changer email / mot de passe). */
sealed interface EditState {
    /** Aucune opération en cours. */
    data object Idle : EditState

    /** Opération en attente de la réponse serveur. */
    data object Loading : EditState

    /** Opération échouée ; [message] est prêt à afficher à l'utilisateur. */
    data class Error(val message: String) : EditState

    /** Opération réussie. */
    data object Success : EditState
}

/**
 * ViewModel de l'écran profil connecté.
 *
 * Au premier montage (dans [viewModelScope], donc une seule fois tant que le ViewModel
 * est retenu), rafraîchit le profil via [getCurrentUser]. Un événement [sessionExpired]
 * est émis si la session n'est plus restaurable. Les actions [changeEmail] et
 * [changePassword] exposent leur résultat via [editState].
 *
 * @property getCurrentUser Use case de récupération du profil courant.
 * @property changeEmailUseCase Use case de changement d'adresse e-mail.
 * @property changePasswordUseCase Use case de changement de mot de passe.
 */
class UserViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val changeEmailUseCase: ChangeEmailUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
) : ViewModel() {

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    private val _sessionExpired = Channel<Unit>(Channel.BUFFERED)

    /** Événement one-shot : la session a expiré et n'est plus restaurable. */
    val sessionExpired: Flow<Unit> = _sessionExpired.receiveAsFlow()

    private val _editState = MutableStateFlow<EditState>(EditState.Idle)

    /** État courant de la dernière opération d'édition (email ou mot de passe). */
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    init {
        viewModelScope.launch { loadProfile() }
    }

    /** Demande un changement d'adresse e-mail. Met à jour [editState] en conséquence. */
    fun changeEmail(newEmail: String) {
        viewModelScope.launch {
            _editState.value = EditState.Loading
            _editState.value = when (val result = changeEmailUseCase(newEmail)) {
                is Result.Success -> {
                    refreshProfile()
                    EditState.Success
                }
                is Result.Failure -> EditState.Error(result.error.message)
            }
        }
    }

    /** Demande un changement de mot de passe. Met à jour [editState] en conséquence. */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _editState.value = EditState.Loading
            _editState.value = when (val result = changePasswordUseCase(currentPassword, newPassword)) {
                is Result.Success -> EditState.Success
                is Result.Failure -> EditState.Error(result.error.message)
            }
        }
    }

    /** Remet [editState] à [EditState.Idle] après la fermeture d'un dialog. */
    fun resetEditState() {
        _editState.value = EditState.Idle
    }

    private suspend fun loadProfile() {
        when (val result = getCurrentUser()) {
            is Result.Success -> _profile.value = result.value
            is Result.Failure ->
                if (result.error == AuthError.SessionExpired) {
                    _sessionExpired.send(Unit)
                }
        }
    }

    private suspend fun refreshProfile() {
        when (val result = getCurrentUser()) {
            is Result.Success -> _profile.value = result.value
            is Result.Failure -> Unit
        }
    }
}

package eu.ejdr.presentation.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.Credentials
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.shared.error.DomainError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * État affichable du formulaire d'authentification (login/register).
 *
 * @property email Valeur courante du champ e-mail.
 * @property password Valeur courante du champ mot de passe.
 * @property error Message d'erreur à afficher, ou `null`.
 * @property loading `true` pendant l'appel au use case.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val error: String? = null,
    val loading: Boolean = false,
)

/**
 * ViewModel partagé des écrans de connexion et d'inscription.
 *
 * Détient l'état du formulaire ([AuthUiState]) dans un [StateFlow] **retenu par la
 * destination** de navigation : l'état survit à la recomposition et à un aller-retour
 * de navigation, et l'appel asynchrone s'exécute dans le [viewModelScope] (annulé
 * proprement si l'écran est détruit, contrairement à l'ancien `rememberCoroutineScope`).
 *
 * La logique métier est injectée via [submit] : chaque écran branche son propre use
 * case (login ou register) sans dupliquer l'état ni le cycle de vie.
 *
 * @property submit Appel au use case métier avec les identifiants saisis.
 */
class AuthViewModel(
    private val submit: suspend (Credentials) -> Result<User, out DomainError>,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _authenticated = Channel<User>(Channel.BUFFERED)

    /** Événements one-shot : émis une fois l'utilisateur authentifié (navigation). */
    val authenticated: Flow<User> = _authenticated.receiveAsFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    /**
     * Soumet le formulaire : valide localement (champs non vides), appelle le use case,
     * puis émet [authenticated] en cas de succès ou met à jour [state] avec l'erreur.
     */
    fun onSubmit() {
        val current = _state.value
        val email = current.email.trim()
        if (email.isEmpty() || current.password.isEmpty()) {
            _state.update { it.copy(error = "Veuillez remplir tous les champs.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = submit(Credentials(email, current.password))) {
                is Result.Success -> _authenticated.send(result.value)
                // Source unique de vérité : message porté par l'erreur de domaine.
                is Result.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }
}

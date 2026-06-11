package eu.ejdr.presentation.features.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * ViewModel de l'écran d'accueil connecté.
 *
 * Au premier montage (dans [viewModelScope], donc **une seule fois** tant que le
 * ViewModel est retenu — contrairement à l'ancien `LaunchedEffect` qui re-déclenchait
 * `GET /me` à chaque recomposition/remount), rafraîchit le profil via
 * [GetCurrentUserUseCase]. Une [AuthError.SessionExpired] résiduelle signifie que la
 * session n'est plus restaurable : l'événement one-shot [sessionExpired] est émis pour
 * que la présentation ramène à la connexion. Sur une autre erreur (réseau), le profil
 * déjà connu reste affiché.
 *
 * @property getCurrentUser Use case de récupération du profil courant.
 */
class UserViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    private val _sessionExpired = Channel<Unit>(Channel.BUFFERED)

    /** Événement one-shot : la session a expiré et n'est plus restaurable. */
    val sessionExpired: Flow<Unit> = _sessionExpired.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = getCurrentUser()) {
                is Result.Success -> _profile.value = result.value
                is Result.Failure ->
                    if (result.error == AuthError.SessionExpired) {
                        _sessionExpired.send(Unit)
                    }
                // Autres erreurs (réseau...) : on conserve le profil déjà connu (null ici).
            }
        }
    }
}

package eu.ejdr.presentation

import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Statut de la restauration de session au démarrage. */
enum class SessionStatus { Unknown, Authenticated, Unauthenticated }

/**
 * État applicatif **global** : source de vérité unique pour le thème et le statut de
 * session, partagé par tout l'arbre depuis la racine [eu.ejdr.presentation.App].
 *
 * Remplace les `mutableStateOf` ad-hoc dispersés dans `App.kt` et le « state lifting »
 * manuel par callbacks. Volontairement borné (thème + session) ; toute donnée transverse
 * future (profil, notifications) s'ajoute ici.
 *
 * Créé au **niveau racine** (hors de l'arbre de navigation), il n'est PAS un ViewModel
 * androidx (aucun `ViewModelStoreOwner` n'existe à la racine) : c'est un state-holder
 * simple, retenu par `remember` côté composable et piloté par le [scope] fourni.
 *
 * @property scope Portée de coroutine qui pilote les chargements asynchrones.
 * @property getTheme Lecture du thème persisté (au démarrage).
 * @property restoreSession Tentative d'auto-login depuis la session persistée.
 */
class RootState(
    private val scope: CoroutineScope,
    getTheme: GetThemeUseCase,
    private val restoreSession: RestoreSessionUseCase,
) {

    private val _theme = MutableStateFlow(ThemeVariant.LIGHT)
    val theme: StateFlow<ThemeVariant> = _theme.asStateFlow()

    private val _sessionStatus = MutableStateFlow(SessionStatus.Unknown)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    init {
        scope.launch { _theme.value = getTheme() }
    }

    /** Applique un nouveau thème (déjà persisté par la feature settings). */
    fun setTheme(theme: ThemeVariant) { _theme.value = theme }

    /**
     * Marque la session comme active (connexion/inscription manuelle réussie).
     *
     * À appeler après un login/register via le formulaire : la restauration de session
     * ([restoreSession]) ne couvre que l'auto-login au démarrage. Sans ça, [sessionStatus]
     * resterait [SessionStatus.Unauthenticated] après une connexion manuelle et les éléments
     * d'UI conditionnés à l'authentification (ex. barre de navigation mobile) ne s'afficheraient pas.
     */
    fun onLoggedIn() { _sessionStatus.value = SessionStatus.Authenticated }

    /**
     * Marque la session comme terminée (déconnexion volontaire).
     *
     * À appeler après le use case de logout : sans ça, [sessionStatus] resterait
     * [SessionStatus.Authenticated] et les éléments d'UI conditionnés à l'authentification
     * (ex. barre de navigation mobile) resteraient visibles sur l'écran de connexion.
     */
    fun onLoggedOut() { _sessionStatus.value = SessionStatus.Unauthenticated }

    /** Lance la restauration de session et publie le résultat dans [sessionStatus]. */
    fun restoreSession() {
        scope.launch {
            // `val` intermédiaire requis : chaîner `.fold` directement sur l'appel du use case
            // empêche Kotlin d'inférer la borne `E : DomainError` de `fold`.
            val result = restoreSession.invoke()
            _sessionStatus.value = result.fold(
                onSuccess = { SessionStatus.Authenticated },
                onFailure = { SessionStatus.Unauthenticated },
            )
        }
    }
}

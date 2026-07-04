package eu.ejdr.presentation.features.update

import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** État du téléchargement d'une mise à jour. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float?) : DownloadState
    data object Error : DownloadState
}

/**
 * State-holder du dialog de mise à jour : porte la machine à états du téléchargement, hors
 * du composable (qui reste « bête »).
 *
 * Contrairement aux ViewModels par destination (retenus via le décorateur Nav3), ce
 * state-holder est **transitoire** : le dialog n'apparaît que ponctuellement (quand une MAJ
 * est disponible), hors de l'arbre de navigation. Il est donc simplement retenu par
 * `remember` côté composable et piloté par un [CoroutineScope] fourni (typiquement
 * `rememberCoroutineScope()`), sans dépendre d'un `ViewModelStoreOwner` (absent à la racine).
 *
 * @property downloadAndInstall Use case de téléchargement + lancement de l'installeur.
 * @property scope Portée de coroutine qui pilote le téléchargement.
 */
class UpdateController(
    private val downloadAndInstall: DownloadAndInstallUpdateUseCase,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    /**
     * Lance le téléchargement de l'installeur à [url], en publiant la progression dans [state].
     *
     * @param sha256Url URL de l'empreinte SHA-256 publiée, transmise au use case pour la
     *   vérification d'intégrité avant lancement (`null` fera échouer la vérification).
     */
    fun download(url: String, sha256Url: String?) {
        _state.value = DownloadState.Downloading(null)
        scope.launch {
            downloadAndInstall(url, sha256Url) { progress ->
                _state.value = DownloadState.Downloading(progress)
            }
                .fold(
                    onSuccess = { /* le launcher quitte l'app ; rien à faire */ },
                    onFailure = { _state.value = DownloadState.Error },
                )
        }
    }
}

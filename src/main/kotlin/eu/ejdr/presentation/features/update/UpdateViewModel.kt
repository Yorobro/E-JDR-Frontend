package eu.ejdr.presentation.features.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.fold
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
 * ViewModel du dialog de mise à jour : porte la machine à états du téléchargement, hors du
 * composable (qui redevient « bête »). Persiste par destination via le décorateur Nav3.
 *
 * @property downloadAndInstall Use case de téléchargement + lancement de l'installeur.
 */
class UpdateViewModel(
    private val downloadAndInstall: DownloadAndInstallUpdateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    /** Lance le téléchargement de l'installeur à [url], en publiant la progression dans [state]. */
    fun download(url: String) {
        _state.value = DownloadState.Downloading(null)
        viewModelScope.launch {
            downloadAndInstall(url) { progress -> _state.value = DownloadState.Downloading(progress) }
                .fold(
                    onSuccess = { /* le launcher quitte l'app ; rien à faire */ },
                    onFailure = { _state.value = DownloadState.Error },
                )
        }
    }
}

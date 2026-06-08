package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.ejdr.application.update.abstraction.UpdateInfo
import eu.ejdr.application.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.launch

private sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float?) : DownloadState
    data object Error : DownloadState
}

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    downloadAndInstall: DownloadAndInstallUpdateUseCase,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    AlertDialog(
        onDismissRequest = { if (state is DownloadState.Idle || state is DownloadState.Error) onDismiss() },
        title = { AppText("Mise à jour disponible", style = AppTextStyle.Title) },
        text = {
            when (val s = state) {
                is DownloadState.Idle -> AppText("La version ${info.version} est disponible.")
                is DownloadState.Downloading -> {
                    if (s.progress != null) {
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.primary,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.primary,
                        )
                    }
                }
                is DownloadState.Error -> AppText("Le téléchargement a échoué. Réessayez.")
            }
        },
        confirmButton = {
            when (state) {
                is DownloadState.Idle -> AppButton(
                    label = "Installer",
                    onClick = {
                        val url = info.downloadUrl
                        if (url == null) {
                            runCatching { Desktop.getDesktop().browse(URI(info.releaseUrl)) }
                            onDismiss()
                        } else {
                            state = DownloadState.Downloading(null)
                            scope.launch {
                                runCatching {
                                    downloadAndInstall(url) { progress ->
                                        state = DownloadState.Downloading(progress)
                                    }
                                }.onFailure {
                                    state = DownloadState.Error
                                }
                            }
                        }
                    },
                )
                is DownloadState.Error -> AppButton(
                    label = "Réessayer",
                    onClick = {
                        val url = info.downloadUrl ?: return@AppButton
                        state = DownloadState.Downloading(null)
                        scope.launch {
                            runCatching {
                                downloadAndInstall(url) { progress ->
                                    state = DownloadState.Downloading(progress)
                                }
                            }.onFailure {
                                state = DownloadState.Error
                            }
                        }
                    },
                )
                else -> {}
            }
        },
        dismissButton = {
            if (state !is DownloadState.Downloading) {
                AppButton(label = "Plus tard", onClick = onDismiss, variant = ButtonVariant.Ghost)
            }
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}

package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.presentation.features.update.DownloadState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Dialog « bête » de mise à jour : il affiche l'[state] fourni et émet des callbacks.
 * Toute la logique (téléchargement, progression, erreur) vit dans le state-holder `UpdateController` appelant.
 */
@Composable
fun UpdateDialog(
    info: UpdateInfoDto,
    state: DownloadState,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (state is DownloadState.Idle || state is DownloadState.Error) onDismiss() },
        title = { AppText("Mise à jour disponible", style = AppTextStyle.Title) },
        text = {
            when (state) {
                is DownloadState.Idle -> AppText("La version ${info.version} est disponible.")
                is DownloadState.Downloading ->
                    if (state.progress != null) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.primary,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.primary,
                        )
                    }
                is DownloadState.Error -> AppText("Le téléchargement a échoué. Réessayez.")
            }
        },
        confirmButton = {
            when (state) {
                is DownloadState.Idle -> AppButton(
                    label = "Installer",
                    onClick = { if (info.downloadUrl == null) onOpenReleasePage() else onInstall() },
                )
                is DownloadState.Error -> AppButton(label = "Réessayer", onClick = onRetry)
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

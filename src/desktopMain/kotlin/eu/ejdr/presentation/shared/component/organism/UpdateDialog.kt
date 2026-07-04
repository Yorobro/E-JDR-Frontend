package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.presentation.features.update.DownloadState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.base.AppDialogCore
import eu.ejdr.presentation.shared.component.base.AppProgressBar
import eu.ejdr.presentation.shared.component.base.AppSurface
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
    AppDialogCore(
        onDismiss = { if (state is DownloadState.Idle || state is DownloadState.Error) onDismiss() },
    ) {
        AppSurface(
            color = AppTheme.colors.surface,
            shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
            elevation = AppTheme.dimens.elevationLg,
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimens.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
            ) {
                AppText("Mise à jour disponible", style = AppTextStyle.Title)
                when (state) {
                    is DownloadState.Idle -> AppText("La version ${info.version} est disponible.")
                    is DownloadState.Downloading ->
                        AppProgressBar(
                            progress = state.progress ?: 0f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is DownloadState.Error -> AppText("Le téléchargement a échoué. Réessayez.")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm, Alignment.End),
                ) {
                    if (state !is DownloadState.Downloading) {
                        AppButton(label = "Plus tard", onClick = onDismiss, variant = ButtonVariant.Ghost)
                    }
                    when (state) {
                        is DownloadState.Idle -> AppButton(
                            label = "Installer",
                            onClick = { if (info.downloadUrl == null) onOpenReleasePage() else onInstall() },
                        )
                        is DownloadState.Error -> AppButton(label = "Réessayer", onClick = onRetry)
                        else -> {}
                    }
                }
            }
        }
    }
}

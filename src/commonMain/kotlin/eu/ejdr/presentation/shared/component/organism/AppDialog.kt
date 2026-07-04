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
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.base.AppDialogCore
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Modal **générique et réutilisable** du design system.
 *
 * Composant bête : il encapsule les choix de direction artistique d'une boîte de dialogue
 * (fond `surface`, coins arrondis `radiusMd`, titre [AppText] de style `Title`, boutons
 * [AppButton]) et expose le contenu via le slot [content]. Toute feature ayant besoin d'un
 * modal s'appuie dessus plutôt que de réécrire un dialog : seul le contenu et les
 * libellés/variantes des boutons changent.
 *
 * L'apparition est animée : fade + léger scale piloté par [AppDialogCore] (respecte reduced-motion).
 *
 * @param title Titre affiché en haut du modal.
 * @param onDismiss Callback de fermeture (clic hors du modal ou bouton d'annulation).
 * @param confirmLabel Libellé du bouton de confirmation.
 * @param onConfirm Callback du bouton de confirmation.
 * @param modifier Modifier Compose appliqué au dialog.
 * @param dismissLabel Libellé du bouton d'annulation ; si `null`, le bouton est masqué.
 * @param confirmVariant Variante visuelle du bouton de confirmation (ex. [ButtonVariant.Danger]).
 * @param confirmEnabled Active ou désactive le bouton de confirmation.
 * @param content Contenu du modal (texte, champ de saisie, etc.).
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = "Annuler",
    confirmVariant: ButtonVariant = ButtonVariant.Primary,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dimens = AppTheme.dimens

    AppDialogCore(onDismiss = onDismiss, modifier = modifier) {
        AppSurface(
            color = AppTheme.colors.surface,
            shape = RoundedCornerShape(dimens.radiusMd),
            elevation = dimens.elevationLg,
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.lg),
                verticalArrangement = Arrangement.spacedBy(dimens.md),
            ) {
                AppText(title, style = AppTextStyle.Subtitle)
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.sm, Alignment.End),
                ) {
                    if (dismissLabel != null) {
                        AppButton(label = dismissLabel, onClick = onDismiss, variant = ButtonVariant.Ghost)
                    }
                    AppButton(
                        label = confirmLabel,
                        onClick = onConfirm,
                        variant = confirmVariant,
                        enabled = confirmEnabled,
                    )
                }
            }
        }
    }
}

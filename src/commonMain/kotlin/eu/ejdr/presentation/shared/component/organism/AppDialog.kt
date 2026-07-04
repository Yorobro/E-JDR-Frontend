package eu.ejdr.presentation.shared.component.organism

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Modal **générique et réutilisable** du design system.
 *
 * Composant bête : il encapsule les choix de direction artistique d'une boîte de dialogue
 * (fond `surface`, coins arrondis `radiusMd`, titre [AppText] de style `Title`, boutons
 * [AppButton]) et expose le contenu via le slot [content]. Toute feature ayant besoin d'un
 * modal s'appuie dessus plutôt que de réécrire un `AlertDialog` : seul le contenu et les
 * libellés/variantes des boutons changent.
 *
 * L'apparition est animée : fade + léger scale (0.95 → 1) piloté par [AppTheme.motion].
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
    val motion = AppTheme.motion
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = tween(
            durationMillis = motion.effectiveDuration(motion.durationMedium),
            easing = motion.easeStandard,
        ),
        label = "dialogScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.effectiveDuration(motion.durationMedium),
            easing = motion.easeStandard,
        ),
        label = "dialogAlpha",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        title = { AppText(title, style = AppTextStyle.Title) },
        text = content,
        confirmButton = {
            AppButton(
                label = confirmLabel,
                onClick = onConfirm,
                variant = confirmVariant,
                enabled = confirmEnabled,
            )
        },
        dismissButton = dismissLabel?.let {
            { AppButton(label = it, onClick = onDismiss, variant = ButtonVariant.Ghost) }
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}

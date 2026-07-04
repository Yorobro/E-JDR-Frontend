package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Bandeau inline signalant qu'une mise à jour distante de la fiche est arrivée pendant
 * l'édition. L'utilisateur choisit : recharger (perd ses modifs) ou ignorer (garde sa saisie).
 *
 * @param onReload Recharge la fiche depuis le serveur.
 * @param onDismiss Ferme le bandeau en gardant la saisie en cours.
 * @param modifier Modifier Compose appliqué au bandeau.
 */
@Composable
fun RemoteChangeBanner(
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimens.sm))
            .background(AppTheme.colors.surface)
            .padding(AppTheme.dimens.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = "Cette fiche a été modifiée ailleurs.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.text,
            modifier = Modifier.weight(1f),
        )
        AppButton(label = "Recharger", onClick = onReload)
        AppButton(label = "Ignorer", onClick = onDismiss, variant = ButtonVariant.Secondary)
    }
}

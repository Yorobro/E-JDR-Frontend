package eu.ejdr.presentation.features.session.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.session.entities.Session
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

private val CardHeight = 96.dp

/**
 * Tuile d'une session dans la liste du détail de campagne (composant bête).
 *
 * Tuile à hauteur fixe (fond `surface`, bordure, coins arrondis) : titre centré, date en
 * dessous. Toute la tuile est cliquable et ouvre le détail de la session. La suppression se
 * fait depuis l'écran de détail (pas d'icône ici).
 *
 * @param session Session à afficher.
 * @param onClick Callback déclenché au clic sur la tuile (ouvre le détail).
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .clickable(onClick = onClick)
            .padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(
            text = session.title,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
        AppText(
            text = session.date,
            style = AppTextStyle.Caption,
            color = AppTheme.colors.textSecondary,
        )
    }
}

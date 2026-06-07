package eu.ejdr.presentation.feature.user.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page d'accueil affichée une fois l'utilisateur connecté.
 *
 * Rendue dans la zone connectée (sous la top bar via `AppScaffold`). Pour l'instant elle se
 * contente d'afficher un message de bienvenue et, si le profil est disponible, l'e-mail de
 * l'utilisateur. Elle pourra plus tard appeler ses propres use cases (composant intelligent).
 *
 * @param user Utilisateur connecté, ou `null` si arrivé par auto-login sans profil chargé.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun UserPage(
    user: User?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm, Alignment.CenterVertically),
    ) {
        AppText(text = "Bienvenue sur E-JDR", style = AppTextStyle.Title)
        if (user != null) {
            AppText(
                text = "Connecté en tant que ${user.email}",
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

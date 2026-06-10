package eu.ejdr.presentation.feature.user.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.application.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Page d'accueil affichée une fois l'utilisateur connecté (composant intelligent).
 *
 * Au montage, rafraîchit le profil via [GetCurrentUserUseCase] (`GET /me`) — première
 * route protégée : si l'access token a expiré, l'intercepteur du client HTTP tente un
 * refresh silencieux de façon transparente. Une [AuthError.SessionExpired] résiduelle
 * signifie que la session n'est plus restaurable : [onSessionExpired] est invoqué.
 * Sur une erreur réseau, le profil déjà connu (fourni par [user]) reste affiché.
 *
 * @param user Profil déjà connu (issu du login ou de l'auto-login), ou `null`.
 * @param onSessionExpired Callback de retour à l'écran de connexion.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun UserPage(
    user: User?,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val getCurrentUser = koinInject<GetCurrentUserUseCase>()
    var profile by remember { mutableStateOf(user) }

    LaunchedEffect(Unit) {
        when (val result = getCurrentUser()) {
            is Result.Success -> profile = result.value
            is Result.Failure ->
                if (result.error == AuthError.SessionExpired) {
                    onSessionExpired()
                }
            // Autres erreurs (réseau...) : on conserve le profil déjà connu.
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm, Alignment.CenterVertically),
    ) {
        AppText(text = "Bienvenue sur E-JDR", style = AppTextStyle.Title)
        profile?.let { current ->
            AppText(
                text = "Connecté en tant que ${current.email}",
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

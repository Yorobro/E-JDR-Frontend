package eu.ejdr.presentation.features.user.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.presentation.features.user.UserViewModel
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page d'accueil affichée une fois l'utilisateur connecté (composant intelligent).
 *
 * Crée un [UserViewModel] retenu par la destination (qui rafraîchit le profil via
 * `GET /me` une seule fois) et observe son état. L'événement one-shot
 * [UserViewModel.sessionExpired] déclenche [onSessionExpired].
 *
 * @param onSessionExpired Callback de retour à l'écran de connexion.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun UserPage(
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel { UserViewModel(get<GetCurrentUserUseCase>()) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sessionExpired.collect { onSessionExpired() }
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

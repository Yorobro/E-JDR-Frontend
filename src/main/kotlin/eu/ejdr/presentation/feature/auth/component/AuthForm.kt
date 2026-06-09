package eu.ejdr.presentation.feature.auth.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppPasswordField
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.AppTextField
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.component.molecule.FormError
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Organisme de formulaire d'authentification (connexion ou inscription).
 *
 * Composant bête : reçoit tout l'état et les callbacks, ne détient rien.
 * Les 4 chaînes variables ([subtitle], [submitLabel], [secondaryText], [secondaryActionLabel])
 * permettent de partager la structure entre [LoginPage] et [RegisterPage].
 *
 * @param email Valeur courante du champ email.
 * @param password Valeur courante du champ mot de passe.
 * @param errorMessage Message d'erreur à afficher, ou `null` si aucun.
 * @param loading Si vrai, désactive les champs et affiche un indicateur sur le bouton principal.
 * @param onEmailChange Callback de mise à jour de l'email.
 * @param onPasswordChange Callback de mise à jour du mot de passe.
 * @param onSubmit Callback déclenché à la soumission du formulaire.
 * @param onSecondaryAction Callback du lien de navigation secondaire (vers l'autre page auth).
 * @param subtitle Texte descriptif sous le titre de l'application.
 * @param submitLabel Libellé du bouton de soumission.
 * @param secondaryText Texte d'accroche précédant le lien secondaire.
 * @param secondaryActionLabel Libellé du lien de navigation secondaire.
 * @param modifier Modifier Compose appliqué au composant.
 */
@Composable
fun AuthForm(
    email: String,
    password: String,
    errorMessage: String?,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSecondaryAction: () -> Unit,
    subtitle: String,
    submitLabel: String,
    secondaryText: String,
    secondaryActionLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(AppTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(360.dp),
            shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
            color = AppTheme.colors.surface,
            shadowElevation = 2.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimens.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthFormHeader(subtitle = subtitle)

                Spacer(Modifier.height(AppTheme.dimens.lg))

                AuthFormFields(
                    email = email,
                    password = password,
                    errorMessage = errorMessage,
                    loading = loading,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                )

                AppButton(
                    label = submitLabel,
                    onClick = onSubmit,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )

                AuthFormFooter(
                    secondaryText = secondaryText,
                    secondaryActionLabel = secondaryActionLabel,
                    loading = loading,
                    onSecondaryAction = onSecondaryAction,
                )
            }
        }
    }
}

/** Titre de l'application et sous-titre descriptif, centrés en haut du formulaire. */
@Composable
private fun AuthFormHeader(subtitle: String) {
    AppText(
        text = "E-JDR",
        style = AppTextStyle.Title,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(AppTheme.dimens.xs))
    AppText(
        text = subtitle,
        style = AppTextStyle.Body,
        color = AppTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

/** Champs email et mot de passe suivis du message d'erreur éventuel. */
@Composable
private fun AuthFormFields(
    email: String,
    password: String,
    errorMessage: String?,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    AppTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Email",
        enabled = !loading,
        leadingIcon = Icons.Outlined.Email,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppTheme.dimens.sm))
    AppPasswordField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Mot de passe",
        enabled = !loading,
        leadingIcon = Icons.Outlined.Lock,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(AppTheme.dimens.xs))
    FormError(errorMessage)
    Spacer(Modifier.height(AppTheme.dimens.sm))
}

/** Séparateur et lien de navigation vers l'autre page d'authentification. */
@Composable
private fun AuthFormFooter(
    secondaryText: String,
    secondaryActionLabel: String,
    loading: Boolean,
    onSecondaryAction: () -> Unit,
) {
    Spacer(Modifier.height(AppTheme.dimens.md))
    HorizontalDivider(color = AppTheme.colors.border)
    Spacer(Modifier.height(AppTheme.dimens.xs))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
    ) {
        AppText(
            text = secondaryText,
            style = AppTextStyle.Caption,
            color = AppTheme.colors.textSecondary,
        )
        AppButton(
            label = secondaryActionLabel,
            onClick = onSecondaryAction,
            variant = ButtonVariant.Text,
            enabled = !loading,
        )
    }
}

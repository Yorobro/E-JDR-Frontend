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

@Composable
fun LoginForm(
    email: String,
    password: String,
    errorMessage: String?,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToRegister: () -> Unit,
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
                AppText(
                    text = "E-JDR",
                    style = AppTextStyle.Title,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(AppTheme.dimens.xs))
                AppText(
                    text = "Connectez-vous pour continuer",
                    style = AppTextStyle.Body,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(AppTheme.dimens.lg))

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

                AppButton(
                    label = "Se connecter",
                    onClick = onSubmit,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(AppTheme.dimens.md))
                HorizontalDivider(color = AppTheme.colors.border)
                Spacer(Modifier.height(AppTheme.dimens.xs))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs),
                ) {
                    AppText(
                        text = "Pas encore de compte ?",
                        style = AppTextStyle.Caption,
                        color = AppTheme.colors.textSecondary,
                    )
                    AppButton(
                        label = "S'inscrire",
                        onClick = onGoToRegister,
                        variant = ButtonVariant.Text,
                        enabled = !loading,
                    )
                }
            }
        }
    }
}

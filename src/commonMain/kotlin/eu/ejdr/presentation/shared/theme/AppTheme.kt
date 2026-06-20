package eu.ejdr.presentation.shared.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors non fourni : encapsulez l'UI dans AppTheme { }")
}
private val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
private val LocalAppDimens = staticCompositionLocalOf { AppDimens() }

/**
 * Point d'accès unique au design system depuis les composables.
 *
 * Expose les jetons de design (couleurs, typographie, dimensions) fournis par le
 * composable racine [AppTheme]. Les composants lisent toujours ces valeurs plutôt
 * que des constantes en dur, ce qui centralise l'apparence.
 */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
    val dimens: AppDimens
        @Composable @ReadOnlyComposable get() = LocalAppDimens.current
}

/**
 * Fournit le design system à l'arbre de composables.
 *
 * À placer une fois à la racine de l'application. Tout composant descendant peut
 * alors lire [AppTheme.colors], [AppTheme.typography] et [AppTheme.dimens].
 *
 * @param colors Palette à utiliser (par défaut [lightColors]).
 * @param typography Typographie à utiliser.
 * @param dimens Dimensions à utiliser.
 * @param content Contenu de l'application.
 */
@Composable
fun AppTheme(
    colors: AppColors = lightColors(),
    typography: AppTypography = AppTypography(),
    dimens: AppDimens = AppDimens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppDimens provides dimens,
        // Couleur de contenu par défaut de l'app : les atomes qui s'appuient sur
        // LocalContentColor (ex. AppIcon sans tint explicite) héritent de `text`. Les
        // conteneurs Material (FAB, bouton…) la surchargent localement par leur contentColor.
        LocalContentColor provides colors.text,
        content = content,
    )
}

package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors non fourni : encapsulez l'UI dans AppTheme { }")
}
private val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
private val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
private val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
private val LocalAppElevation = staticCompositionLocalOf { AppElevation() }

/**
 * Point d'accès unique au design system depuis les composables.
 *
 * Expose les jetons de design (couleurs, typographie, dimensions, motion) fournis par le
 * composable racine [AppTheme]. Les composants lisent toujours ces valeurs plutôt que des
 * constantes en dur, ce qui centralise l'apparence. Ne dépend d'aucun framework externe.
 */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
    val dimens: AppDimens
        @Composable @ReadOnlyComposable get() = LocalAppDimens.current
    val motion: AppMotion
        @Composable @ReadOnlyComposable get() = LocalAppMotion.current
    val elevation: AppElevation
        @Composable @ReadOnlyComposable get() = LocalAppElevation.current
    val treatment: AppTreatment
        @Composable @ReadOnlyComposable get() = LocalAppTreatment.current
}

/**
 * Fournit le design system à l'arbre de composables (100 % maison, sans Material).
 *
 * @param colors Palette à utiliser (par défaut le thème de `ThemeVariant.DEFAULT`).
 * @param typography Typographie à utiliser.
 * @param dimens Dimensions à utiliser.
 * @param motion Jetons d'animation à utiliser.
 * @param elevation Jetons d'ombre à utiliser.
 * @param content Contenu de l'application.
 */
@Composable
fun AppTheme(
    colors: AppColors = colorsFor(eu.ejdr.domain.features.settings.entities.ThemeVariant.DEFAULT),
    typography: AppTypography = appTypography(),
    dimens: AppDimens = AppDimens(),
    motion: AppMotion = AppMotion(),
    elevation: AppElevation = AppElevation(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppDimens provides dimens,
        LocalAppMotion provides motion,
        LocalAppElevation provides elevation,
        LocalContentColor provides colors.text,
        content = content,
    )
}

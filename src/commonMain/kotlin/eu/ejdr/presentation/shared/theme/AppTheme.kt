package eu.ejdr.presentation.shared.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    val motion: AppMotion
        @Composable @ReadOnlyComposable get() = LocalAppMotion.current
}

/**
 * Fournit le design system à l'arbre de composables.
 *
 * À placer une fois à la racine de l'application. Tout composant descendant peut
 * alors lire [AppTheme.colors], [AppTheme.typography] et [AppTheme.dimens].
 *
 * @param colors Palette à utiliser (par défaut le thème [colorsFor] de [eu.ejdr.domain.features.settings.entities.ThemeVariant.DEFAULT]).
 * @param typography Typographie à utiliser.
 * @param dimens Dimensions à utiliser.
 * @param content Contenu de l'application.
 */
@Composable
fun AppTheme(
    colors: AppColors = colorsFor(eu.ejdr.domain.features.settings.entities.ThemeVariant.DEFAULT),
    typography: AppTypography = appTypography(),
    dimens: AppDimens = AppDimens(),
    motion: AppMotion = AppMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppDimens provides dimens,
        LocalAppMotion provides motion,
        // Couleur de contenu par défaut de l'app : les atomes qui s'appuient sur
        // LocalContentColor (ex. AppIcon sans tint explicite) héritent de `text`. Les
        // conteneurs Material (FAB, bouton…) la surchargent localement par leur contentColor.
        LocalContentColor provides colors.text,
    ) {
        // Material3 ColorScheme dérivé de nos AppColors : sans ça, les composants Material
        // BRUTS (Surface, NavigationBar, AlertDialog, indicateurs…) resteraient sur le schéma
        // clair par défaut → fond clair persistant en thème sombre. On part du schéma de base
        // correspondant (light/dark, qui remplit tous les rôles cohéremment) puis on surcharge
        // les rôles clés avec la palette du design system.
        MaterialTheme(colorScheme = colors.toMaterialColorScheme(), content = content)
    }
}

/** Projette nos [AppColors] sur un [androidx.compose.material3.ColorScheme] Material3. */
private fun AppColors.toMaterialColorScheme() =
    (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = primary,
        onPrimary = onPrimary,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = beige,
        onSurfaceVariant = textSecondary,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = beige,
        surfaceContainerLow = background,
        surfaceContainerLowest = background,
        outline = border,
        outlineVariant = border,
        error = danger,
        onError = onDanger,
    )

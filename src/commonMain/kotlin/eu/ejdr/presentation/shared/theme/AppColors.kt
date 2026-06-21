package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs du design system (neutres gris/beige + accent taupe).
 *
 * Conteneur immuable lu par tous les composants via [AppTheme]. Modifier ces
 * valeurs (ou fournir une autre instance) change l'apparence de toute l'application.
 *
 * @property background Fond général de l'application.
 * @property surface Fond des surfaces surélevées (barres, cartes).
 * @property beige Teinte beige intermédiaire pour les fonds secondaires.
 * @property border Couleur des bordures au repos.
 * @property muted Texte/élément atténué (placeholders, désactivé).
 * @property textSecondary Texte secondaire et labels.
 * @property text Couleur du texte principal.
 * @property primary Couleur d'accent (bouton primaire, focus, éléments actifs).
 * @property onPrimary Couleur du contenu posé sur [primary].
 * @property danger Couleur des erreurs et actions destructives.
 * @property onDanger Couleur du contenu posé sur [danger].
 * @property isDark Vrai si cette palette est sombre : sert de base au [ColorScheme] Material3
 * (darkColorScheme vs lightColorScheme) pour que les composants Material bruts (Surface,
 * NavigationBar, dialogs…) suivent eux aussi le thème.
 */
data class AppColors(
    val background: Color,
    val surface: Color,
    val beige: Color,
    val border: Color,
    val muted: Color,
    val textSecondary: Color,
    val text: Color,
    val primary: Color,
    val onPrimary: Color,
    val danger: Color,
    val onDanger: Color,
    val isDark: Boolean,
)

/** Thème Parchemin — clair, chaleureux (beige + sceau de cire). */
fun parchmentColors(): AppColors = AppColors(
    background = ParchmentPalette.parchment,
    surface = ParchmentPalette.vellum,
    beige = ParchmentPalette.sand,
    border = ParchmentPalette.oak,
    muted = ParchmentPalette.faded,
    textSecondary = ParchmentPalette.sepia,
    text = ParchmentPalette.ink,
    primary = ParchmentPalette.bole,
    onPrimary = ParchmentPalette.onAccent,
    danger = ParchmentPalette.rust,
    onDanger = ParchmentPalette.onAccent,
    isDark = false,
)

/** Thème Taupe — clair, minimaliste (gris/beige neutre, surfaces blanches). */
fun taupeColors(): AppColors = AppColors(
    background = TaupePalette.background,
    surface = TaupePalette.surface,
    beige = TaupePalette.beige,
    border = TaupePalette.border,
    muted = TaupePalette.faded,
    textSecondary = TaupePalette.sepia,
    text = TaupePalette.ink,
    primary = TaupePalette.taupe,
    onPrimary = TaupePalette.onAccent,
    danger = TaupePalette.rust,
    onDanger = TaupePalette.onAccent,
    isDark = false,
)

/** Thème Grimoire — sombre, premium (brun-noir chaud, accent laiton). */
fun grimoireColors(): AppColors = AppColors(
    background = GrimoirePalette.background,
    surface = GrimoirePalette.surface,
    beige = GrimoirePalette.raised,
    border = GrimoirePalette.border,
    muted = GrimoirePalette.faded,
    textSecondary = GrimoirePalette.sepia,
    text = GrimoirePalette.cream,
    primary = GrimoirePalette.brass,
    onPrimary = GrimoirePalette.onAccent,
    danger = GrimoirePalette.ember,
    onDanger = GrimoirePalette.onDanger,
    isDark = true,
)

// Deprecated stubs retained for App.kt/AppTheme.kt until Task 3+4 refactor them.
// TODO: Remove after Task 3 refactors App.kt and Task 4 refactors AppTheme.kt.
@Deprecated("Use parchmentColors(), taupeColors(), or grimoireColors() instead. Task 3+4 will remove.")
fun lightColors(): AppColors = taupeColors()

@Deprecated("Use grimoireColors() instead. Task 3+4 will remove.")
fun darkColors(): AppColors = grimoireColors()

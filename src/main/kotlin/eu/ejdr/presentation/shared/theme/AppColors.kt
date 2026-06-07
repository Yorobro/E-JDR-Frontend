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
)

/** Palette claire par défaut (gris/beige, accent taupe). */
fun lightColors(): AppColors = AppColors(
    background = Color(0xFFFAF8F4),
    surface = Color(0xFFEFE9E1),
    beige = Color(0xFFDDD5C8),
    border = Color(0xFFB8AF9D),
    muted = Color(0xFF8A8275),
    textSecondary = Color(0xFF5B554C),
    text = Color(0xFF33302B),
    primary = Color(0xFF5B554C),
    onPrimary = Color(0xFFFFFFFF),
    danger = Color(0xFFA13D33),
    onDanger = Color(0xFFFFFFFF),
)

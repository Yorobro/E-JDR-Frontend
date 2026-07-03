package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Intensité de traitement visuel d'une zone de l'UI.
 *
 * [Rich] : ornements dorés, filets, reliefs (écrans vitrine : Splash, Auth, Accueil, Fiche).
 * [Plain] : sobre soigné, sans ornement (listes, référentiels, réglages, dialogues).
 * Les composants lisent `AppTheme.treatment` pour adapter leur rendu sans dupliquer d'écran.
 */
enum class AppTreatment { Rich, Plain }

val LocalAppTreatment = staticCompositionLocalOf { AppTreatment.Plain }

/** Applique un [AppTreatment] à un sous-arbre (les écrans vitrine posent [AppTreatment.Rich]). */
@Composable
fun ProvideTreatment(treatment: AppTreatment, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppTreatment provides treatment,
        content = content,
    )
}

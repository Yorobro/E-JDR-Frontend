package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color

/**
 * Teintes brutes du design system — **unique source de vérité chromatique**.
 *
 * Chaque couleur n'est définie qu'ICI, nommée par ce qu'elle EST (et non par son rôle).
 * Les rôles sémantiques (`AppColors`) pointent vers ces teintes. Pour changer une couleur
 * dans toute l'app, modifier une seule `val` ici.
 *
 * Trois familles, une par thème : Parchemin (clair chaleureux), Taupe (clair minimaliste),
 * Grimoire (sombre premium).
 */

/** Parchemin — beige chaud, encre brune, sceau de cire. */
object ParchmentPalette {
    val parchment = Color(0xFFF2EBDC) // fond, vieille page
    val vellum = Color(0xFFFBF7EE)    // surfaces élevées (plus claires → profondeur)
    val sand = Color(0xFFE4D8C2)      // zones secondaires
    val oak = Color(0xFFC2B393)       // bordures
    val ink = Color(0xFF2E2A22)       // texte principal (brun-noir d'encre)
    val sepia = Color(0xFF6B5D45)     // texte secondaire
    val faded = Color(0xFF9A8E76)     // texte atténué / placeholder
    val bole = Color(0xFF8A4B3A)      // ACCENT : terre de Sienne (sceau)
    val boleHi = Color(0xFFA15B48)    // accent survol/pressé
    val gold = Color(0xFFB08A3E)      // accent rare (actif/badge)
    val rust = Color(0xFF9E3B2E)      // danger
    val onAccent = Color(0xFFFBF7EE)  // contenu sur accent/danger
}

/** Taupe — gris/beige neutre, minimaliste, surfaces blanches. */
object TaupePalette {
    val background = Color(0xFFFAF8F4) // fond crème (conservé de l'ancien thème)
    val surface = Color(0xFFFFFFFF)    // blanc pur → cartes qui se détachent
    val beige = Color(0xFFEFEAE1)      // zones secondaires
    val border = Color(0xFFE0D9CC)     // bordures hairline claires
    val ink = Color(0xFF2A2722)        // texte principal
    val sepia = Color(0xFF6B655B)      // texte secondaire
    val faded = Color(0xFF9C958A)      // texte atténué
    val taupe = Color(0xFF5A5248)      // ACCENT taupe (conservé, enrichi)
    val taupeHi = Color(0xFF74695A)    // accent survol/pressé
    val rust = Color(0xFFA13D33)       // danger
    val onAccent = Color(0xFFFFFFFF)   // contenu sur accent/danger
}

/** Grimoire — brun-noir chaud, crème, laiton/or vieilli. */
object GrimoirePalette {
    val background = Color(0xFF16140F) // presque noir, chaud (pas bleuté)
    val surface = Color(0xFF211D16)    // cartes
    val raised = Color(0xFF2B261D)     // surfaces hautes / zones secondaires
    val border = Color(0xFF3D372C)     // bordures
    val cream = Color(0xFFECE4D6)      // texte principal crème chaud
    val sepia = Color(0xFF9E9683)      // texte secondaire
    val faded = Color(0xFF6E675A)      // texte atténué
    val brass = Color(0xFFC9A24B)      // ACCENT : laiton/or vieilli
    val brassHi = Color(0xFFDBB662)    // accent survol/pressé
    val ember = Color(0xFFB5503A)      // danger (rouge braise)
    val onAccent = Color(0xFF16140F)   // contenu sur accent (sombre sur or)
    val onDanger = Color(0xFFFBF7EE)   // contenu sur danger
}

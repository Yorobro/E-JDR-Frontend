package eu.ejdr.domain.features.settings.entities

/** Thèmes disponibles dans l'application (chacun a son ambiance figée). */
enum class ThemeVariant {
    /** Clair, chaleureux (beige + sceau de cire). */
    PARCHEMIN,

    /** Clair, minimaliste (gris/beige neutre). */
    TAUPE,

    /** Sombre, premium (brun-noir chaud, accent laiton). */
    GRIMOIRE,

    ;

    companion object {
        /** Thème par défaut / repli sûr quand rien n'est persisté. */
        val DEFAULT = PARCHEMIN
    }
}

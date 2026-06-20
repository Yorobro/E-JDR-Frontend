package eu.ejdr.presentation.features.charactersheet.component

/**
 * Source de vérité unique pour les slugs de statistiques tels qu'attendus/renvoyés par le serveur.
 *
 * Ces valeurs ne doivent PAS être modifiées sans mise à jour correspondante côté API. Tout code qui
 * compare ou transmet un slug de stat doit référencer ces constantes plutôt que des littéraux inline,
 * afin qu'une faute de frappe soit détectée à la compilation.
 */
object StatKeys {
    const val DEXTERITE = "dexterite"
    const val INTELLIGENCE = "intelligence"
    const val PERCEPTION = "perception"
    const val SOCIAL = "social"
    const val VIGUEUR = "vigueur"

    /**
     * Liste ordonnée (slug, libellé FR) pour les composants d'affichage/sélection des
     * caractéristiques. L'ordre correspond à l'ordre de la fiche papier.
     */
    val ORDERED: List<Pair<String, String>> = listOf(
        DEXTERITE to "Dextérité",
        INTELLIGENCE to "Intelligence",
        PERCEPTION to "Perception",
        SOCIAL to "Social",
        VIGUEUR to "Vigueur",
    )
}

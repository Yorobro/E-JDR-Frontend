package eu.ejdr.domain.features.charactersheet.entities

/**
 * Bourse d'un personnage (domaine front anémique) : pièces d'or, d'argent et de cuivre.
 *
 * @property gold Pièces d'or.
 * @property silver Pièces d'argent.
 * @property copper Pièces de cuivre.
 */
data class Purse(
    val gold: Int,
    val silver: Int,
    val copper: Int,
)

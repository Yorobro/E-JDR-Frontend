package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.Purse

private const val SILVER_PER_GOLD = 100
private const val COPPER_PER_SILVER = 100

/** Total de la bourse en pièces de cuivre. */
private fun Purse.totalInCopper(): Int =
    gold * SILVER_PER_GOLD * COPPER_PER_SILVER + silver * COPPER_PER_SILVER + copper

/**
 * Représente une bourse sous forme normalisée lisible (ex. « 1 PO · 50 PA · 0 PC »).
 * Recombine les pièces selon 1 PO = 100 PA = 10 000 PC.
 */
fun Purse.formatNormalized(): String {
    var total = totalInCopper()
    val g = total / (SILVER_PER_GOLD * COPPER_PER_SILVER)
    total -= g * SILVER_PER_GOLD * COPPER_PER_SILVER
    val s = total / COPPER_PER_SILVER
    val c = total - s * COPPER_PER_SILVER
    return "$g PO · $s PA · $c PC"
}

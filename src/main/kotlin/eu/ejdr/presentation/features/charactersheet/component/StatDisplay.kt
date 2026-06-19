package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.charactersheet.entities.ResolvedReference

/**
 * Modèle d'affichage d'une caractéristique : valeur de base + bonus dérivés + total.
 *
 * La [base] reste la valeur ÉDITABLE de la fiche (jamais altérée par les bonus). Les [bonuses]
 * sont les montants apportés par les sources qui ciblent cette stat (peuple puis formation, dans
 * cet ordre) ; ils sont purement d'affichage. Le [total] est `(base ?: 0) + somme(bonuses)`.
 *
 * @property base Valeur de base de la stat (telle que saisie), ou `null` si non renseignée.
 * @property bonuses Montants des bonus ciblant cette stat, dans l'ordre peuple → formation.
 * @property total Somme de la base (0 si null) et des bonus.
 */
data class StatDisplay(
    val base: Int?,
    val bonuses: List<Int>,
    val total: Int,
) {
    /** Y a-t-il au moins un bonus à afficher (sinon : rendu identique à avant) ? */
    val hasBonus: Boolean get() = bonuses.isNotEmpty()
}

/**
 * Calcule l'affichage d'une stat à partir de sa base et des sources résolues (peuple, formation).
 *
 * Une source contribue un bonus uniquement si sa `stat` (slug serveur) est égale à [statKey] ET
 * que son `bonus` est non nul. L'ordre des bonus est déterministe : peuple d'abord, formation
 * ensuite. Fonction pure (testable, sans dépendance Compose).
 *
 * @param statKey Slug de la stat évaluée (`dexterite`/`intelligence`/`perception`/`social`/`vigueur`).
 * @param base Valeur de base de la stat (ou `null`).
 * @param formation Formation résolue de la fiche (ou `null`).
 * @param peuple Peuple résolu de la fiche (ou `null`).
 */
fun statDisplay(
    statKey: String,
    base: Int?,
    formation: ResolvedFormation?,
    peuple: ResolvedReference?,
): StatDisplay {
    val bonuses = buildList {
        bonusFor(statKey, peuple?.stat, peuple?.bonus)?.let(::add)
        bonusFor(statKey, formation?.stat, formation?.bonus)?.let(::add)
    }
    return StatDisplay(base = base, bonuses = bonuses, total = (base ?: 0) + bonuses.sum())
}

/** Retourne le montant du bonus si la source cible [statKey] avec un bonus non nul, sinon `null`. */
private fun bonusFor(statKey: String, sourceStat: String?, sourceBonus: Int?): Int? =
    if (sourceStat == statKey && sourceBonus != null) sourceBonus else null

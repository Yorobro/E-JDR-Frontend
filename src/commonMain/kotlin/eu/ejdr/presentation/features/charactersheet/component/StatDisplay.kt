package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.ResolvedFormation
import eu.ejdr.domain.features.charactersheet.entities.ResolvedReference

/**
 * Source d'un bonus de caractéristique, avec son libellé d'affichage (français, minuscule).
 *
 * @property label Libellé affiché à côté du montant (ex. « peuple », « formation »).
 */
enum class StatBonusSource(val label: String) {
    PEUPLE("peuple"),
    FORMATION("formation"),
}

/**
 * Un bonus ciblant une caractéristique : sa source et son montant.
 *
 * @property source Origine du bonus (peuple ou formation).
 * @property amount Montant ajouté à la base par cette source.
 */
data class StatBonus(val source: StatBonusSource, val amount: Int)

/**
 * Modèle d'affichage d'une caractéristique : base éditable + détail des sources de bonus + total.
 *
 * La [base] reste la valeur ÉDITABLE de la fiche (jamais altérée par les bonus). Les [bonuses]
 * détaillent chaque source qui cible cette stat (peuple puis formation, dans cet ordre) avec son
 * montant ; ils sont purement d'affichage et RECONSTRUITS côté front depuis les blocs résolus.
 * Le [total] provient du backend (champ `*Totale`) ; un repli local le recalcule si absent.
 *
 * @property base Valeur de base de la stat (telle que saisie), ou `null` si non renseignée.
 * @property bonuses Détail des bonus ciblant cette stat, dans l'ordre peuple → formation.
 * @property total Total de la stat (base + somme des bonus), source de vérité = backend.
 */
data class StatDisplay(
    val base: Int?,
    val bonuses: List<StatBonus>,
    val total: Int,
) {
    /** Y a-t-il au moins un bonus à afficher (sinon : rendu identique à avant) ? */
    val hasBonus: Boolean get() = bonuses.isNotEmpty()
}

/**
 * Calcule l'affichage d'une stat à partir de sa base, du total serveur et des sources résolues.
 *
 * Les deux sources sont **asymétriques**, comme côté serveur : la **formation** apporte au plus un
 * bonus (`stat`/`bonus`), le **peuple** en apporte 0..N — mais **au plus un par statistique**, donc
 * au plus un pour la stat évaluée ici. L'ordre des bonus est déterministe : peuple d'abord,
 * formation ensuite.
 *
 * Le [total] retenu est celui du backend ; à défaut (back muet, ou version antérieure au
 * multi-bonus), il est recalculé localement en `(base ?: 0) + somme(bonus)`. Fonction pure (sans
 * dépendance Compose).
 *
 * @param statKey Slug de la stat évaluée (`dexterite`/`intelligence`/`perception`/`social`/`vigueur`).
 * @param base Valeur de base de la stat (ou `null`).
 * @param total Total calculé renvoyé par le backend (champ `*Totale`), ou `null` si absent.
 * @param formation Formation résolue de la fiche (ou `null`).
 * @param peuple Peuple résolu de la fiche (ou `null`).
 */
fun statDisplay(
    statKey: String,
    base: Int?,
    total: Int?,
    formation: ResolvedFormation?,
    peuple: ResolvedReference?,
): StatDisplay {
    val bonuses = buildList {
        // `firstOrNull` et non `filter` : le backend garantit au plus un bonus de peuple par stat.
        // On n'affichera donc jamais « + 1 peuple + 2 peuple » sur une même caractéristique, même
        // si la donnée dérivait.
        peuple?.statBonuses
            ?.firstOrNull { it.stat == statKey && it.bonus != 0 }
            ?.let { add(StatBonus(StatBonusSource.PEUPLE, it.bonus)) }
        bonusFor(StatBonusSource.FORMATION, statKey, formation?.stat, formation?.bonus)?.let(::add)
    }
    val resolvedTotal = total ?: ((base ?: 0) + bonuses.sumOf { it.amount })
    return StatDisplay(base = base, bonuses = bonuses, total = resolvedTotal)
}

/** Retourne le bonus si la source **mono-bonus** (formation) cible [statKey], sinon `null`. */
private fun bonusFor(
    source: StatBonusSource,
    statKey: String,
    sourceStat: String?,
    sourceBonus: Int?,
): StatBonus? =
    if (sourceStat == statKey && sourceBonus != null) StatBonus(source, sourceBonus) else null

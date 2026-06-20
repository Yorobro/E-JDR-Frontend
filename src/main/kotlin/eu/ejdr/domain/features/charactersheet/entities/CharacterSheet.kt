package eu.ejdr.domain.features.charactersheet.entities

/**
 * Fiche de personnage appartenant à un utilisateur.
 *
 * Conteneur de données pur (domaine front anémique) : représente une fiche telle que reçue
 * du serveur. Aucune logique métier — les règles vivent côté backend. Les champs détaillés
 * (identité, caractéristiques, textes longs) sont optionnels (`null` si non renseignés) et
 * ne sont peuplés que par le détail (`GET /character-sheets/:id`) ; les listes ne portent
 * que les champs de base.
 *
 * @property id Identifiant unique stable de la fiche.
 * @property ownerId Identifiant du propriétaire de la fiche.
 * @property name Nom affiché de la fiche.
 * @property createdAt Date de création au format ISO 8601 (telle que renvoyée par l'API).
 * @property formationId Id de la formation choisie (élément de référence du propriétaire), ou `null`.
 * @property niveau Niveau du personnage (entier).
 * @property peupleId Id du peuple choisi (élément de référence du propriétaire), ou `null`.
 * @property sexe Sexe du personnage (M/F/NB).
 * @property tailleEtPoids Taille et poids du personnage.
 * @property age Âge du personnage (entier).
 * @property apparence Description de l'apparence.
 * @property dexterite Caractéristique de dextérité (base éditable).
 * @property intelligence Caractéristique d'intelligence (base éditable).
 * @property perception Caractéristique de perception (base éditable).
 * @property social Caractéristique sociale (base éditable).
 * @property vigueur Caractéristique de vigueur (base éditable).
 * @property dexteriteTotale Total de dextérité (base + bonus peuple/formation) calculé serveur, ou `null`.
 * @property intelligenceTotale Total d'intelligence calculé serveur, ou `null`.
 * @property perceptionTotale Total de perception calculé serveur, ou `null`.
 * @property socialTotale Total social calculé serveur, ou `null`.
 * @property vigueurTotale Total de vigueur calculé serveur, ou `null`.
 * @property pointsDeVie Points de vie.
 * @property pointsDeMagie Points de magie.
 * @property protection Valeur de protection / armure.
 * @property purse Bourse du personnage (pièces d'or, d'argent et de cuivre).
 * @property notes Notes libres.
 * @property formation Formation **résolue** (nom + stat ciblée + bonus + compétences apportées),
 *   ou `null` si aucune. Bloc dérivé renseigné par le détail ; purement d'affichage (n'altère pas
 *   les stats de base).
 * @property peuple Peuple **résolu** (nom + stat ciblée + bonus), ou `null` si aucun. Bloc dérivé
 *   renseigné par le détail ; purement d'affichage.
 *
 * Note : armes, armures, compétences, équipements, sorts et miracles ne sont **plus** des champs
 * ici — ce sont des relations N‑N gérées à part (cf. feature `reference`).
 */
data class CharacterSheet(
    val id: String,
    val ownerId: String,
    val name: String,
    val createdAt: String,
    val formationId: String? = null,
    val niveau: Int? = null,
    val peupleId: String? = null,
    val sexe: String? = null,
    val tailleEtPoids: String? = null,
    val age: Int? = null,
    val apparence: String? = null,
    val dexterite: Int? = null,
    val intelligence: Int? = null,
    val perception: Int? = null,
    val social: Int? = null,
    val vigueur: Int? = null,
    val dexteriteTotale: Int? = null,
    val intelligenceTotale: Int? = null,
    val perceptionTotale: Int? = null,
    val socialTotale: Int? = null,
    val vigueurTotale: Int? = null,
    val pointsDeVie: Int? = null,
    val pointsDeMagie: Int? = null,
    val protection: Int? = null,
    val purse: Purse? = null,
    val notes: String? = null,
    val formation: ResolvedFormation? = null,
    val peuple: ResolvedReference? = null,
)

/**
 * Élément de référence N‑1 **résolu** rattaché à une fiche (peuple, ou base d'une formation).
 *
 * @property id Identifiant de l'élément de référence.
 * @property name Nom affiché.
 * @property stat Statistique ciblée (slug serveur `dexterite`/.../`vigueur`), ou `null`.
 * @property bonus Montant du bonus appliqué à [stat] (déjà calculé côté serveur), ou `null`.
 */
data class ResolvedReference(
    val id: String,
    val name: String,
    val stat: String? = null,
    val bonus: Int? = null,
)

/**
 * Formation **résolue** rattachée à une fiche : comme [ResolvedReference] mais porte en plus les
 * compétences apportées par la formation (dérivées, affichées en lecture seule).
 *
 * @property id Identifiant de la formation.
 * @property name Nom affiché.
 * @property stat Statistique ciblée (slug serveur), ou `null`.
 * @property bonus Montant du bonus appliqué à [stat], ou `null`.
 * @property competences Compétences apportées par la formation (lecture seule, non retirables).
 */
data class ResolvedFormation(
    val id: String,
    val name: String,
    val stat: String? = null,
    val bonus: Int? = null,
    val competences: List<ResolvedCompetence> = emptyList(),
)

/**
 * Compétence apportée par une formation (dérivée, lecture seule).
 *
 * @property id Identifiant de la compétence.
 * @property name Nom affiché de la compétence.
 */
data class ResolvedCompetence(
    val id: String,
    val name: String,
)

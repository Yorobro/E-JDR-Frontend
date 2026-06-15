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
 * @property formation Formation du personnage.
 * @property niveau Niveau du personnage (entier).
 * @property peuple Peuple du personnage.
 * @property sexe Sexe du personnage (M/F/NB).
 * @property tailleEtPoids Taille et poids du personnage.
 * @property age Âge du personnage (entier).
 * @property apparence Description de l'apparence.
 * @property dexterite Caractéristique de dextérité.
 * @property intelligence Caractéristique d'intelligence.
 * @property perception Caractéristique de perception.
 * @property social Caractéristique sociale.
 * @property vigueur Caractéristique de vigueur.
 * @property pointsDeVie Points de vie.
 * @property pointsDeMagie Points de magie.
 * @property protection Valeur de protection / armure.
 * @property competences Compétences du personnage (texte libre).
 * @property purse Bourse du personnage (pièces d'or, d'argent et de cuivre).
 * @property armes Description des armes.
 * @property armures Description des armures.
 * @property equipement Description de l'équipement.
 * @property sortsEtMiracles Description des sorts et miracles.
 * @property notes Notes libres.
 */
data class CharacterSheet(
    val id: String,
    val ownerId: String,
    val name: String,
    val createdAt: String,
    val formation: String? = null,
    val niveau: Int? = null,
    val peuple: String? = null,
    val sexe: String? = null,
    val tailleEtPoids: String? = null,
    val age: Int? = null,
    val apparence: String? = null,
    val dexterite: Int? = null,
    val intelligence: Int? = null,
    val perception: Int? = null,
    val social: Int? = null,
    val vigueur: Int? = null,
    val pointsDeVie: Int? = null,
    val pointsDeMagie: Int? = null,
    val protection: Int? = null,
    val competences: String? = null,
    val purse: Purse? = null,
    val armes: String? = null,
    val armures: String? = null,
    val equipement: String? = null,
    val sortsEtMiracles: String? = null,
    val notes: String? = null,
)

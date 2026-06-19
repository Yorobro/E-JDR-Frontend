package eu.ejdr.domain.features.reference.entities

/**
 * Catégories d'éléments de référence. Source unique de vérité du mapping
 * **type ↔ slug d'URL ↔ libellé affiché**, partagée par toute la couche front.
 *
 * - [slug] : segment d'URL attendu par l'API (`/reference/{slug}`, `/character-sheets/:id/{slug}`).
 * - [label] : libellé français affiché à l'utilisateur (pluriel, pour les listes/onglets).
 * - [singularLabel] : libellé au singulier (pour les dialogs « Ajouter une … » / champs).
 * - [linkable] : `true` si la catégorie se rattache à une fiche en N‑N (armes/armures/compétences/
 *   équipements) ; `false` pour les catégories N‑1 (formation/peuple), choisies via un dropdown.
 *
 * @property slug Segment d'URL de la catégorie côté API.
 * @property label Libellé pluriel affiché.
 * @property singularLabel Libellé singulier affiché.
 * @property linkable Indique si la catégorie est rattachable en N‑N à une fiche.
 */
enum class ReferenceType(
    val slug: String,
    val label: String,
    val singularLabel: String,
    val linkable: Boolean,
) {
    FORMATION("formations", "Formations", "formation", linkable = false),
    PEUPLE("peoples", "Peuples", "peuple", linkable = false),
    ARME("armes", "Armes", "arme", linkable = true),
    ARMURE("armures", "Armures", "armure", linkable = true),
    // Les compétences sont 100 % dérivées de la formation (lecture seule) : plus de liaison N‑N à
    // la fiche. Le catalogue reste créable/supprimable via le hub ; seul le rattachement disparaît.
    COMPETENCE("competences", "Compétences", "compétence", linkable = false),
    EQUIPEMENT("equipements", "Équipements", "équipement", linkable = true);

    companion object {
        /** Retrouve un type par son [slug] d'URL, ou `null` si inconnu. */
        fun fromSlug(slug: String): ReferenceType? = entries.firstOrNull { it.slug == slug }
    }
}

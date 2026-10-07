package eu.ejdr.domain.features.reference.entities

/**
 * Bonus de caractéristique porté par un **peuple**.
 *
 * Un peuple en porte 0..N, avec **au plus un par statistique** — règle garantie par le backend, qui
 * refuse un doublon en 400. Une **formation**, elle, reste mono-bonus et expose `stat`/`bonus`
 * directement sur [ReferenceItem] : le contrat est volontairement asymétrique.
 *
 * ⚠️ À ne pas confondre avec `eu.ejdr.presentation.features.charactersheet.component.StatBonus`,
 * qui est le modèle d'**affichage** d'une ligne de bonus sur la fiche (avec sa source).
 *
 * @property stat Slug serveur (`dexterite`/`intelligence`/`perception`/`social`/`vigueur`).
 * @property bonus Montant ajouté à [stat].
 */
data class ReferenceStatBonus(
    val stat: String,
    val bonus: Int,
)

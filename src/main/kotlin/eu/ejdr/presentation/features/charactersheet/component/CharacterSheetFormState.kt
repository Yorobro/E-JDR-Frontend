package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet

/**
 * État éditable d'une fiche, tenu en mémoire par l'écran détail pendant l'édition.
 *
 * Chaque champ est un `String` mutable (les nombres sont gérés en texte pour préserver les
 * états de frappe intermédiaires et le champ vide). Construit à partir d'une [CharacterSheet]
 * et reproductible en [CharacterSheet] via [toCharacterSheet]. Hors design system : simple
 * conteneur d'état observable par Compose.
 *
 * @param source Fiche initiale dont les valeurs amorcent les champs.
 */
class CharacterSheetFormState(source: CharacterSheet) {
    private val id = source.id
    private val ownerId = source.ownerId
    private val createdAt = source.createdAt

    var name by mutableStateOf(source.name)
    var formation by mutableStateOf(source.formation.orEmpty())
    var niveau by mutableStateOf(source.niveau.orEmpty())
    var peuple by mutableStateOf(source.peuple.orEmpty())
    var sexe by mutableStateOf(source.sexe.orEmpty())
    var tailleEtPoids by mutableStateOf(source.tailleEtPoids.orEmpty())
    var age by mutableStateOf(source.age.orEmpty())
    var apparence by mutableStateOf(source.apparence.orEmpty())
    var dexterite by mutableStateOf(source.dexterite.toFieldText())
    var intelligence by mutableStateOf(source.intelligence.toFieldText())
    var perception by mutableStateOf(source.perception.toFieldText())
    var social by mutableStateOf(source.social.toFieldText())
    var vigueur by mutableStateOf(source.vigueur.toFieldText())
    var pointsDeVie by mutableStateOf(source.pointsDeVie.toFieldText())
    var pointsDeMagie by mutableStateOf(source.pointsDeMagie.toFieldText())
    var protection by mutableStateOf(source.protection.toFieldText())
    var monnaie by mutableStateOf(source.monnaie.toFieldText())
    var armes by mutableStateOf(source.armes.orEmpty())
    var armures by mutableStateOf(source.armures.orEmpty())
    var equipement by mutableStateOf(source.equipement.orEmpty())
    var sortsEtMiracles by mutableStateOf(source.sortsEtMiracles.orEmpty())
    var notes by mutableStateOf(source.notes.orEmpty())

    /** Le nom est-il valide (non vide après trim) ? Sert à (dés)activer le bouton Enregistrer. */
    val isNameValid: Boolean get() = name.isNotBlank()

    /** Reconstruit une [CharacterSheet] à partir des champs édités (texte vide → `null`). */
    fun toCharacterSheet(): CharacterSheet = CharacterSheet(
        id = id,
        ownerId = ownerId,
        name = name.trim(),
        createdAt = createdAt,
        formation = formation.toNullableText(),
        niveau = niveau.toNullableText(),
        peuple = peuple.toNullableText(),
        sexe = sexe.toNullableText(),
        tailleEtPoids = tailleEtPoids.toNullableText(),
        age = age.toNullableText(),
        apparence = apparence.toNullableText(),
        dexterite = dexterite.toNullableInt(),
        intelligence = intelligence.toNullableInt(),
        perception = perception.toNullableInt(),
        social = social.toNullableInt(),
        vigueur = vigueur.toNullableInt(),
        pointsDeVie = pointsDeVie.toNullableInt(),
        pointsDeMagie = pointsDeMagie.toNullableInt(),
        protection = protection.toNullableInt(),
        monnaie = monnaie.toNullableInt(),
        armes = armes.toNullableText(),
        armures = armures.toNullableText(),
        equipement = equipement.toNullableText(),
        sortsEtMiracles = sortsEtMiracles.toNullableText(),
        notes = notes.toNullableText(),
    )
}

/** Représente un entier optionnel en texte de champ (`null` → ""). */
private fun Int?.toFieldText(): String = this?.toString() ?: ""

/** Texte de champ → `String?` (vide après trim → `null`). */
private fun String.toNullableText(): String? = trim().ifEmpty { null }

/** Texte de champ → `Int?` (vide ou non numérique → `null`). */
private fun String.toNullableInt(): Int? = trim().toIntOrNull()

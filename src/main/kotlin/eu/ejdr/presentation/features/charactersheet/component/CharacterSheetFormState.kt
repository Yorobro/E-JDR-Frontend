package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.domain.features.charactersheet.entities.Purse

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
    // formationId / peupleId : id de l'élément de référence sélectionné (ou "" si aucun),
    // pilotés par les dropdowns. La résolution id→nom se fait dans la page à partir des catalogues.
    var formationId by mutableStateOf(source.formationId.orEmpty())
    var niveau by mutableStateOf(source.niveau.toFieldText())
    var peupleId by mutableStateOf(source.peupleId.orEmpty())
    var sexe by mutableStateOf(source.sexe.orEmpty())
    var tailleEtPoids by mutableStateOf(source.tailleEtPoids.orEmpty())
    var age by mutableStateOf(source.age.toFieldText())
    var apparence by mutableStateOf(source.apparence.orEmpty())
    var dexterite by mutableStateOf(source.dexterite.toFieldText())
    var intelligence by mutableStateOf(source.intelligence.toFieldText())
    var perception by mutableStateOf(source.perception.toFieldText())
    var social by mutableStateOf(source.social.toFieldText())
    var vigueur by mutableStateOf(source.vigueur.toFieldText())
    // pointsDeVie / protection ne sont PAS édités : calculés et renvoyés par le serveur
    // (PV = 10 + vigueur totale ; protection = somme des armures liées), affichés en lecture seule.
    var pointsDeMagie by mutableStateOf(source.pointsDeMagie.toFieldText())
    var purseGold by mutableStateOf(source.purse?.gold.toFieldText())
    var purseSilver by mutableStateOf(source.purse?.silver.toFieldText())
    var purseCopper by mutableStateOf(source.purse?.copper.toFieldText())
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
        formationId = formationId.toNullableText(),
        niveau = niveau.toNullableInt(),
        peupleId = peupleId.toNullableText(),
        sexe = sexe.toNullableText(),
        tailleEtPoids = tailleEtPoids.toNullableText(),
        age = age.toNullableInt(),
        apparence = apparence.toNullableText(),
        dexterite = dexterite.toNullableInt(),
        intelligence = intelligence.toNullableInt(),
        perception = perception.toNullableInt(),
        social = social.toNullableInt(),
        vigueur = vigueur.toNullableInt(),
        // pointsDeVie / protection : dérivés serveur, jamais réécrits depuis le formulaire (restent null).
        pointsDeMagie = pointsDeMagie.toNullableInt(),
        purse = buildPurse(),
        sortsEtMiracles = sortsEtMiracles.toNullableText(),
        notes = notes.toNullableText(),
    )

    /** Construit la bourse depuis les 3 champs ; null si les 3 sont vides. */
    private fun buildPurse(): Purse? {
        val g = purseGold.toNullableInt()
        val s = purseSilver.toNullableInt()
        val c = purseCopper.toNullableInt()
        return if (g == null && s == null && c == null) null
        else Purse(gold = g ?: 0, silver = s ?: 0, copper = c ?: 0)
    }
}

/** Représente un entier optionnel en texte de champ (`null` → ""). */
private fun Int?.toFieldText(): String = this?.toString() ?: ""

/** Texte de champ → `String?` (vide après trim → `null`). */
private fun String.toNullableText(): String? = trim().ifEmpty { null }

/** Texte de champ → `Int?` (vide ou non numérique → `null`). */
private fun String.toNullableInt(): Int? = trim().toIntOrNull()

package eu.ejdr.presentation.shared.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * State-holder réutilisable pour les formulaires de l'interface.
 *
 * ## Pourquoi ce pattern ?
 *
 * Par défaut, l'état d'un formulaire (champs, erreur, chargement) est dispersé en plusieurs
 * `remember { mutableStateOf(...) }` directement dans le composable de la page. C'est suffisant
 * pour les formulaires simples (login, register), mais ça devient difficile à maintenir quand les
 * formulaires grossissent (feuille de personnage, creation de campagne).
 *
 * Ce pattern "state-holder" centralise l'état dans un objet Kotlin ordinaire (ni ViewModel, ni
 * Compose-aware) et l'instancie via un `remember` dans un helper `rememberFormState`. L'approche
 * est la même que celle des `ScrollState`, `FocusRequester`, etc. dans Compose.
 *
 * ## Modèle à suivre pour les écrans futurs
 *
 * ```kotlin
 * // 1. Définir le state-holder pour l'écran :
 * class CharacterSheetState {
 *     var name by mutableStateOf("")
 *     var hp   by mutableStateOf(0)
 *     var error by mutableStateOf<String?>(null)
 *     var loading by mutableStateOf(false)
 *
 *     fun reset() { name = ""; hp = 0; error = null; loading = false }
 * }
 *
 * // 2. Fournir un helper remember (évite de répéter le remember dans chaque composable) :
 * @Composable
 * fun rememberCharacterSheetState(): CharacterSheetState = remember { CharacterSheetState() }
 *
 * // 3. Utiliser dans la page :
 * @Composable
 * fun CharacterSheetPage(...) {
 *     val state = rememberCharacterSheetState()
 *     CharacterSheetForm(
 *         name    = state.name,
 *         hp      = state.hp,
 *         error   = state.error,
 *         loading = state.loading,
 *         onNameChange = { state.name = it; state.error = null },
 *         onHpChange   = { state.hp   = it },
 *         onSubmit     = { /* appel use case, state.loading = true … */ },
 *     )
 * }
 * ```
 *
 * ## Ce fichier : FormState générique minimal
 *
 * [FormState] est un state-holder *générique* couvrant le cas le plus courant : un ensemble de
 * champs, un message d'erreur, et un indicateur de chargement. Il est utilisé tel quel pour les
 * formulaires simples (cf. [rememberFormState]). Pour les formulaires riches, il sert de modèle
 * pour définir un state-holder spécifique (comme `CharacterSheetState` ci-dessus).
 *
 * @param T Type représentant les valeurs des champs du formulaire.
 * @property fields  Valeurs courantes des champs.
 * @property error   Message d'erreur à afficher, ou `null` si aucune erreur.
 * @property loading Indique qu'un appel asynchrone (use case) est en cours.
 */
class FormState<T>(initial: T) {
    /** Valeurs courantes des champs du formulaire. */
    var fields by mutableStateOf(initial)

    /** Message d'erreur à afficher, ou `null` si aucune erreur. */
    var error by mutableStateOf<String?>(null)

    /** `true` pendant la durée d'un appel asynchrone (use case). */
    var loading by mutableStateOf(false)

    /** Réinitialise l'erreur et remet les champs à leur valeur d'origine. */
    fun reset(initial: T) {
        fields = initial
        error = null
        loading = false
    }
}

/**
 * Crée et mémorise un [FormState] avec la valeur initiale [initial].
 *
 * Usage :
 * ```kotlin
 * data class LoginFields(val email: String = "", val password: String = "")
 *
 * @Composable
 * fun LoginPage(...) {
 *     val state = rememberFormState(LoginFields())
 *     // state.fields.email, state.fields.password, state.error, state.loading
 * }
 * ```
 *
 * @param T     Type représentant les valeurs des champs.
 * @param initial Valeur initiale des champs.
 */
@Composable
fun <T> rememberFormState(initial: T): FormState<T> = remember { FormState(initial) }

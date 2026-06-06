package eu.ejdr.application.common

import eu.ejdr.domain.error.DomainError

/**
 * Résultat d'un use case selon le style railway-oriented : soit un succès, soit
 * un échec métier typé.
 *
 * Permet de remonter les erreurs comme des valeurs : aucune exception ne traverse
 * la frontière vers la couche présentation. L'échec est toujours un [DomainError],
 * ce qui force chaque feature à modéliser ses erreurs explicitement.
 *
 * @param T Type de la valeur produite en cas de succès.
 * @param E Type de l'erreur métier en cas d'échec.
 */
sealed interface Result<out T, out E : DomainError> {
    /**
     * Issue favorable portant la valeur produite.
     *
     * @property value Valeur calculée par le use case.
     */
    data class Success<out T>(val value: T) : Result<T, Nothing>

    /**
     * Issue défavorable portant l'erreur métier rencontrée.
     *
     * @property error Erreur de domaine décrivant la cause de l'échec.
     */
    data class Failure<out E : DomainError>(val error: E) : Result<Nothing, E>
}

/**
 * Réduit un [Result] vers une valeur unique en traitant les deux issues.
 *
 * Permet de consommer un résultat sans `when` explicite, typiquement pour mapper
 * succès et échec vers un même type (ex. un état d'UI).
 *
 * @param onSuccess Transformation appliquée à la valeur en cas de succès.
 * @param onFailure Transformation appliquée à l'erreur en cas d'échec.
 * @return La valeur produite par la branche correspondant à l'issue.
 */
inline fun <T, E : DomainError, R> Result<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R,
): R = when (this) {
    is Result.Success -> onSuccess(value)
    is Result.Failure -> onFailure(error)
}

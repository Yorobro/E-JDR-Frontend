package eu.ejdr.application.shared

import eu.ejdr.domain.shared.error.DomainError

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

/**
 * Transforme la valeur de succès via [transform] ; un échec est propagé tel quel.
 */
inline fun <T, E : DomainError, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Failure -> this
    }

/**
 * Transforme l'erreur d'échec via [transform] ; un succès est propagé tel quel.
 */
inline fun <T, E : DomainError, F : DomainError> Result<T, E>.mapError(
    transform: (E) -> F,
): Result<T, F> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Failure(transform(error))
}

/**
 * Enchaîne une opération produisant elle-même un [Result] ; court-circuite sur échec.
 */
inline fun <T, E : DomainError, R> Result<T, E>.flatMap(
    transform: (T) -> Result<R, E>,
): Result<R, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> this
}

/** Renvoie la valeur de succès, ou `null` en cas d'échec. */
fun <T, E : DomainError> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    is Result.Failure -> null
}

/** Renvoie la valeur de succès, ou la valeur de repli calculée à partir de l'erreur. */
inline fun <T, E : DomainError> Result<T, E>.getOrElse(onFailure: (E) -> T): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> onFailure(error)
}

/** Exécute [action] avec la valeur en cas de succès, puis renvoie le résultat inchangé. */
inline fun <T, E : DomainError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(value)
    return this
}

/** Exécute [action] avec l'erreur en cas d'échec, puis renvoie le résultat inchangé. */
inline fun <T, E : DomainError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Failure) action(error)
    return this
}

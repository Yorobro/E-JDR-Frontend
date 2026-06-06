package eu.ejdr.application.common

import eu.ejdr.domain.error.DomainError

sealed interface Result<out T, out E : DomainError> {
    data class Success<out T>(val value: T) : Result<T, Nothing>
    data class Failure<out E : DomainError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : DomainError, R> Result<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R,
): R = when (this) {
    is Result.Success -> onSuccess(value)
    is Result.Failure -> onFailure(error)
}

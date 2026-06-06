package eu.ejdr.infrastructure.http.auth

import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import eu.ejdr.infrastructure.http.auth.dto.AuthResponseDto
import io.ktor.http.HttpStatusCode

class AuthHttpMapper {

    fun toUser(dto: AuthResponseDto): User = User(id = dto.userId, email = dto.email)

    fun toAuthError(status: HttpStatusCode, code: String?, message: String?): AuthError =
        when (status) {
            HttpStatusCode.Unauthorized -> AuthError.InvalidCredentials
            HttpStatusCode.Conflict -> AuthError.EmailAlreadyUsed
            HttpStatusCode.Forbidden -> AuthError.SessionExpired
            else -> AuthError.Unknown(message ?: code ?: status.description)
        }
}

package eu.ejdr.infrastructure.http.auth

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.Credentials
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.auth.dto.ApiErrorDto
import eu.ejdr.infrastructure.http.auth.dto.AuthRequestDto
import eu.ejdr.infrastructure.http.auth.dto.AuthResponseDto
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthHttpRepository(
    private val client: HttpClient,
    private val config: AppConfig,
    private val mapper: AuthHttpMapper,
    private val cookiesStorage: SecureCookiesStorage,
) : AuthRepository {

    override suspend fun login(credentials: Credentials): Result<User, AuthError> =
        authenticate("/auth/login", credentials)

    override suspend fun register(credentials: Credentials): Result<User, AuthError> =
        authenticate("/auth/register", credentials)

    private suspend fun authenticate(path: String, credentials: Credentials): Result<User, AuthError> =
        runCatching {
            val response: HttpResponse = client.post("${config.baseUrl}$path") {
                contentType(ContentType.Application.Json)
                setBody(AuthRequestDto(credentials.email, credentials.password))
            }
            if (response.status.isSuccess()) {
                Result.Success(mapper.toUser(response.body<AuthResponseDto>()))
            } else {
                val err = runCatching { response.body<ApiErrorDto>() }.getOrNull()
                Result.Failure(mapper.toAuthError(response.status, err?.code, err?.message))
            }
        }.getOrElse { Result.Failure(AuthError.Network) }

    override suspend fun refresh(): Result<Unit, AuthError> =
        runCatching {
            val response = client.post("${config.baseUrl}/auth/refresh")
            if (response.status.isSuccess()) {
                Result.Success(Unit)
            } else {
                cookiesStorage.clearPersisted()
                Result.Failure(AuthError.SessionExpired)
            }
        }.getOrElse { Result.Failure(AuthError.Network) }

    override suspend fun logout(): Result<Unit, AuthError> =
        runCatching {
            client.post("${config.baseUrl}/auth/logout")
            cookiesStorage.clearPersisted()
            Result.Success(Unit)
        }.getOrElse {
            cookiesStorage.clearPersisted()
            Result.Success(Unit)
        }

    override fun hasPersistedSession(): Boolean = cookiesStorage.hasPersistedSession()
}

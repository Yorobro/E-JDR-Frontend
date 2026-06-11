package eu.ejdr.infrastructure.http.features.auth

import eu.ejdr.domain.features.auth.error.AuthError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthHttpMapperTest {

    private val mapper = AuthHttpMapper

    @Test
    fun `401 maps to InvalidCredentials`() {
        val error = mapper.toAuthError(HttpStatusCode.Unauthorized, code = "INVALID_CREDENTIALS", message = null)
        assertEquals(AuthError.InvalidCredentials, error)
    }

    @Test
    fun `409 maps to EmailAlreadyUsed`() {
        val error = mapper.toAuthError(HttpStatusCode.Conflict, code = "EMAIL_ALREADY_USED", message = null)
        assertEquals(AuthError.EmailAlreadyUsed, error)
    }

    @Test
    fun `unknown status maps to Unknown with message`() {
        val error = mapper.toAuthError(HttpStatusCode.InternalServerError, code = null, message = "boom")
        assertEquals("Erreur inattendue: boom", error.message)
    }

    @Test
    fun `INVALID_REFRESH_TOKEN code on 401 maps to SessionExpired`() {
        // Contrat partagé : le backend renvoie 401 + ce code pour un refresh à renouveler ;
        // le code prime sur le statut (un 401 nu resterait InvalidCredentials).
        val error = mapper.toAuthError(HttpStatusCode.Unauthorized, code = "INVALID_REFRESH_TOKEN", message = null)
        assertEquals(AuthError.SessionExpired, error)
    }

    @Test
    fun `ACCOUNT_LOCKED code maps to AccountLocked`() {
        val error = mapper.toAuthError(HttpStatusCode.TooManyRequests, code = "ACCOUNT_LOCKED", message = null)
        assertEquals(AuthError.AccountLocked, error)
    }
}

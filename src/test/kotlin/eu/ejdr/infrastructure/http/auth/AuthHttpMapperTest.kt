package eu.ejdr.infrastructure.http.auth

import eu.ejdr.domain.error.entities.auth.AuthError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthHttpMapperTest {

    private val mapper = AuthHttpMapper()

    @Test
    fun `401 maps to InvalidCredentials`() {
        val error = mapper.toAuthError(HttpStatusCode.Unauthorized, code = "INVALID_CREDENTIALS", message = null)
        assertEquals(AuthError.InvalidCredentials, error)
    }

    @Test
    fun `409 maps to EmailAlreadyUsed`() {
        val error = mapper.toAuthError(HttpStatusCode.Conflict, code = "EMAIL_TAKEN", message = null)
        assertEquals(AuthError.EmailAlreadyUsed, error)
    }

    @Test
    fun `unknown status maps to Unknown with message`() {
        val error = mapper.toAuthError(HttpStatusCode.InternalServerError, code = null, message = "boom")
        assertEquals("Erreur inattendue: boom", error.message)
    }
}

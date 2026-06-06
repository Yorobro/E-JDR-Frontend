package eu.ejdr.infrastructure.http.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestDto(val email: String, val password: String)

@Serializable
data class AuthResponseDto(val userId: String, val email: String)

@Serializable
data class ApiErrorDto(val code: String? = null, val message: String? = null)

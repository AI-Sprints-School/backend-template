package com.learning.domain.models

import kotlinx.serialization.Serializable

// Request models
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class PasswordResetRequestRequest(
    val email: String
)

@Serializable
data class PasswordResetRequest(
    val token: String,
    val newPassword: String
)

// Response models
@Serializable
data class RegisterResponse(
    val message: String,
    val userId: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePhoto: String? = null,
    val emailVerified: Boolean = false
)
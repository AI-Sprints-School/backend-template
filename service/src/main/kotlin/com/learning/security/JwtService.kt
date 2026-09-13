package com.learning.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.learning.config.EnvironmentConfig
import com.learning.domain.models.Roles
import java.util.Date
import java.util.UUID

/**
 * Настройки JWT. В приложении читаются из секции `jwt` файла application.yaml,
 * по умолчанию — из переменных окружения.
 */
data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlSeconds: Long,
) {
    companion object {
        fun fromEnvironment() = JwtSettings(
            secret = EnvironmentConfig.jwtSecret,
            issuer = EnvironmentConfig.jwtIssuer,
            audience = EnvironmentConfig.jwtAudience,
            realm = "Backend Starter Course",
            accessTokenTtlSeconds = EnvironmentConfig.jwtAccessTokenExpiration,
            refreshTokenTtlSeconds = EnvironmentConfig.jwtRefreshTokenExpiration,
        )
    }
}

/**
 * Выпуск и разбор JWT.
 *
 * Один механизм на всё приложение: токены подписывает и проверяет библиотека
 * `com.auth0:java-jwt` — та же, на которой работает плагин `ktor-server-auth-jwt`.
 * Плагин аутентификации берёт у этого сервиса [verifier], поэтому подпись,
 * издатель и аудитория проверяются одинаково и в маршрутах, и в [AuthService].
 */
class JwtService(private val settings: JwtSettings = JwtSettings.fromEnvironment()) {

    private val algorithm: Algorithm = Algorithm.HMAC256(settings.secret)

    val realm: String get() = settings.realm

    val refreshTokenTtlSeconds: Long get() = settings.refreshTokenTtlSeconds

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(settings.issuer)
        .withAudience(settings.audience)
        .build()

    fun generateAccessToken(userId: String, email: String, role: String = Roles.STUDENT): String = TODO("Глава 5, урок 20: JwtService.generateAccessToken")

    fun generateRefreshToken(userId: String): String = TODO("Глава 5, урок 20: JwtService.generateRefreshToken")

    fun validateToken(token: String): Boolean = TODO("Глава 5, урок 20: JwtService.validateToken")

    /** Проверенный токен или null, если подпись, срок, издатель или формат не сходятся. */
    fun getClaims(token: String): DecodedJWT? = TODO("Глава 5, урок 20: JwtService.getClaims")

    fun getUserId(token: String): String? = TODO("Глава 5, урок 20: JwtService.getUserId")

    fun getEmail(token: String): String? = TODO("Глава 5, урок 20: JwtService.getEmail")

    fun getTokenType(token: String): String? = TODO("Глава 5, урок 20: JwtService.getTokenType")

    fun getRole(token: String): String? = TODO("Глава 5, урок 20: JwtService.getRole")

    fun isTokenExpired(token: String): Boolean {
        TODO("Глава 5, урок 20: JwtService.isTokenExpired")
    }

    private fun newToken(userId: String, type: String, ttlSeconds: Long): JWTCreator.Builder = TODO("Глава 5, урок 20: JwtService.newToken")

    companion object {
        const val CLAIM_TYPE = "type"
        const val CLAIM_EMAIL = "email"
        const val CLAIM_ROLE = "role"
        const val TYPE_ACCESS = "access"
        const val TYPE_REFRESH = "refresh"
    }
}

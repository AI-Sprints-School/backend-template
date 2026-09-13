package com.learning

import com.learning.domain.models.Roles
import com.learning.domain.models.User
import com.learning.repositories.UserRepository
import com.learning.security.JwtService
import com.learning.security.JwtSettings
import io.ktor.server.config.ApplicationConfig
import java.util.*

/**
 * Пользователи для HTTP-тестов: создаются прямо в базе, токен выпускается тем же
 * [JwtService] с настройками из application.yaml, что и у приложения.
 *
 * Регистрация через HTTP здесь не используется намеренно: у маршрутов `/auth`
 * лимит 5 запросов в минуту, и тест на роли упирался бы в него, а не в проверку.
 */
object TestUsers {

    private val jwt: JwtService by lazy {
        val config = ApplicationConfig("application.yaml")
        fun value(path: String) = config.property(path).getString()
        JwtService(
            JwtSettings(
                secret = value("jwt.secret"),
                issuer = value("jwt.issuer"),
                audience = value("jwt.audience"),
                realm = value("jwt.realm"),
                accessTokenTtlSeconds = value("jwt.accessTokenExpiration").toLong(),
                refreshTokenTtlSeconds = value("jwt.refreshTokenExpiration").toLong(),
            )
        )
    }

    data class Authorized(val user: User, val accessToken: String) {
        val id: String get() = user.id.toString()
        val bearer: String get() = "Bearer $accessToken"
    }

    fun student(email: String = uniqueEmail("student")) = create(email, Roles.STUDENT)

    fun admin(email: String = uniqueEmail("admin")) = create(email, Roles.ADMIN)

    private fun create(email: String, role: String): Authorized {
        val users = UserRepository()
        val created = users.createUser(email, "not-a-real-hash", "Тест", "Пользователь")
            ?: error("Не удалось создать пользователя $email")
        if (role != Roles.STUDENT) check(users.updateRole(created.id, role))
        val user = users.findById(created.id)!!
        return Authorized(user, jwt.generateAccessToken(user.id.toString(), user.email, user.role))
    }

    private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}

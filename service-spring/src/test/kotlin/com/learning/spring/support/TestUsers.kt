package com.learning.spring.support

import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Пользователи для HTTP-тестов: создаются прямо в базе, токен подписывается HS256 тем же
 * секретом, издателем и аудиторией, что студент задал в `jwt.*` (`application.yaml` и
 * профиль `test`). Claims — как у Ktor-версии: `sub`, `email`, `role`, `type`, `iss`, `aud`, `exp`.
 *
 * Регистрация через HTTP здесь не используется намеренно: тесты ролей проверяют роли,
 * а не регистрацию. Готово в шаблоне.
 */
class TestUsers(private val jdbc: JdbcTemplate, private val environment: Environment) {

    data class Authorized(val id: UUID, val email: String, val token: String)

    fun student(email: String = uniqueEmail("student")) = create(email, "student")

    fun admin(email: String = uniqueEmail("admin")) = create(email, "admin")

    private fun create(email: String, role: String): Authorized {
        val id = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO users (id, email, password_hash, first_name, last_name, role) VALUES (?, ?, 'not-a-real-hash', 'Тест', 'Пользователь', ?)",
            id, email, role,
        )
        return Authorized(id, email, accessToken(id, email, role))
    }

    /** Токен с заданным сроком: `ttlSeconds` < 0 даёт уже просроченный. */
    fun accessToken(id: UUID, email: String, role: String, ttlSeconds: Long = 3600, secret: String = jwtProperty("secret")): String {
        val now = System.currentTimeMillis() / 1000
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val claims = buildString {
            append("{")
            append(""""sub":"$id","email":"$email","role":"$role","type":"access",""")
            append(""""iss":"${jwtProperty("issuer")}","aud":"${jwtProperty("audience")}",""")
            append(""""iat":$now,"exp":${now + ttlSeconds}}""")
        }
        val signingInput = "${base64Url(header.toByteArray())}.${base64Url(claims.toByteArray())}"
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")) }
        return "$signingInput.${base64Url(mac.doFinal(signingInput.toByteArray()))}"
    }

    fun jwtProperty(name: String): String =
        environment.getProperty("jwt.$name") ?: error("Урок 3: в настройках нет jwt.$name (application.yaml / application-test.yaml)")

    private fun base64Url(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}

package com.learning.spring

import com.learning.spring.auth.ExternalIdentity
import com.learning.spring.auth.ExternalLoginService
import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.net.URLDecoder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Урок 15: вход через Keycloak. Keycloak в тестах не запущен: редирект на провайдера строит сам
 * Spring (регистрация клиента явными адресами провайдера), а привязку учётки тест проверяет вызовом
 * `ExternalLoginService` — так же, как его вызывает обработчик успешного входа.
 */
@Tag("core")
@Tag("chapter3")
class OAuthLoginTest : IntegrationTest() {

    @Autowired
    private lateinit var externalLogin: ExternalLoginService

    @Test
    fun `login redirects to keycloak with state and pkce`() {
        val response = http.get("/oauth2/authorization/keycloak")

        assertEquals(302, response.status, "Ожидался редирект на провайдера (клиент keycloak в application.yaml, oauth2Login): ${response.body}")
        val location = URI(response.header("Location").single())
        assertEquals("http://localhost:8180/realms/learning/protocol/openid-connect/auth", "${location.scheme}://${location.authority}${location.path}")
        val query = location.rawQuery.split('&').associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }
        assertEquals("service-spring", query["client_id"])
        assertEquals("code", query["response_type"])
        // Адрес возврата — тот, что зарегистрирован у клиента в realm (на 8081 он совпадает с {baseUrl})
        assertTrue(query["redirect_uri"].orEmpty().endsWith("/login/oauth2/code/keycloak"), "redirect_uri: ${query["redirect_uri"]}")
        assertTrue(query["scope"].orEmpty().split(' ').contains("openid"), "scope без openid — это не OpenID Connect: ${query["scope"]}")
        assertTrue(!query["state"].isNullOrBlank(), "В редиректе нет state")
        assertTrue(!query["code_challenge"].isNullOrBlank(), "В редиректе нет code_challenge (PKCE)")
        assertEquals("S256", query["code_challenge_method"])
    }

    @Test
    fun `only the login chain keeps a session`() {
        val login = http.get("/oauth2/authorization/keycloak")
        assertTrue(login.header("Set-Cookie").any { it.startsWith("JSESSIONID=") }, "Цепочка входа хранит запрос авторизации в сессии: ${login.headers}")

        val anonymous = http.get("/api/v1/auth/me")
        assertEquals(401, anonymous.status, "API без токена — 401 JSON, а не редирект на вход: ${anonymous.status}")
        assertEquals(emptyList(), anonymous.header("Set-Cookie"), "API без состояния: ответ без Set-Cookie")

        val student = users.student()
        val me = http.get("/api/v1/auth/me", student.token)
        assertEquals(200, me.status, me.body)
        assertEquals(emptyList(), me.header("Set-Cookie"), "API без состояния: ответ без Set-Cookie")
    }

    @Test
    fun `new external user is created as student without password`() {
        val identity = identity()

        val auth = externalLogin.login(identity)

        val row = jdbc.queryForMap("SELECT id, role, password_hash, auth_provider, external_subject FROM users WHERE email = ?", identity.email)
        assertEquals("student", row["role"])
        assertNull(row["password_hash"], "У пользователя провайдера пароля нет")
        assertEquals("KEYCLOAK", row["auth_provider"])
        assertEquals(identity.subject, row["external_subject"])
        assertEquals(row["id"].toString(), auth.user.id)
        val me = http.get("/api/v1/auth/me", auth.accessToken)
        assertEquals(200, me.status, "Токен после входа через провайдера — тот же JWT урока 12: ${me.body}")
        assertEquals(identity.email, me.json["email"].asString())
    }

    @Test
    fun `repeated login finds the same user by provider and subject`() {
        val identity = identity()
        val first = externalLogin.login(identity)

        // Почта у провайдера сменилась — sub тот же, пользователь тот же
        val second = externalLogin.login(identity.copy(email = "renamed-${UUID.randomUUID()}@example.com"))

        assertEquals(first.user.id, second.user.id)
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM users WHERE external_subject = ?", Int::class.java, identity.subject))
    }

    @Test
    fun `unverified email is not linked to an existing user`() {
        val local = users.student()

        try {
            externalLogin.login(identity(email = local.email, emailVerified = false))
            fail("Почта не подтверждена провайдером — привязка к существующему пользователю запрещена, ожидалось исключение")
        } catch (_: Exception) {
            // отказ — то, что нужно; TODO заглушки (NotImplementedError) сюда не попадает и роняет тест
        }

        val row = jdbc.queryForMap("SELECT auth_provider, external_subject FROM users WHERE id = ?", local.id)
        assertEquals("LOCAL", row["auth_provider"])
        assertNull(row["external_subject"], "Чужая учётка провайдера не должна попасть в аккаунт сервиса")
    }

    @Test
    fun `verified email links the external account to the existing user`() {
        val local = users.student()
        val identity = identity(email = local.email, emailVerified = true)

        val auth = externalLogin.login(identity)

        assertEquals(local.id.toString(), auth.user.id)
        val row = jdbc.queryForMap("SELECT auth_provider, external_subject, password_hash FROM users WHERE id = ?", local.id)
        assertEquals("KEYCLOAK", row["auth_provider"])
        assertEquals(identity.subject, row["external_subject"])
        assertNull(row["password_hash"], "Пароль, заданный до привязки, после привязки не работает")
    }

    private fun identity(email: String = "kc-${UUID.randomUUID()}@example.com", emailVerified: Boolean = true) = ExternalIdentity(
        provider = "KEYCLOAK",
        subject = UUID.randomUUID().toString(),
        email = email,
        emailVerified = emailVerified,
        firstName = "Внешний",
        lastName = "Пользователь",
    )
}

package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Уроки 12–13: регистрация, вход, `/auth/me`; токен — выпуск `JwtEncoder`, проверка ресурс-сервером. */
@Tag("chapter3")
class AuthFlowTest : IntegrationTest() {

    private val password = "SecurePass123"

    private fun email() = "user-${UUID.randomUUID()}@example.com"

    private fun registerBody(email: String) = """{"email":"$email","password":"$password","firstName":"Иван","lastName":"Иванов"}"""

    @Test
    @Tag("core")
    fun `register returns 201 with tokens and user`() {
        val email = email()

        val response = http.post("/api/v1/auth/register", registerBody(email))

        assertEquals(201, response.status, response.body)
        assertTrue(response.json["accessToken"].asString().isNotBlank())
        assertEquals(email, response.json["user"]["email"].asString())
    }

    @Test
    @Tag("core")
    fun `register with taken email returns 409`() {
        val email = email()
        http.post("/api/v1/auth/register", registerBody(email))

        val response = http.post("/api/v1/auth/register", registerBody(email))

        assertEquals(409, response.status, response.body)
        assertEquals("CONFLICT", response.json["error"].asString())
    }

    @Test
    @Tag("core")
    fun `login returns token that opens auth me`() {
        val email = email()
        http.post("/api/v1/auth/register", registerBody(email))

        val login = http.post("/api/v1/auth/login", """{"email":"$email","password":"$password"}""")
        assertEquals(200, login.status, login.body)
        val me = http.get("/api/v1/auth/me", login.json["accessToken"].asString())

        assertEquals(200, me.status, me.body)
        assertEquals(email, me.json["email"].asString())
    }

    @Test
    @Tag("core")
    fun `login with wrong password returns 401`() {
        val email = email()
        http.post("/api/v1/auth/register", registerBody(email))

        assertEquals(401, http.post("/api/v1/auth/login", """{"email":"$email","password":"WrongPass123"}""").status)
    }

    @Test
    @Tag("core")
    fun `login of unknown user returns 401`() =
        assertEquals(401, http.post("/api/v1/auth/login", """{"email":"${email()}","password":"$password"}""").status)

    @Test
    @Tag("extension")
    fun `refresh token gives a new access token`() {
        val email = email()
        val registered = http.post("/api/v1/auth/register", registerBody(email))

        val refreshed = http.post("/api/v1/auth/refresh", """{"refreshToken":"${registered.json["refreshToken"].asString()}"}""")

        assertEquals(200, refreshed.status, refreshed.body)
        assertTrue(refreshed.json["accessToken"].asString().isNotBlank())
    }

    @Test
    @Tag("extension")
    fun `refresh after logout returns 401`() {
        val registered = http.post("/api/v1/auth/register", registerBody(email()))
        val refresh = registered.json["refreshToken"].asString()
        val body = """{"refreshToken":"$refresh"}"""

        http.post("/api/v1/auth/logout", body, registered.json["accessToken"].asString())

        assertEquals(401, http.post("/api/v1/auth/refresh", body).status)
    }
}

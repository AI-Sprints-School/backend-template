package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Урок 11: что открыто (каталог, пробы, регистрация и вход), что закрыто (401 без токена). */
@Tag("core")
@Tag("chapter3")
class AccessRulesTest : IntegrationTest() {

    @Test
    fun `catalog is public`() = assertEquals(200, http.get("/api/v1/courses").status)

    @Test
    fun `probes are public`() = assertEquals(200, http.get("/health/live").status)

    @Test
    fun `register is public and reaches validation`() =
        assertEquals(400, http.post("/api/v1/auth/register", "{}").status, "Регистрация должна быть открыта: ждём 400 валидации, не 401")

    @Test
    fun `auth me without token returns 401`() = assertEquals(401, http.get("/api/v1/auth/me").status)

    @Test
    fun `user progress without token returns 401`() = assertEquals(401, http.get("/api/v1/user/progress").status)

    @Test
    fun `course creation without token returns 401`() = assertEquals(401, http.post("/api/v1/courses", "{}").status)
}

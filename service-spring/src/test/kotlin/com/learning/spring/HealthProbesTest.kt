package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Урок 4: тот же контракт проб, что у Ktor-двойника, — но отвечает Actuator, а не ручной маршрут. */
@Tag("core")
@Tag("chapter1")
class HealthProbesTest : IntegrationTest() {

    @Test
    fun `live probe returns 200 and UP`() {
        val response = http.get("/health/live")
        assertEquals(200, response.status, response.body)
        assertEquals("UP", response.json["status"].asString())
    }

    @Test
    fun `ready probe returns 200 and shows database`() {
        val response = http.get("/health/ready")
        assertEquals(200, response.status, response.body)
        assertEquals("UP", response.json["status"].asString())
        assertEquals("UP", response.json["components"]["db"]["status"].asString(), "Проба готовности не проверяет базу: нет db в группе ready")
    }

    @Test
    fun `summary health returns 200 and shows database`() {
        val response = http.get("/health")
        assertEquals(200, response.status, response.body)
        assertEquals("UP", response.json["components"]["db"]["status"].asString())
    }
}

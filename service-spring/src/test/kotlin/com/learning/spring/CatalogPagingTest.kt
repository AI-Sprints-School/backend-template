package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Урок 9: каталог со страницами — `page`, `limit`, блок `pagination`. */
@Tag("core")
@Tag("chapter2")
class CatalogPagingTest : IntegrationTest() {

    @Test
    fun `limit caps the page size`() {
        repeat(3) { data.course() }

        val response = http.get("/api/v1/courses?page=1&limit=2")

        assertEquals(200, response.status, response.body)
        assertEquals(2, response.json["data"].size())
        assertEquals(2, response.json["pagination"]["limit"].asInt())
        assertTrue(response.json["pagination"]["total"].asInt() >= 3)
    }

    @Test
    fun `total pages match total and limit`() {
        repeat(3) { data.course() }

        val pagination = http.get("/api/v1/courses?page=1&limit=2").json["pagination"]

        val total = pagination["total"].asInt()
        assertEquals((total + 1) / 2, pagination["totalPages"].asInt())
    }
}

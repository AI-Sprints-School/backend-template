package com.learning.spring

import com.learning.spring.support.IntegrationTest
import com.learning.spring.support.RollbackScenario
import com.learning.spring.support.TestHttp
import com.learning.spring.support.TestKafka
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Урок 23: событие уходит только после коммита и только о новой отметке.
 * Как доказать, что события нет: после проверяемого действия тот же студент отмечает другой урок —
 * это «метка». У событий одного ключа одна партиция и общий порядок, поэтому всё, что сервис
 * отправил до метки, лежит в топике раньше неё.
 */
@Tag("core")
@Tag("chapter6")
class AfterCommitTest : IntegrationTest() {

    @Test
    fun `rolled back completion publishes no event`() {
        val course = data.course()
        val failing = data.lesson(course, 1)
        val marker = data.lesson(course, 2)
        val student = users.student()
        val rollback = RollbackScenario(jdbc)

        TestKafka.recorder().use { topic ->
            rollback.failCommitFor(failing.id)
            try {
                val response = http.post("/api/v1/lessons/${failing.id}/complete", bearer = student.token)
                assertTrue(response.status >= 500, "Коммит должен был упасть (сценарий отката), а ответ ${response.status}: ${response.body}")
            } finally {
                rollback.clear()
            }
            assertEquals(200, http.post("/api/v1/lessons/${marker.id}/complete", bearer = student.token).status)

            val lessons = topic.awaitRecords { lessonId(it.value()) == marker.id.toString() }
                .filter { it.key() == student.id.toString() }.map { lessonId(it.value()) }
            assertEquals(listOf(marker.id.toString()), lessons, "Откаченная отметка не должна давать события")
        }
    }

    @Test
    fun `completing the same lesson twice publishes one event`() {
        val course = data.course()
        val lesson = data.lesson(course, 1)
        val marker = data.lesson(course, 2)
        val student = users.student()

        TestKafka.recorder().use { topic ->
            repeat(2) { assertEquals(200, http.post("/api/v1/lessons/${lesson.id}/complete", bearer = student.token).status) }
            assertEquals(200, http.post("/api/v1/lessons/${marker.id}/complete", bearer = student.token).status)

            val lessons = topic.awaitRecords { lessonId(it.value()) == marker.id.toString() }
                .filter { it.key() == student.id.toString() }.map { lessonId(it.value()) }
            assertEquals(listOf(lesson.id.toString(), marker.id.toString()), lessons, "Повторная отметка — не новое событие")
        }
    }

    private fun lessonId(json: String): String? = runCatching { TestHttp.mapper.readTree(json)["lessonId"]?.asString() }.getOrNull()
}

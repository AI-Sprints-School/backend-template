package com.learning.spring

import com.learning.spring.support.IntegrationTest
import com.learning.spring.support.TestHttp
import com.learning.spring.support.TestKafka
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Урок 22: отметка урока отправляет событие `LessonCompletedEvent` в топик `lesson-completed`
 * (3 партиции) с ключом `userId`. Тест читает брокер обычным клиентом Kafka — мимо кода сервиса.
 */
@Tag("core")
@Tag("chapter6")
class LessonEventPublishingTest : IntegrationTest() {

    @Test
    fun `topic lesson-completed has three partitions`() {
        assertEquals(3, TestKafka.partitions(), "Топик lesson-completed: 3 партиции (NewTopic через TopicBuilder)")
    }

    @Test
    fun `completing a lesson publishes an event keyed by user id`() {
        val course = data.course()
        val first = data.lesson(course, 1)
        data.lesson(course, 2)
        val student = users.student()

        TestKafka.recorder().use { topic ->
            assertEquals(200, http.post("/api/v1/lessons/${first.id}/complete", bearer = student.token).status)

            val record = topic.awaitRecords { it.key() == student.id.toString() }.single { it.key() == student.id.toString() }
            val event = TestHttp.mapper.readTree(record.value())
            assertTrue(event.hasNonNull("eventId"), "В событии нет eventId: ${record.value()}")
            assertEquals(student.id.toString(), event["userId"].asString())
            assertEquals(course.id.toString(), event["courseId"].asString())
            assertEquals(first.id.toString(), event["lessonId"].asString())
            assertEquals(1, event["completedLessons"].asInt())
            assertEquals(2, event["totalLessons"].asInt())
            assertNotNull(event["occurredAt"]?.takeIf { !it.isNull }, "В событии нет occurredAt: ${record.value()}")
            assertTrue(event["occurredAt"].isString, "occurredAt — строка ISO-8601 (Instant), а не число: ${record.value()}")
            Instant.parse(event["occurredAt"].asString())
        }
    }
}

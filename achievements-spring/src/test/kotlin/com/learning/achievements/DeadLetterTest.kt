package com.learning.achievements

import com.learning.achievements.support.AchievementsTest
import com.learning.achievements.support.TestEvents.Event
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Урок 25: сбойная запись не стопорит очередь — после повторов она уходит в `lesson-completed-dlt`,
 * следующая запись той же партиции обрабатывается.
 *
 * До урока 25 класс пропускается: без `ErrorHandlingDeserializer` битый JSON не доходит до обработчика
 * ошибок, консьюмер читает его снова и снова, лог растёт без конца, и прогон падает по памяти.
 */
@Tag("core")
@Tag("chapter6")
class DeadLetterTest : AchievementsTest() {

    @Autowired
    private lateinit var consumerFactory: ConsumerFactory<*, *>

    @BeforeEach
    fun requireErrorHandlingDeserializer() {
        val configured = consumerFactory.valueDeserializer is ErrorHandlingDeserializer<*> ||
            consumerFactory.configurationProperties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG].toString()
                .contains(ErrorHandlingDeserializer::class.java.simpleName)
        assumeTrue(configured) { "Урок 25: нет ErrorHandlingDeserializer в spring.kafka.consumer — тест пропущен" }
    }

    @Test
    fun `broken json goes to the dead letter topic and the next record is processed`() {
        val userId = UUID.randomUUID()
        val next = Event(userId = userId, completedLessons = 1, totalLessons = 1)

        events.deadLetters().use { dlt ->
            events.send(userId.toString(), """{"eventId": "not-json""")
            events.send(next)

            dlt.await { it.key() == userId.toString() }
        }
        awaitAchievement(userId, next.courseId)
    }

    @Test
    fun `invalid event goes to the dead letter topic without an achievement`() {
        val invalid = Event(completedLessons = 5, totalLessons = 3)
        val next = Event(userId = invalid.userId, completedLessons = 1, totalLessons = 1)

        events.deadLetters().use { dlt ->
            events.send(invalid)
            events.send(next)

            val record = dlt.await { it.key() == invalid.userId.toString() }
            assertTrue(record.value().orEmpty().contains(invalid.eventId.toString()), "В DLT — та самая запись: ${record.value()}")
        }
        awaitAchievement(invalid.userId, next.courseId)
        assertEquals(0, achievements(invalid.userId, invalid.courseId))
    }

    private fun awaitAchievement(userId: UUID, courseId: UUID) {
        await atMost Duration.ofSeconds(60) untilAsserted {
            assertEquals(1, achievements(userId, courseId), "Следующая запись после сбойной не обработана — очередь стоит")
        }
    }
}

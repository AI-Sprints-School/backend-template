package com.learning.achievements

import com.learning.achievements.support.AchievementsTest
import com.learning.achievements.support.TestEvents.Event
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Урок 24: консьюмер достижений и идемпотентная обработка.
 * Как доказать, что достижения нет: после проверяемого события тот же студент проходит другой курс —
 * это «метка». Ключ один — партиция одна, консьюмер обрабатывает события по порядку, поэтому
 * когда метка обработана, проверяемое событие обработано тоже.
 */
@Tag("core")
@Tag("chapter6")
class AchievementConsumerTest : AchievementsTest() {

    @Test
    fun `last lesson of a course grants an achievement`() {
        val event = Event(completedLessons = 3, totalLessons = 3)

        events.send(event)

        awaitAchievement(event.userId, event.courseId)
    }

    @Test
    fun `not the last lesson grants nothing`() {
        val userId = UUID.randomUUID()
        val event = Event(userId = userId, completedLessons = 2, totalLessons = 3)

        events.send(event)
        sendMarkerAndWait(userId)

        assertEquals(0, achievements(userId, event.courseId))
    }

    @Test
    fun `the same event twice grants one achievement`() {
        val event = Event(completedLessons = 3, totalLessons = 3)

        repeat(2) { events.send(event) }
        sendMarkerAndWait(event.userId)

        assertEquals(1, achievements(event.userId, event.courseId))
        assertEquals(1, processed(event.eventId), "Событие отмечается в processed_events")
    }

    @Test
    fun `two final events of one course grant one achievement`() {
        val first = Event(completedLessons = 3, totalLessons = 3)
        val second = first.copy(eventId = UUID.randomUUID(), lessonId = UUID.randomUUID())

        events.send(first)
        events.send(second)
        sendMarkerAndWait(first.userId)

        assertEquals(1, achievements(first.userId, first.courseId))
    }

    private fun sendMarkerAndWait(userId: UUID) {
        val marker = Event(userId = userId, completedLessons = 1, totalLessons = 1)
        events.send(marker)
        awaitAchievement(userId, marker.courseId)
    }

    private fun awaitAchievement(userId: UUID, courseId: UUID) {
        await atMost Duration.ofSeconds(30) untilAsserted {
            assertEquals(1, achievements(userId, courseId), "Достижение «курс пройден» не выдано")
        }
    }
}

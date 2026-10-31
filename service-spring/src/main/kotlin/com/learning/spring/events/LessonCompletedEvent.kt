package com.learning.spring.events

import java.time.Instant
import java.util.UUID

/**
 * Глава 6: событие «урок пройден» — контракт между сервисами. Копия этого класса лежит во втором
 * сервисе (`achievements-spring/…/events/LessonCompletedEvent.kt`): поля и их смысл меняются
 * только в обоих местах сразу. В топик `lesson-completed` событие уходит JSON с ключом `userId`.
 *
 * @property eventId id события — по нему получатель отбрасывает повтор
 * @property completedLessons сколько опубликованных уроков курса студент прошёл, включая этот
 * @property totalLessons сколько опубликованных уроков в курсе
 * @property occurredAt когда отметка записана (в JSON — строка ISO-8601)
 */
data class LessonCompletedEvent(
    val eventId: UUID,
    val userId: UUID,
    val courseId: UUID,
    val lessonId: UUID,
    val completedLessons: Int,
    val totalLessons: Int,
    val occurredAt: Instant,
)

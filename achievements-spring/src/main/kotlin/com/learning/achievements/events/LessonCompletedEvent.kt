package com.learning.achievements.events

import java.time.Instant
import java.util.UUID

/**
 * Событие «урок пройден» — копия контракта из `service-spring/…/events/LessonCompletedEvent.kt`.
 * Пакеты у копий разные, поэтому имя класса из заголовка записи здесь не годится: тип для
 * десериализации задаёт сам получатель. Поля и смысл — как у отправителя; меняются в обоих местах сразу.
 *
 * Корректное событие: `totalLessons > 0` и `completedLessons` от 1 до `totalLessons`. Событие,
 * которое этому не отвечает, обработчик отклоняет исключением — оно уходит в `lesson-completed-dlt` (урок 25).
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

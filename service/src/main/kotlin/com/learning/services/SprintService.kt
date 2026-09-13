package com.learning.services

import com.learning.models.SprintRequest
import com.learning.models.SprintResponse
import com.learning.repositories.SprintRepository
import java.time.Instant
import java.util.*

/** Спринты-акции. Реализация перенесена с ветки `last-checkpoints` (04df904). */
class SprintService(
    private val sprintRepository: SprintRepository
) {

    fun getAllSprints(): List<SprintResponse> {
        TODO("Расширение, глава 4, урок 17: SprintService.getAllSprints")
    }

    fun getSprintById(id: String): SprintResponse? {
        TODO("Расширение, глава 4, урок 17: SprintService.getSprintById")
    }

    fun createSprint(request: SprintRequest): SprintResponse {
        TODO("Расширение, глава 4, урок 17: SprintService.createSprint")
    }

    fun updateSprint(id: String, request: SprintRequest): SprintResponse {
        TODO("Расширение, глава 4, урок 17: SprintService.updateSprint")
    }

    fun deleteSprint(id: String): Boolean {
        TODO("Расширение, глава 4, урок 17: SprintService.deleteSprint")
    }

    private fun com.learning.domain.models.Sprint.toResponse() = SprintResponse(
        id = id.toString(),
        title = title,
        description = description,
        coverImage = null,
        discount = discountPercentage,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        isActive = isActive,
        courses = emptyList()
    )

    /** Дата в ISO-8601 (`2026-09-01T00:00:00Z`); неверный формат — ошибка запроса, а не «сейчас». */
    private fun parseInstant(dateStr: String): Instant {
        return try {
            Instant.parse(dateStr)
        } catch (e: Exception) {
            throw IllegalArgumentException("Неверный формат даты: $dateStr")
        }
    }
}

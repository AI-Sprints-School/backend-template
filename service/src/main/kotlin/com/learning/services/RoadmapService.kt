package com.learning.services

import com.learning.domain.models.Roadmap
import com.learning.domain.models.RoadmapStep
import com.learning.models.*
import com.learning.repositories.RoadmapRepository
import java.util.*

/** Роадмап. Реализация перенесена с ветки `last-checkpoints` (04df904). */
class RoadmapService(
    private val roadmapRepository: RoadmapRepository
) {

    fun getRoadmap(): RoadmapResponse? {
        val roadmap = roadmapRepository.findAllRoadmaps().firstOrNull() ?: return null
        val (_, steps) = roadmapRepository.getRoadmapWithSteps(roadmap.id)
        return roadmap.toResponse(steps)
    }

    fun updateRoadmap(request: RoadmapRequest): RoadmapResponse {
        val existing = roadmapRepository.findAllRoadmaps().firstOrNull()
            ?: throw Exception("Роадмап не найден")

        roadmapRepository.updateRoadmap(
            roadmapId = existing.id,
            title = request.title,
            description = request.description,
            category = request.category
        )

        val updated = roadmapRepository.findRoadmapById(existing.id)
            ?: throw Exception("Ошибка при обновлении роадмапа")
        val (_, steps) = roadmapRepository.getRoadmapWithSteps(updated.id)

        return updated.toResponse(steps)
    }

    fun getAllRoadmaps(): List<RoadmapResponse> {
        return roadmapRepository.findAllRoadmaps().map { roadmap ->
            val (_, steps) = roadmapRepository.getRoadmapWithSteps(roadmap.id)
            roadmap.toResponse(steps)
        }
    }

    private fun Roadmap.toResponse(steps: List<RoadmapStep>) = RoadmapResponse(
        id = id.toString(),
        title = title,
        description = description,
        category = category,
        stages = steps.map { step ->
            RoadmapStageResponse(
                id = step.id.toString(),
                title = step.title,
                description = step.description,
                orderIndex = step.order,
                estimatedDuration = 0,
                courses = emptyList()
            )
        }
    )
}

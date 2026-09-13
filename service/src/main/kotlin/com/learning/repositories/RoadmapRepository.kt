package com.learning.repositories

import com.learning.database.RoadmapSteps
import com.learning.database.Roadmaps
import com.learning.domain.models.Roadmap
import com.learning.domain.models.RoadmapStep
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class RoadmapRepository {
    private val logger = LoggerFactory.getLogger(RoadmapRepository::class.java)

    // Roadmap methods
    fun createRoadmap(
        title: String,
        description: String,
        category: String,
        isPublished: Boolean = false
    ): Roadmap? {
        return try {
            transaction {
                val roadmapId = UUID.randomUUID()
                Roadmaps.insert {
                    it[id] = roadmapId
                    it[Roadmaps.title] = title
                    it[Roadmaps.description] = description
                    it[Roadmaps.category] = category
                    it[Roadmaps.isPublished] = isPublished
                    it[Roadmaps.createdAt] = Instant.now()
                    it[Roadmaps.updatedAt] = Instant.now()
                }

                Roadmaps.selectAll().where { Roadmaps.id eq roadmapId }.singleOrNull()?.toRoadmap()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании roadmap: ${e.message}", e)
            null
        }
    }

    fun findRoadmapById(roadmapId: UUID): Roadmap? {
        return try {
            transaction {
                Roadmaps.selectAll().where { Roadmaps.id eq roadmapId }.singleOrNull()?.toRoadmap()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске roadmap по ID: ${e.message}", e)
            null
        }
    }

    fun findAllRoadmaps(): List<Roadmap> {
        return try {
            transaction {
                Roadmaps.selectAll()
                    .orderBy(Roadmaps.createdAt to SortOrder.DESC)
                    .map { it.toRoadmap() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении всех roadmap: ${e.message}", e)
            emptyList()
        }
    }

    fun findPublishedRoadmaps(): List<Roadmap> {
        return try {
            transaction {
                Roadmaps.selectAll().where { Roadmaps.isPublished eq true }
                    .orderBy(Roadmaps.createdAt to SortOrder.DESC)
                    .map { it.toRoadmap() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении опубликованных roadmap: ${e.message}", e)
            emptyList()
        }
    }

    fun findByCategory(category: String): List<Roadmap> {
        return try {
            transaction {
                Roadmaps.selectAll().where { Roadmaps.category eq category }
                    .orderBy(Roadmaps.createdAt to SortOrder.DESC)
                    .map { it.toRoadmap() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске roadmap по категории: ${e.message}", e)
            emptyList()
        }
    }

    fun updateRoadmap(
        roadmapId: UUID,
        title: String? = null,
        description: String? = null,
        category: String? = null,
        isPublished: Boolean? = null
    ): Boolean {
        return try {
            transaction {
                Roadmaps.update({ Roadmaps.id eq roadmapId }) { updateBuilder ->
                    title?.let { updateBuilder[Roadmaps.title] = title }
                    description?.let { updateBuilder[Roadmaps.description] = description }
                    category?.let { updateBuilder[Roadmaps.category] = category }
                    isPublished?.let { updateBuilder[Roadmaps.isPublished] = isPublished }
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении roadmap: ${e.message}", e)
            false
        }
    }

    fun deleteRoadmap(roadmapId: UUID): Boolean {
        return try {
            transaction {
                Roadmaps.deleteWhere { Roadmaps.id eq roadmapId } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении roadmap: ${e.message}", e)
            false
        }
    }

    // RoadmapStep methods
    fun createRoadmapStep(
        roadmapId: UUID,
        title: String,
        description: String,
        order: Int,
        courseId: UUID? = null,
        isCompleted: Boolean = false
    ): RoadmapStep? {
        return try {
            transaction {
                val stepId = UUID.randomUUID()
                RoadmapSteps.insert {
                    it[id] = stepId
                    it[RoadmapSteps.roadmapId] = roadmapId
                    it[RoadmapSteps.title] = title
                    it[RoadmapSteps.description] = description
                    it[RoadmapSteps.order] = order
                    it[RoadmapSteps.courseId] = courseId
                    it[RoadmapSteps.isCompleted] = isCompleted
                    it[RoadmapSteps.createdAt] = Instant.now()
                    it[RoadmapSteps.updatedAt] = Instant.now()
                }

                RoadmapSteps.selectAll().where { RoadmapSteps.id eq stepId }.singleOrNull()?.toRoadmapStep()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании шага roadmap: ${e.message}", e)
            null
        }
    }

    fun findStepById(stepId: UUID): RoadmapStep? {
        return try {
            transaction {
                RoadmapSteps.selectAll().where { RoadmapSteps.id eq stepId }.singleOrNull()?.toRoadmapStep()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске шага по ID: ${e.message}", e)
            null
        }
    }

    fun findStepsByRoadmapId(roadmapId: UUID): List<RoadmapStep> {
        return try {
            transaction {
                RoadmapSteps.selectAll().where { RoadmapSteps.roadmapId eq roadmapId }
                    .orderBy(RoadmapSteps.order to SortOrder.ASC)
                    .map { it.toRoadmapStep() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении шагов roadmap: ${e.message}", e)
            emptyList()
        }
    }

    fun updateRoadmapStep(
        stepId: UUID,
        title: String? = null,
        description: String? = null,
        order: Int? = null,
        courseId: UUID? = null,
        isCompleted: Boolean? = null
    ): Boolean {
        return try {
            transaction {
                RoadmapSteps.update({ RoadmapSteps.id eq stepId }) { updateBuilder ->
                    title?.let { updateBuilder[RoadmapSteps.title] = title }
                    description?.let { updateBuilder[RoadmapSteps.description] = description }
                    order?.let { updateBuilder[RoadmapSteps.order] = order }
                    courseId?.let { updateBuilder[RoadmapSteps.courseId] = courseId }
                    isCompleted?.let { updateBuilder[RoadmapSteps.isCompleted] = isCompleted }
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении шага roadmap: ${e.message}", e)
            false
        }
    }

    fun deleteRoadmapStep(stepId: UUID): Boolean {
        return try {
            transaction {
                RoadmapSteps.deleteWhere { RoadmapSteps.id eq stepId } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении шага roadmap: ${e.message}", e)
            false
        }
    }

    fun reorderSteps(roadmapId: UUID, stepOrders: Map<UUID, Int>): Boolean {
        return try {
            transaction {
                stepOrders.forEach { (stepId, newOrder) ->
                    RoadmapSteps.update({ RoadmapSteps.id eq stepId }) { updateBuilder ->
                        updateBuilder[RoadmapSteps.order] = newOrder
                        updateBuilder[updatedAt] = Instant.now()
                    }
                }
                true
            }
        } catch (e: Exception) {
            logger.error("Ошибка при изменении порядка шагов: ${e.message}", e)
            false
        }
    }

    fun getRoadmapWithSteps(roadmapId: UUID): Pair<Roadmap?, List<RoadmapStep>> {
        return try {
            transaction {
                val roadmap = Roadmaps.selectAll().where { Roadmaps.id eq roadmapId }.singleOrNull()?.toRoadmap()
                val steps = RoadmapSteps.selectAll().where { RoadmapSteps.roadmapId eq roadmapId }
                    .orderBy(RoadmapSteps.order to SortOrder.ASC)
                    .map { it.toRoadmapStep() }
                Pair(roadmap, steps)
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении roadmap с шагами: ${e.message}", e)
            Pair(null, emptyList())
        }
    }

    fun getCategories(): List<String> {
        return try {
            transaction {
                Roadmaps.selectAll()
                    .map { it[Roadmaps.category] }
                    .distinct()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении категорий roadmap: ${e.message}", e)
            emptyList()
        }
    }

    fun searchRoadmaps(query: String): List<Roadmap> {
        return try {
            transaction {
                Roadmaps.selectAll().where {
                    (Roadmaps.title like "%$query%") or
                            (Roadmaps.description like "%$query%") or
                            (Roadmaps.category like "%$query%")
                }.orderBy(Roadmaps.createdAt to SortOrder.DESC)
                    .map { it.toRoadmap() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске roadmap: ${e.message}", e)
            emptyList()
        }
    }

    private fun ResultRow.toRoadmap(): Roadmap {
        return Roadmap(
            id = this[Roadmaps.id],
            title = this[Roadmaps.title],
            description = this[Roadmaps.description],
            category = this[Roadmaps.category],
            isPublished = this[Roadmaps.isPublished],
            createdAt = this[Roadmaps.createdAt],
            updatedAt = this[Roadmaps.updatedAt]
        )
    }

    private fun ResultRow.toRoadmapStep(): RoadmapStep {
        return RoadmapStep(
            id = this[RoadmapSteps.id],
            roadmapId = this[RoadmapSteps.roadmapId],
            title = this[RoadmapSteps.title],
            description = this[RoadmapSteps.description],
            order = this[RoadmapSteps.order],
            courseId = this[RoadmapSteps.courseId],
            isCompleted = this[RoadmapSteps.isCompleted],
            createdAt = this[RoadmapSteps.createdAt],
            updatedAt = this[RoadmapSteps.updatedAt]
        )
    }
}

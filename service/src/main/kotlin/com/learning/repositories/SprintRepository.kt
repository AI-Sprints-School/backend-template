package com.learning.repositories

import com.learning.database.Sprints
import com.learning.domain.models.Sprint
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class SprintRepository {
    private val logger = LoggerFactory.getLogger(SprintRepository::class.java)

    fun createSprint(
        title: String,
        description: String,
        discountPercentage: Int,
        startDate: Instant,
        endDate: Instant,
        isActive: Boolean = true
    ): Sprint? {
        return try {
            transaction {
                val sprintId = UUID.randomUUID()
                Sprints.insert {
                    it[id] = sprintId
                    it[Sprints.title] = title
                    it[Sprints.description] = description
                    it[Sprints.discountPercentage] = discountPercentage
                    it[Sprints.startDate] = startDate
                    it[Sprints.endDate] = endDate
                    it[Sprints.isActive] = isActive
                    it[Sprints.createdAt] = Instant.now()
                    it[Sprints.updatedAt] = Instant.now()
                }

                Sprints.selectAll().where { Sprints.id eq sprintId }.singleOrNull()?.toSprint()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании спринта: ${e.message}", e)
            null
        }
    }

    fun findById(sprintId: UUID): Sprint? {
        return try {
            transaction {
                Sprints.selectAll().where { Sprints.id eq sprintId }.singleOrNull()?.toSprint()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске спринта по ID: ${e.message}", e)
            null
        }
    }

    fun findAll(): List<Sprint> {
        return try {
            transaction {
                Sprints.selectAll()
                    .orderBy(Sprints.createdAt to SortOrder.DESC)
                    .map { it.toSprint() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении всех спринтов: ${e.message}", e)
            emptyList()
        }
    }

    fun findActive(): List<Sprint> {
        return try {
            transaction {
                Sprints.selectAll().where { Sprints.isActive eq true }
                    .orderBy(Sprints.startDate to SortOrder.ASC)
                    .map { it.toSprint() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении активных спринтов: ${e.message}", e)
            emptyList()
        }
    }

    fun findCurrentlyActive(): List<Sprint> {
        return try {
            val now = Instant.now()
            transaction {
                Sprints.selectAll().where {
                    (Sprints.isActive eq true) and
                            (Sprints.startDate less now) and
                            (Sprints.endDate greater now)
                }.orderBy(Sprints.startDate to SortOrder.ASC)
                    .map { it.toSprint() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении текущих активных спринтов: ${e.message}", e)
            emptyList()
        }
    }

    fun findUpcoming(): List<Sprint> {
        return try {
            val now = Instant.now()
            transaction {
                Sprints.selectAll().where {
                    (Sprints.isActive eq true) and
                            (Sprints.startDate greater now)
                }.orderBy(Sprints.startDate to SortOrder.ASC)
                    .map { it.toSprint() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении предстоящих спринтов: ${e.message}", e)
            emptyList()
        }
    }

    fun findExpired(): List<Sprint> {
        return try {
            val now = Instant.now()
            transaction {
                Sprints.selectAll().where {
                    Sprints.endDate less now
                }.orderBy(Sprints.endDate to SortOrder.DESC)
                    .map { it.toSprint() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении истекших спринтов: ${e.message}", e)
            emptyList()
        }
    }

    fun updateSprint(
        sprintId: UUID,
        title: String? = null,
        description: String? = null,
        discountPercentage: Int? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        isActive: Boolean? = null
    ): Boolean {
        return try {
            transaction {
                Sprints.update({ Sprints.id eq sprintId }) { updateBuilder ->
                    title?.let { updateBuilder[Sprints.title] = title }
                    description?.let { updateBuilder[Sprints.description] = description }
                    discountPercentage?.let { updateBuilder[Sprints.discountPercentage] = discountPercentage }
                    startDate?.let { updateBuilder[Sprints.startDate] = startDate }
                    endDate?.let { updateBuilder[Sprints.endDate] = endDate }
                    isActive?.let { updateBuilder[Sprints.isActive] = isActive }
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении спринта: ${e.message}", e)
            false
        }
    }

    fun deleteSprint(sprintId: UUID): Boolean {
        return try {
            transaction {
                Sprints.deleteWhere { Sprints.id eq sprintId } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении спринта: ${e.message}", e)
            false
        }
    }

    fun activateSprint(sprintId: UUID): Boolean {
        return try {
            transaction {
                Sprints.update({ Sprints.id eq sprintId }) { updateBuilder ->
                    updateBuilder[Sprints.isActive] = true
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при активации спринта: ${e.message}", e)
            false
        }
    }

    fun deactivateSprint(sprintId: UUID): Boolean {
        return try {
            transaction {
                Sprints.update({ Sprints.id eq sprintId }) { updateBuilder ->
                    updateBuilder[Sprints.isActive] = false
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при деактивации спринта: ${e.message}", e)
            false
        }
    }

    fun getSprintStats(sprintId: UUID): Map<String, Any> {
        return try {
            transaction {
                val sprint = Sprints.selectAll().where { Sprints.id eq sprintId }.singleOrNull()
                if (sprint != null) {
                    val now = Instant.now()
                    mapOf(
                        "isActive" to sprint[Sprints.isActive],
                        "isCurrentlyActive" to (sprint[Sprints.isActive] &&
                                sprint[Sprints.startDate] < now &&
                                sprint[Sprints.endDate] > now),
                        "isExpired" to (sprint[Sprints.endDate] < now),
                        "isUpcoming" to (sprint[Sprints.startDate] > now),
                        "discountPercentage" to sprint[Sprints.discountPercentage],
                        "startDate" to sprint[Sprints.startDate],
                        "endDate" to sprint[Sprints.endDate]
                    )
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении статистики спринта: ${e.message}", e)
            emptyMap()
        }
    }

    fun searchSprints(query: String): List<Sprint> {
        return try {
            transaction {
                Sprints.selectAll().where {
                    (Sprints.title like "%$query%") or
                            (Sprints.description like "%$query%")
                }.orderBy(Sprints.createdAt to SortOrder.DESC)
                    .map { it.toSprint() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске спринтов: ${e.message}", e)
            emptyList()
        }
    }

    private fun ResultRow.toSprint(): Sprint {
        return Sprint(
            id = this[Sprints.id],
            title = this[Sprints.title],
            description = this[Sprints.description],
            discountPercentage = this[Sprints.discountPercentage],
            startDate = this[Sprints.startDate],
            endDate = this[Sprints.endDate],
            isActive = this[Sprints.isActive],
            createdAt = this[Sprints.createdAt],
            updatedAt = this[Sprints.updatedAt]
        )
    }
}

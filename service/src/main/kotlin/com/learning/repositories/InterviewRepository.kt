package com.learning.repositories

import com.learning.database.InterviewQuestions
import com.learning.domain.models.InterviewQuestion
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class InterviewRepository {
    private val logger = LoggerFactory.getLogger(InterviewRepository::class.java)

    fun createQuestion(
        question: String,
        answer: String,
        category: String,
        difficulty: String,
        tags: List<String> = emptyList(),
        isPublished: Boolean = false
    ): InterviewQuestion? {
        return try {
            transaction {
                val questionId = UUID.randomUUID()
                InterviewQuestions.insert {
                    it[id] = questionId
                    it[InterviewQuestions.question] = question
                    it[InterviewQuestions.answer] = answer
                    it[InterviewQuestions.category] = category
                    it[InterviewQuestions.difficulty] = difficulty
                    it[InterviewQuestions.tags] = tags.joinToString(",")
                    it[InterviewQuestions.isPublished] = isPublished
                    it[InterviewQuestions.createdAt] = Instant.now()
                    it[InterviewQuestions.updatedAt] = Instant.now()
                }

                InterviewQuestions.selectAll().where { InterviewQuestions.id eq questionId }.singleOrNull()?.toInterviewQuestion()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании вопроса собеседования: ${e.message}", e)
            null
        }
    }

    fun findById(questionId: UUID): InterviewQuestion? {
        return try {
            transaction {
                InterviewQuestions.selectAll().where { InterviewQuestions.id eq questionId }.singleOrNull()?.toInterviewQuestion()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске вопроса по ID: ${e.message}", e)
            null
        }
    }

    fun findAll(): List<InterviewQuestion> {
        return try {
            transaction {
                InterviewQuestions.selectAll()
                    .orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении всех вопросов: ${e.message}", e)
            emptyList()
        }
    }

    fun findPublished(): List<InterviewQuestion> {
        return try {
            transaction {
                InterviewQuestions.selectAll().where { InterviewQuestions.isPublished eq true }
                    .orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении опубликованных вопросов: ${e.message}", e)
            emptyList()
        }
    }

    fun findByCategory(category: String): List<InterviewQuestion> {
        return try {
            transaction {
                InterviewQuestions.selectAll().where { InterviewQuestions.category eq category }
                    .orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске вопросов по категории: ${e.message}", e)
            emptyList()
        }
    }

    fun findByDifficulty(difficulty: String): List<InterviewQuestion> {
        return try {
            transaction {
                InterviewQuestions.selectAll().where { InterviewQuestions.difficulty eq difficulty }
                    .orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске вопросов по сложности: ${e.message}", e)
            emptyList()
        }
    }

    fun findByCategoryAndDifficulty(category: String, difficulty: String): List<InterviewQuestion> {
        return try {
            transaction {
                InterviewQuestions.selectAll().where {
                    (InterviewQuestions.category eq category) and
                            (InterviewQuestions.difficulty eq difficulty)
                }.orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске вопросов по категории и сложности: ${e.message}", e)
            emptyList()
        }
    }

    fun updateQuestion(
        questionId: UUID,
        question: String? = null,
        answer: String? = null,
        category: String? = null,
        difficulty: String? = null,
        tags: List<String>? = null,
        isPublished: Boolean? = null
    ): Boolean {
        return try {
            transaction {
                InterviewQuestions.update({ InterviewQuestions.id eq questionId }) { updateBuilder ->
                    question?.let { updateBuilder[InterviewQuestions.question] = question }
                    answer?.let { updateBuilder[InterviewQuestions.answer] = answer }
                    category?.let { updateBuilder[InterviewQuestions.category] = category }
                    difficulty?.let { updateBuilder[InterviewQuestions.difficulty] = difficulty }
                    tags?.let { updateBuilder[InterviewQuestions.tags] = tags.joinToString(",") }
                    isPublished?.let { updateBuilder[InterviewQuestions.isPublished] = isPublished }
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении вопроса: ${e.message}", e)
            false
        }
    }

    fun deleteQuestion(questionId: UUID): Boolean {
        return try {
            transaction {
                InterviewQuestions.deleteWhere { InterviewQuestions.id eq questionId } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении вопроса: ${e.message}", e)
            false
        }
    }

    fun searchQuestions(query: String): List<InterviewQuestion> {
        return try {
            transaction {
                InterviewQuestions.selectAll().where {
                    (InterviewQuestions.question like "%$query%") or
                            (InterviewQuestions.answer like "%$query%") or
                            (InterviewQuestions.category like "%$query%")
                }.orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске вопросов: ${e.message}", e)
            emptyList()
        }
    }

    fun getRandomQuestions(count: Int, category: String? = null, difficulty: String? = null): List<InterviewQuestion> {
        return try {
            transaction {
                val query = when {
                    category != null && difficulty != null -> InterviewQuestions.selectAll().where {
                        (InterviewQuestions.category eq category) and (InterviewQuestions.difficulty eq difficulty)
                    }

                    category != null -> InterviewQuestions.selectAll().where { InterviewQuestions.category eq category }
                    difficulty != null -> InterviewQuestions.selectAll().where { InterviewQuestions.difficulty eq difficulty }
                    else -> InterviewQuestions.selectAll()
                }

                query
                    .orderBy(InterviewQuestions.createdAt to SortOrder.DESC)
                    .limit(count)
                    .map { it.toInterviewQuestion() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении случайных вопросов: ${e.message}", e)
            emptyList()
        }
    }

    fun getCategories(): List<String> {
        return try {
            transaction {
                InterviewQuestions.selectAll()
                    .map { it[InterviewQuestions.category] }
                    .distinct()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении категорий: ${e.message}", e)
            emptyList()
        }
    }

    fun getDifficulties(): List<String> {
        return try {
            transaction {
                InterviewQuestions.selectAll()
                    .map { it[InterviewQuestions.difficulty] }
                    .distinct()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении уровней сложности: ${e.message}", e)
            emptyList()
        }
    }

    private fun ResultRow.toInterviewQuestion(): InterviewQuestion {
        return InterviewQuestion(
            id = this[InterviewQuestions.id],
            question = this[InterviewQuestions.question],
            answer = this[InterviewQuestions.answer],
            category = this[InterviewQuestions.category],
            difficulty = this[InterviewQuestions.difficulty],
            tags = this[InterviewQuestions.tags]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            isPublished = this[InterviewQuestions.isPublished],
            createdAt = this[InterviewQuestions.createdAt],
            updatedAt = this[InterviewQuestions.updatedAt]
        )
    }
}

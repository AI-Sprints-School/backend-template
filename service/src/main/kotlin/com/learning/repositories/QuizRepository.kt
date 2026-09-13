package com.learning.repositories

import com.learning.database.Quizzes
import com.learning.domain.models.Quiz
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class QuizRepository {
    private val logger = LoggerFactory.getLogger(QuizRepository::class.java)

    fun createQuiz(
        courseId: UUID?,
        lessonId: UUID?,
        title: String,
        description: String?,
        timeLimit: Int,
        passingScore: Int,
        isPublished: Boolean = false
    ): Quiz? {
        return try {
            transaction {
                val quizId = UUID.randomUUID()
                Quizzes.insert {
                    it[id] = quizId
                    it[Quizzes.courseId] = courseId
                    it[Quizzes.lessonId] = lessonId
                    it[Quizzes.title] = title
                    it[Quizzes.description] = description
                    it[Quizzes.timeLimit] = timeLimit
                    it[Quizzes.passingScore] = passingScore
                    it[Quizzes.questionsCount] = 0
                    it[Quizzes.isPublished] = isPublished
                    it[Quizzes.createdAt] = Instant.now()
                    it[Quizzes.updatedAt] = Instant.now()
                }

                Quizzes.selectAll().where { Quizzes.id eq quizId }.singleOrNull()?.toQuiz()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании теста: ${e.message}", e)
            null
        }
    }

    fun findById(quizId: UUID): Quiz? {
        return try {
            transaction {
                Quizzes.selectAll().where { Quizzes.id eq quizId }.singleOrNull()?.toQuiz()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске теста по ID: ${e.message}", e)
            null
        }
    }

    fun findAll(): List<Quiz> {
        return try {
            transaction {
                Quizzes.selectAll()
                    .orderBy(Quizzes.createdAt to SortOrder.DESC)
                    .map { it.toQuiz() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении всех тестов: ${e.message}", e)
            emptyList()
        }
    }

    fun findPublished(): List<Quiz> {
        return try {
            transaction {
                Quizzes.selectAll().where { Quizzes.isPublished eq true }
                    .orderBy(Quizzes.createdAt to SortOrder.DESC)
                    .map { it.toQuiz() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении опубликованных тестов: ${e.message}", e)
            emptyList()
        }
    }

    fun findByCourseId(courseId: UUID): List<Quiz> {
        return try {
            transaction {
                Quizzes.selectAll().where { Quizzes.courseId eq courseId }
                    .orderBy(Quizzes.createdAt to SortOrder.DESC)
                    .map { it.toQuiz() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении тестов курса: ${e.message}", e)
            emptyList()
        }
    }

    fun findByLessonId(lessonId: UUID): List<Quiz> {
        return try {
            transaction {
                Quizzes.selectAll().where { Quizzes.lessonId eq lessonId }
                    .orderBy(Quizzes.createdAt to SortOrder.DESC)
                    .map { it.toQuiz() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении тестов урока: ${e.message}", e)
            emptyList()
        }
    }

    fun updateQuiz(
        quizId: UUID,
        title: String? = null,
        description: String? = null,
        timeLimit: Int? = null,
        passingScore: Int? = null,
        isPublished: Boolean? = null
    ): Boolean {
        return try {
            transaction {
                Quizzes.update({ Quizzes.id eq quizId }) { updateBuilder ->
                    title?.let { updateBuilder[Quizzes.title] = title }
                    description?.let { updateBuilder[Quizzes.description] = description }
                    timeLimit?.let { updateBuilder[Quizzes.timeLimit] = timeLimit }
                    passingScore?.let { updateBuilder[Quizzes.passingScore] = passingScore }
                    isPublished?.let { updateBuilder[Quizzes.isPublished] = isPublished }
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении теста: ${e.message}", e)
            false
        }
    }

    fun deleteQuiz(quizId: UUID): Boolean {
        return try {
            transaction {
                Quizzes.deleteWhere { Quizzes.id eq quizId } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении теста: ${e.message}", e)
            false
        }
    }

    fun updateQuestionsCount(quizId: UUID, count: Int): Boolean {
        return try {
            transaction {
                Quizzes.update({ Quizzes.id eq quizId }) { updateBuilder ->
                    updateBuilder[Quizzes.questionsCount] = count
                    updateBuilder[updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении количества вопросов: ${e.message}", e)
            false
        }
    }

    fun getQuizStats(quizId: UUID): Map<String, Any> {
        return try {
            transaction {
                val quiz = Quizzes.selectAll().where { Quizzes.id eq quizId }.singleOrNull()
                if (quiz != null) {
                    mapOf(
                        "questionsCount" to quiz[Quizzes.questionsCount],
                        "timeLimit" to quiz[Quizzes.timeLimit],
                        "passingScore" to quiz[Quizzes.passingScore],
                        "isPublished" to quiz[Quizzes.isPublished]
                    )
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении статистики теста: ${e.message}", e)
            emptyMap()
        }
    }

    fun searchQuizzes(query: String): List<Quiz> {
        return try {
            transaction {
                Quizzes.selectAll().where {
                    (Quizzes.title like "%$query%") or
                            (Quizzes.description like "%$query%")
                }.orderBy(Quizzes.createdAt to SortOrder.DESC)
                    .map { it.toQuiz() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске тестов: ${e.message}", e)
            emptyList()
        }
    }

    private fun ResultRow.toQuiz(): Quiz {
        return Quiz(
            id = this[Quizzes.id],
            courseId = this[Quizzes.courseId],
            lessonId = this[Quizzes.lessonId],
            title = this[Quizzes.title],
            description = this[Quizzes.description],
            timeLimit = this[Quizzes.timeLimit],
            passingScore = this[Quizzes.passingScore],
            questionsCount = this[Quizzes.questionsCount],
            isPublished = this[Quizzes.isPublished],
            createdAt = this[Quizzes.createdAt],
            updatedAt = this[Quizzes.updatedAt]
        )
    }
}

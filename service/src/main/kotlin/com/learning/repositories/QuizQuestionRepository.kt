package com.learning.repositories

import com.learning.database.QuizAnswers
import com.learning.database.QuizQuestions
import com.learning.database.Quizzes
import com.learning.domain.models.QuestionType
import com.learning.domain.models.QuizAnswer
import com.learning.domain.models.QuizQuestion
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class QuizQuestionRepository {
    private val logger = LoggerFactory.getLogger(QuizQuestionRepository::class.java)

    // ==================== Вопросы ====================

    fun createQuestion(
        quizId: UUID,
        question: String,
        questionType: QuestionType,
        explanation: String?,
        points: Int,
        order: Int
    ): QuizQuestion? {
        return try {
            transaction {
                val questionId = UUID.randomUUID()
                QuizQuestions.insert {
                    it[id] = questionId
                    it[QuizQuestions.quizId] = quizId
                    it[QuizQuestions.question] = question
                    it[QuizQuestions.questionType] = questionType.name
                    it[QuizQuestions.explanation] = explanation
                    it[QuizQuestions.points] = points
                    it[QuizQuestions.order] = order
                    it[createdAt] = Instant.now()
                    it[updatedAt] = Instant.now()
                }

                // Обновляем счётчик вопросов в тесте
                updateQuizQuestionsCount(quizId)

                findQuestionById(questionId)
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании вопроса: ${e.message}", e)
            null
        }
    }

    fun findQuestionById(questionId: UUID): QuizQuestion? {
        return try {
            transaction {
                QuizQuestions.selectAll().where { QuizQuestions.id eq questionId }
                    .singleOrNull()?.toQuizQuestion()?.let { question ->
                        // Загружаем варианты ответов
                        val answers = findAnswersByQuestionId(questionId)
                        question.copy(answers = answers)
                    }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении вопроса: ${e.message}", e)
            null
        }
    }

    fun findQuestionsByQuizId(quizId: UUID): List<QuizQuestion> {
        return try {
            transaction {
                QuizQuestions.selectAll().where { QuizQuestions.quizId eq quizId }
                    .orderBy(QuizQuestions.order to SortOrder.ASC)
                    .map { row ->
                        val question = row.toQuizQuestion()
                        val answers = findAnswersByQuestionId(question.id)
                        question.copy(answers = answers)
                    }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении вопросов теста: ${e.message}", e)
            emptyList()
        }
    }

    fun updateQuestion(
        questionId: UUID,
        question: String? = null,
        questionType: QuestionType? = null,
        explanation: String? = null,
        points: Int? = null,
        order: Int? = null
    ): Boolean {
        return try {
            transaction {
                QuizQuestions.update({ QuizQuestions.id eq questionId }) { updateBuilder ->
                    question?.let { updateBuilder[QuizQuestions.question] = it }
                    questionType?.let { updateBuilder[QuizQuestions.questionType] = it.name }
                    explanation?.let { updateBuilder[QuizQuestions.explanation] = it }
                    points?.let { updateBuilder[QuizQuestions.points] = it }
                    order?.let { updateBuilder[QuizQuestions.order] = it }
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
                // Получаем quizId перед удалением
                val quizId = QuizQuestions.selectAll().where { QuizQuestions.id eq questionId }
                    .singleOrNull()?.get(QuizQuestions.quizId)

                // Удаляем варианты ответов
                QuizAnswers.deleteWhere { QuizAnswers.questionId eq questionId }

                // Удаляем вопрос
                val deleted = QuizQuestions.deleteWhere { QuizQuestions.id eq questionId } > 0

                // Обновляем счётчик
                quizId?.let { updateQuizQuestionsCount(it) }

                deleted
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении вопроса: ${e.message}", e)
            false
        }
    }

    // ==================== Варианты ответов ====================

    fun createAnswer(
        questionId: UUID,
        answerText: String,
        isCorrect: Boolean,
        order: Int
    ): QuizAnswer? {
        return try {
            transaction {
                val answerId = UUID.randomUUID()
                QuizAnswers.insert {
                    it[id] = answerId
                    it[QuizAnswers.questionId] = questionId
                    it[QuizAnswers.answerText] = answerText
                    it[QuizAnswers.isCorrect] = isCorrect
                    it[QuizAnswers.order] = order
                    it[createdAt] = Instant.now()
                }

                QuizAnswers.selectAll().where { QuizAnswers.id eq answerId }
                    .singleOrNull()?.toQuizAnswer()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании варианта ответа: ${e.message}", e)
            null
        }
    }

    fun createAnswers(questionId: UUID, answers: List<AnswerData>): List<QuizAnswer> {
        return try {
            transaction {
                answers.mapIndexedNotNull { index, answerData ->
                    val answerId = UUID.randomUUID()
                    QuizAnswers.insert {
                        it[id] = answerId
                        it[QuizAnswers.questionId] = questionId
                        it[answerText] = answerData.text
                        it[isCorrect] = answerData.isCorrect
                        it[order] = answerData.order ?: (index + 1)
                        it[createdAt] = Instant.now()
                    }

                    QuizAnswers.selectAll().where { QuizAnswers.id eq answerId }
                        .singleOrNull()?.toQuizAnswer()
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при создании вариантов ответов: ${e.message}", e)
            emptyList()
        }
    }

    fun findAnswersByQuestionId(questionId: UUID): List<QuizAnswer> {
        return try {
            transaction {
                QuizAnswers.selectAll().where { QuizAnswers.questionId eq questionId }
                    .orderBy(QuizAnswers.order to SortOrder.ASC)
                    .map { it.toQuizAnswer() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении вариантов ответов: ${e.message}", e)
            emptyList()
        }
    }

    fun findAnswerById(answerId: UUID): QuizAnswer? {
        return try {
            transaction {
                QuizAnswers.selectAll().where { QuizAnswers.id eq answerId }
                    .singleOrNull()?.toQuizAnswer()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении варианта ответа: ${e.message}", e)
            null
        }
    }

    fun findCorrectAnswers(questionId: UUID): List<QuizAnswer> {
        return try {
            transaction {
                QuizAnswers.selectAll().where {
                    (QuizAnswers.questionId eq questionId) and (QuizAnswers.isCorrect eq true)
                }.orderBy(QuizAnswers.order to SortOrder.ASC)
                    .map { it.toQuizAnswer() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при получении правильных ответов: ${e.message}", e)
            emptyList()
        }
    }

    fun updateAnswer(
        answerId: UUID,
        answerText: String? = null,
        isCorrect: Boolean? = null,
        order: Int? = null
    ): Boolean {
        return try {
            transaction {
                QuizAnswers.update({ QuizAnswers.id eq answerId }) { updateBuilder ->
                    answerText?.let { updateBuilder[QuizAnswers.answerText] = it }
                    isCorrect?.let { updateBuilder[QuizAnswers.isCorrect] = it }
                    order?.let { updateBuilder[QuizAnswers.order] = it }
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении варианта ответа: ${e.message}", e)
            false
        }
    }

    fun deleteAnswer(answerId: UUID): Boolean {
        return try {
            transaction {
                QuizAnswers.deleteWhere { QuizAnswers.id eq answerId } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении варианта ответа: ${e.message}", e)
            false
        }
    }

    fun deleteAnswersByQuestionId(questionId: UUID): Boolean {
        return try {
            transaction {
                QuizAnswers.deleteWhere { QuizAnswers.questionId eq questionId } >= 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при удалении вариантов ответов: ${e.message}", e)
            false
        }
    }

    // ==================== Проверка ответов ====================

    /**
     * Проверяет ответ пользователя на вопрос
     * @param questionId ID вопроса
     * @param selectedAnswerIds список ID выбранных пользователем ответов
     * @return результат проверки с правильными ответами и объяснением
     */
    fun checkAnswer(questionId: UUID, selectedAnswerIds: List<UUID>): AnswerCheckResultData? {
        return try {
            transaction {
                val question = QuizQuestions.selectAll().where { QuizQuestions.id eq questionId }
                    .singleOrNull()?.toQuizQuestion() ?: return@transaction null

                val allAnswers = findAnswersByQuestionId(questionId)
                val correctAnswers = allAnswers.filter { it.isCorrect }
                val correctAnswerIds = correctAnswers.map { it.id }.toSet()

                val isCorrect = when (QuestionType.fromString(question.questionType.name)) {
                    QuestionType.SINGLE_CHOICE -> {
                        // Для одиночного выбора: ровно один ответ и он правильный
                        selectedAnswerIds.size == 1 && selectedAnswerIds.first() in correctAnswerIds
                    }
                    QuestionType.MULTIPLE_CHOICE -> {
                        // Для множественного выбора: выбраны все правильные и только они
                        selectedAnswerIds.toSet() == correctAnswerIds
                    }
                    QuestionType.TEXT -> {
                        // Для текстового ответа - отдельная логика
                        false
                    }
                }

                AnswerCheckResultData(
                    questionId = questionId,
                    isCorrect = isCorrect,
                    correctAnswers = correctAnswers,
                    selectedAnswerIds = selectedAnswerIds,
                    explanation = question.explanation,
                    pointsEarned = if (isCorrect) question.points else 0,
                    maxPoints = question.points
                )
            }
        } catch (e: Exception) {
            logger.error("Ошибка при проверке ответа: ${e.message}", e)
            null
        }
    }

    // ==================== Вспомогательные методы ====================

    private fun updateQuizQuestionsCount(quizId: UUID) {
        val count = QuizQuestions.selectAll().where { QuizQuestions.quizId eq quizId }.count().toInt()
        Quizzes.update({ Quizzes.id eq quizId }) {
            it[questionsCount] = count
            it[updatedAt] = Instant.now()
        }
    }

    private fun ResultRow.toQuizQuestion(): QuizQuestion {
        return QuizQuestion(
            id = this[QuizQuestions.id],
            quizId = this[QuizQuestions.quizId],
            question = this[QuizQuestions.question],
            questionType = QuestionType.fromString(this[QuizQuestions.questionType]),
            explanation = this[QuizQuestions.explanation],
            points = this[QuizQuestions.points],
            order = this[QuizQuestions.order],
            createdAt = this[QuizQuestions.createdAt],
            updatedAt = this[QuizQuestions.updatedAt]
        )
    }

    private fun ResultRow.toQuizAnswer(): QuizAnswer {
        return QuizAnswer(
            id = this[QuizAnswers.id],
            questionId = this[QuizAnswers.questionId],
            answerText = this[QuizAnswers.answerText],
            isCorrect = this[QuizAnswers.isCorrect],
            order = this[QuizAnswers.order],
            createdAt = this[QuizAnswers.createdAt]
        )
    }
}

// Data classes для создания
data class AnswerData(
    val text: String,
    val isCorrect: Boolean,
    val order: Int? = null
)

data class AnswerCheckResultData(
    val questionId: UUID,
    val isCorrect: Boolean,
    val correctAnswers: List<QuizAnswer>,
    val selectedAnswerIds: List<UUID>,
    val explanation: String?,
    val pointsEarned: Int,
    val maxPoints: Int
)

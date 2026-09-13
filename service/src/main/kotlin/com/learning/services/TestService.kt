package com.learning.services

import com.learning.domain.models.QuestionType
import com.learning.domain.models.QuizAnswer
import com.learning.domain.models.QuizQuestion
import com.learning.models.*
import com.learning.repositories.AnswerData
import com.learning.repositories.QuizQuestionRepository
import com.learning.repositories.QuizRepository
import java.util.*

class TestService(
    private val quizRepository: QuizRepository,
    private val questionRepository: QuizQuestionRepository,
) {

    // ==================== Тесты ====================

    fun getAllTests(): List<TestResponse> {
        val quizzes = quizRepository.findPublished()
        return quizzes.map { it.toTestResponse() }
    }

    fun getTestById(id: String): TestResponse? {
        val quizId = UUID.fromString(id)
        val quiz = quizRepository.findById(quizId)
        return quiz?.toTestResponse()
    }

    fun createTest(request: TestRequest): TestResponse {
        val quiz = quizRepository.createQuiz(
            courseId = request.courseId?.let { UUID.fromString(it) },
            lessonId = request.lessonId?.let { UUID.fromString(it) },
            title = request.title,
            description = request.description,
            timeLimit = request.timeLimit ?: 30,
            passingScore = request.passingScore,
            isPublished = request.isActive
        ) ?: throw IllegalStateException("Не удалось создать тест")

        return quiz.toTestResponse()
    }

    fun updateTest(id: String, request: TestRequest): TestResponse {
        val quizId = UUID.fromString(id)
        
        quizRepository.updateQuiz(
            quizId = quizId,
            title = request.title,
            description = request.description,
            timeLimit = request.timeLimit,
            passingScore = request.passingScore,
            isPublished = request.isActive
        )
        
        return quizRepository.findById(quizId)?.toTestResponse()
            ?: throw IllegalStateException("Тест не найден")
    }

    fun deleteTest(id: String): Boolean {
        val quizId = UUID.fromString(id)
        return quizRepository.deleteQuiz(quizId)
    }

    // ==================== Вопросы ====================

    /**
     * Получить вопросы теста ДЛЯ ПОЛЬЗОВАТЕЛЯ (без правильных ответов)
     */
    fun getTestQuestionsForUser(testId: String): List<QuestionForUserResponse> {
        val quizId = UUID.fromString(testId)
        val questions = questionRepository.findQuestionsByQuizId(quizId)
        return questions.map { it.toQuestionForUserResponse() }
    }

    /**
     * Получить вопросы теста ДЛЯ АДМИНА (с правильными ответами)
     */
    fun getTestQuestions(testId: String): List<QuestionResponse> {
        val quizId = UUID.fromString(testId)
        val questions = questionRepository.findQuestionsByQuizId(quizId)
        return questions.map { it.toQuestionResponse() }
    }

    /**
     * Создать вопрос с вариантами ответов
     */
    fun createQuestion(testId: String, request: QuestionRequest): QuestionResponse {
        val quizId = UUID.fromString(testId)
        
        // Проверяем, что тест существует
        quizRepository.findById(quizId)
            ?: throw IllegalArgumentException("Тест не найден")

        // Валидация: должен быть хотя бы один правильный ответ
        if (request.questionType != "TEXT" && request.answers.isEmpty()) {
            throw IllegalArgumentException("Необходимо указать варианты ответов")
        }
        
        if (request.questionType != "TEXT" && request.answers.none { it.isCorrect }) {
            throw IllegalArgumentException("Необходимо указать хотя бы один правильный ответ")
        }

        // Создаём вопрос
        val question = questionRepository.createQuestion(
            quizId = quizId,
            question = request.questionText,
            questionType = QuestionType.fromString(request.questionType),
            explanation = request.explanation,
            points = request.points,
            order = request.orderIndex
        ) ?: throw IllegalStateException("Не удалось создать вопрос")

        // Создаём варианты ответов
        if (request.answers.isNotEmpty()) {
            val answerDataList = request.answers.mapIndexed { index, answer ->
                AnswerData(
                    text = answer.answerText,
                    isCorrect = answer.isCorrect,
                    order = answer.orderIndex ?: (index + 1)
                )
            }
            questionRepository.createAnswers(question.id, answerDataList)
        }

        // Возвращаем вопрос с ответами
        return questionRepository.findQuestionById(question.id)?.toQuestionResponse()
            ?: throw IllegalStateException("Не удалось получить созданный вопрос")
    }

    fun updateQuestion(questionId: String, request: QuestionRequest): QuestionResponse {
        val qId = UUID.fromString(questionId)
        
        questionRepository.updateQuestion(
            questionId = qId,
            question = request.questionText,
            questionType = QuestionType.fromString(request.questionType),
            explanation = request.explanation,
            points = request.points,
            order = request.orderIndex
        )

        // Обновляем варианты ответов: удаляем старые и создаём новые
        if (request.answers.isNotEmpty()) {
            questionRepository.deleteAnswersByQuestionId(qId)
            val answerDataList = request.answers.mapIndexed { index, answer ->
                AnswerData(
                    text = answer.answerText,
                    isCorrect = answer.isCorrect,
                    order = answer.orderIndex ?: (index + 1)
                )
            }
            questionRepository.createAnswers(qId, answerDataList)
        }

        return questionRepository.findQuestionById(qId)?.toQuestionResponse()
            ?: throw IllegalStateException("Вопрос не найден")
    }

    fun deleteQuestion(questionId: String): Boolean {
        val qId = UUID.fromString(questionId)
        return questionRepository.deleteQuestion(qId)
    }

    // ==================== Проверка ответов ====================

    /**
     * Проверить ответ пользователя на вопрос
     * Возвращает результат с объяснением
     */
    fun checkAnswer(request: CheckAnswerRequest): CheckAnswerResponse {
        val questionId = UUID.fromString(request.questionId)
        val selectedIds = request.selectedAnswerIds.map { UUID.fromString(it) }

        val result = questionRepository.checkAnswer(questionId, selectedIds)
            ?: throw IllegalArgumentException("Вопрос не найден")

        val message = if (result.isCorrect) {
            "✅ Верно! Отличная работа!"
        } else {
            "❌ Неверно. Попробуйте разобраться в объяснении."
        }

        return CheckAnswerResponse(
            questionId = result.questionId.toString(),
            isCorrect = result.isCorrect,
            message = message,
            explanation = result.explanation,
            correctAnswers = result.correctAnswers.map { 
                CorrectAnswerInfo(
                    id = it.id.toString(),
                    answerText = it.answerText
                )
            },
            pointsEarned = result.pointsEarned,
            maxPoints = result.maxPoints
        )
    }

    // ==================== Extension functions ====================
    
    private fun com.learning.domain.models.Quiz.toTestResponse(): TestResponse {
        return TestResponse(
            id = this.id.toString(),
            courseId = this.courseId?.toString(),
            lessonId = this.lessonId?.toString(),
            title = this.title,
            description = this.description ?: "",
            passingScore = this.passingScore,
            timeLimit = this.timeLimit,
            questionsCount = this.questionsCount,
            isActive = this.isPublished
        )
    }

    private fun QuizQuestion.toQuestionResponse(): QuestionResponse {
        return QuestionResponse(
            id = this.id.toString(),
            questionText = this.question,
            questionType = this.questionType.name,
            points = this.points,
            orderIndex = this.order,
            explanation = this.explanation,
            answers = this.answers.map { it.toAnswerResponse() }
        )
    }

    private fun QuizQuestion.toQuestionForUserResponse(): QuestionForUserResponse {
        return QuestionForUserResponse(
            id = this.id.toString(),
            questionText = this.question,
            questionType = this.questionType.name,
            points = this.points,
            orderIndex = this.order,
            answers = this.answers.map { it.toAnswerForUserResponse() }
        )
    }

    private fun QuizAnswer.toAnswerResponse(): AnswerResponse {
        return AnswerResponse(
            id = this.id.toString(),
            answerText = this.answerText,
            isCorrect = this.isCorrect,
            orderIndex = this.order
        )
    }

    private fun QuizAnswer.toAnswerForUserResponse(): AnswerForUserResponse {
        return AnswerForUserResponse(
            id = this.id.toString(),
            answerText = this.answerText,
            orderIndex = this.order
        )
    }
}

package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.security.withRole
import io.ktor.server.auth.*
import com.learning.models.*
import com.learning.services.TestService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.testRoutes(testService: TestService) {

    route("/tests") {
        // Получить все тесты
        get {
            try {
                val tests = testService.getAllTests()
                call.respond(TestsResponse(tests = tests))
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении тестов",
                        details = e.message
                    )
                )
            }
        }

        // Получить тест по ID
        get("/{id}") {
            try {
                val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                val test = testService.getTestById(testId)

                if (test != null) {
                    call.respond(test)
                } else {
                    call.respond(
                        status = HttpStatusCode.NotFound,
                        message = ErrorResponse(
                            error = "TEST_NOT_FOUND",
                            message = "Тест не найден"
                        )
                    )
                }
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "INVALID_REQUEST",
                        message = e.message ?: "Неверный запрос"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении теста",
                        details = e.message
                    )
                )
            }
        }

        // Получить вопросы теста ДЛЯ ПОЛЬЗОВАТЕЛЯ (без правильных ответов!)
        get("/{id}/questions") {
            try {
                val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                val questions = testService.getTestQuestionsForUser(testId)
                call.respond(mapOf("questions" to questions))
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "INVALID_REQUEST",
                        message = e.message ?: "Неверный запрос"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении вопросов теста",
                        details = e.message
                    )
                )
            }
        }

        // ==================== Управление вопросами ====================

        // ==================== ПРОВЕРКА ОТВЕТА ====================

        /**
         * Проверить ответ пользователя
         * 
         * POST /api/v1/tests/check-answer
         * 
         * Request:
         * {
         *   "questionId": "uuid",
         *   "selectedAnswerIds": ["uuid1", "uuid2"]
         * }
         * 
         * Response:
         * {
         *   "questionId": "uuid",
         *   "isCorrect": true/false,
         *   "message": "✅ Верно! Отличная работа!",
         *   "explanation": "Развёрнутое объяснение...",
         *   "correctAnswers": [
         *     { "id": "uuid", "answerText": "Правильный ответ" }
         *   ],
         *   "pointsEarned": 1,
         *   "maxPoints": 1
         * }
         */
        post("/check-answer") {
            try {
                val request = call.receive<CheckAnswerRequest>()
                
                // Валидация
                if (request.questionId.isBlank()) {
                    throw IllegalArgumentException("ID вопроса не указан")
                }
                if (request.selectedAnswerIds.isEmpty()) {
                    throw IllegalArgumentException("Не выбрано ни одного ответа")
                }

                val result = testService.checkAnswer(request)
                call.respond(result)
                
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "VALIDATION_ERROR",
                        message = e.message ?: "Неверный запрос"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при проверке ответа",
                        details = e.message
                    )
                )
            }
        }

        // ==================== Отправка всего теста ====================

        post("/{id}/submit") {
            try {
                val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                val request = call.receive<TestSubmissionRequest>()

                // Проверяем каждый ответ
                var totalPoints = 0
                var maxPoints = 0
                var correctCount = 0

                request.answers.forEach { answer ->
                    val checkRequest = CheckAnswerRequest(
                        questionId = answer.questionId,
                        selectedAnswerIds = answer.answerIds
                    )
                    val result = testService.checkAnswer(checkRequest)
                    totalPoints += result.pointsEarned
                    maxPoints += result.maxPoints
                    if (result.isCorrect) correctCount++
                }

                val score = if (maxPoints > 0) (totalPoints * 100 / maxPoints) else 0
                val test = testService.getTestById(testId)
                val passed = score >= (test?.passingScore ?: 70)

                val response = TestSubmissionResponse(
                    sessionId = java.util.UUID.randomUUID().toString(),
                    score = score,
                    totalQuestions = request.answers.size,
                    correctAnswers = correctCount,
                    passed = passed,
                    completedAt = System.currentTimeMillis()
                )

                call.respond(response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "INVALID_REQUEST",
                        message = e.message ?: "Неверный запрос"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при отправке ответов",
                        details = e.message
                    )
                )
            }
        }

        // Запись — только администратор: без токена 401, без роли 403
        authenticate("auth-jwt") {
            withRole(Roles.ADMIN) {
                // Получить вопросы теста ДЛЯ АДМИНА (с правильными ответами)
                get("/{id}/questions/admin") {
                    try {
                        val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                        val questions = testService.getTestQuestions(testId)
                        call.respond(QuestionsResponse(questions = questions))
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "INVALID_REQUEST",
                                message = e.message ?: "Неверный запрос"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при получении вопросов теста",
                                details = e.message
                            )
                        )
                    }
                }

                // Создать тест
                post {
                    try {
                        val request = call.receive<TestRequest>()
                        val test = testService.createTest(request)
                        call.respond(
                            status = HttpStatusCode.Created,
                            message = test
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при создании теста",
                                details = e.message
                            )
                        )
                    }
                }

                // Обновить тест
                put("/{id}") {
                    try {
                        val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                        val request = call.receive<TestRequest>()
                        val test = testService.updateTest(testId, request)
                        call.respond(test)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при обновлении теста",
                                details = e.message
                            )
                        )
                    }
                }

                // Удалить тест
                delete("/{id}") {
                    try {
                        val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                        val deleted = testService.deleteTest(testId)
                        if (deleted) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message = ErrorResponse(
                                    error = "TEST_NOT_FOUND",
                                    message = "Тест не найден"
                                )
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "INVALID_REQUEST",
                                message = e.message ?: "Неверный запрос"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при удалении теста",
                                details = e.message
                            )
                        )
                    }
                }

                // Создать вопрос с вариантами ответов
                post("/{id}/questions") {
                    try {
                        val testId = call.parameters["id"] ?: throw IllegalArgumentException("ID теста не указан")
                        val request = call.receive<QuestionRequest>()
                        val question = testService.createQuestion(testId, request)
                        call.respond(
                            status = HttpStatusCode.Created,
                            message = question
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при создании вопроса",
                                details = e.message
                            )
                        )
                    }
                }

                // Обновить вопрос
                put("/questions/{questionId}") {
                    try {
                        val questionId = call.parameters["questionId"] ?: throw IllegalArgumentException("ID вопроса не указан")
                        val request = call.receive<QuestionRequest>()
                        val question = testService.updateQuestion(questionId, request)
                        call.respond(question)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при обновлении вопроса",
                                details = e.message
                            )
                        )
                    }
                }

                // Удалить вопрос
                delete("/questions/{questionId}") {
                    try {
                        val questionId = call.parameters["questionId"] ?: throw IllegalArgumentException("ID вопроса не указан")
                        val deleted = testService.deleteQuestion(questionId)
                        if (deleted) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message = ErrorResponse(
                                    error = "QUESTION_NOT_FOUND",
                                    message = "Вопрос не найден"
                                )
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "INVALID_REQUEST",
                                message = e.message ?: "Неверный запрос"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при удалении вопроса",
                                details = e.message
                            )
                        )
                    }
                }
            }
        }
    }
}

package com.learning.services

import com.learning.models.*
import com.learning.repositories.InterviewRepository
import java.util.*

/** Вопросы собеседования. Реализация перенесена с ветки `last-checkpoints` (04df904). */
class InterviewService(
    private val interviewRepository: InterviewRepository
) {

    fun getInterviewQuestions(page: Int = 1, limit: Int = 10, category: String? = null): InterviewQuestionsResponse {
        TODO("Расширение, глава 4, урок 17: InterviewService.getInterviewQuestions")
    }

    fun getInterviewQuestionById(id: String): InterviewQuestionResponse? {
        TODO("Расширение, глава 4, урок 17: InterviewService.getInterviewQuestionById")
    }

    fun createInterviewQuestion(request: InterviewQuestionRequest): InterviewQuestionResponse {
        TODO("Расширение, глава 4, урок 17: InterviewService.createInterviewQuestion")
    }

    fun updateInterviewQuestion(id: String, request: InterviewQuestionRequest): InterviewQuestionResponse {
        TODO("Расширение, глава 4, урок 17: InterviewService.updateInterviewQuestion")
    }

    fun deleteInterviewQuestion(id: String): Boolean {
        TODO("Расширение, глава 4, урок 17: InterviewService.deleteInterviewQuestion")
    }

    fun submitInterviewAnswer(request: InterviewAnswerRequest): InterviewAnswerResponse {
        TODO("Расширение, глава 4, урок 17: InterviewService.submitInterviewAnswer")
    }

    companion object {
        const val MAX_LIMIT = 100
    }

    private fun com.learning.domain.models.InterviewQuestion.toResponse() = InterviewQuestionResponse(
        id = id.toString(),
        category = category,
        questionText = question,
        answer = answer.ifBlank { null },
        difficulty = difficulty,
        tags = tags,
        status = if (isPublished) "ACTIVE" else "DRAFT",
        createdAt = createdAt.toString(),
        answeredAt = if (answer.isNotBlank()) updatedAt.toString() else null
    )
}

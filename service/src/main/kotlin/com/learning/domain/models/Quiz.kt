package com.learning.domain.models

import java.time.Instant
import java.util.*

data class Quiz(
    val id: UUID,
    val courseId: UUID? = null,
    val lessonId: UUID? = null,
    val title: String,
    val description: String? = null,
    val timeLimit: Int, // in minutes
    val passingScore: Int, // percentage
    val questionsCount: Int = 0,
    val isPublished: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val formattedTimeLimit: String
        get() = when {
            timeLimit < 60 -> "${timeLimit} мин"
            timeLimit < 1440 -> "${timeLimit / 60} ч ${timeLimit % 60} мин"
            else -> "${timeLimit / 1440} дн ${(timeLimit % 1440) / 60} ч"
        }
    
    val isPassingScoreValid: Boolean
        get() = passingScore in 1..100
}

data class QuizQuestion(
    val id: UUID,
    val quizId: UUID,
    val question: String,
    val questionType: QuestionType,
    val explanation: String? = null, // Развёрнутое объяснение правильного ответа
    val points: Int = 1,
    val order: Int,
    val answers: List<QuizAnswer> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class QuizAnswer(
    val id: UUID,
    val questionId: UUID,
    val answerText: String,
    val isCorrect: Boolean = false,
    val order: Int,
    val createdAt: Instant = Instant.now()
)

enum class QuestionType {
    SINGLE_CHOICE,    // Один правильный ответ из нескольких
    MULTIPLE_CHOICE,  // Несколько правильных ответов
    TEXT;             // Свободный текстовый ответ
    
    companion object {
        fun fromString(value: String): QuestionType {
            return when (value.uppercase()) {
                "SINGLE_CHOICE", "SINGLE" -> SINGLE_CHOICE
                "MULTIPLE_CHOICE", "MULTIPLE" -> MULTIPLE_CHOICE
                "TEXT", "FREE_TEXT" -> TEXT
                else -> SINGLE_CHOICE
            }
        }
    }
}

/**
 * Результат проверки ответа пользователя
 */
data class AnswerCheckResult(
    val questionId: UUID,
    val isCorrect: Boolean,
    val correctAnswers: List<QuizAnswer>,
    val userAnswers: List<UUID>,
    val explanation: String?,
    val pointsEarned: Int,
    val maxPoints: Int
)

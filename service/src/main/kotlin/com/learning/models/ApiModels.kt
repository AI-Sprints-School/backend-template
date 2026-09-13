package com.learning.models

import kotlinx.serialization.Serializable

// Auth Models
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 3600,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePhoto: String? = null,
    val emailVerified: Boolean = false
)

@Serializable
data class MessageResponse(
    val message: String
)

// Course Models
@Serializable
data class CourseRequest(
    val title: String,
    val description: String,
    val coverImage: String? = null,
    val category: String,
    val difficulty: String,
    val duration: Int,
    val price: Double,
    val isPremium: Boolean = false,
    /** null — при создании черновик, при обновлении не менять */
    val isPublished: Boolean? = null
)

@Serializable
data class CourseResponse(
    val id: String,
    val title: String,
    val description: String,
    val coverImage: String?,
    val category: String,
    val difficulty: String,
    val duration: Int,
    val lessonsCount: Int,
    val rating: Double,
    val studentsCount: Int,
    val price: Double,
    val isPremium: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CourseListResponse(
    val data: List<CourseResponse>,
    val pagination: PaginationResponse
)

@Serializable
data class PaginationResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

// Lesson Models
@Serializable
data class LessonRequest(
    val courseId: String,
    val title: String,
    val description: String? = null,
    val orderIndex: Int,
    val duration: Int,
    val videoUrl: String? = null,
    val isPreview: Boolean = false,
    /** null — при создании черновик, при обновлении не менять */
    val isPublished: Boolean? = null
)

@Serializable
data class LessonResponse(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String?,
    val orderIndex: Int,
    val duration: Int,
    val videoUrl: String?,
    val isPreview: Boolean,
    val contents: List<LessonContentResponse> = emptyList()
)

@Serializable
data class LessonContentResponse(
    val id: String,
    val contentType: String,
    val content: String,
    val orderIndex: Int
)

@Serializable
data class LessonContentsResponse(
    val contents: List<LessonContentResponse>
)

// Test Models
@Serializable
data class TestRequest(
    val courseId: String? = null,
    val lessonId: String? = null,
    val title: String,
    val description: String? = null,
    val passingScore: Int,
    val timeLimit: Int? = null,
    val isActive: Boolean = true
)

@Serializable
data class TestResponse(
    val id: String,
    val courseId: String?,
    val lessonId: String?,
    val title: String,
    val description: String?,
    val passingScore: Int,
    val timeLimit: Int?,
    val questionsCount: Int,
    val isActive: Boolean
)

@Serializable
data class QuestionRequest(
    val testId: String,
    val questionText: String,
    val questionType: String, // SINGLE_CHOICE, MULTIPLE_CHOICE, TEXT
    val points: Int = 1,
    val orderIndex: Int,
    val explanation: String? = null, // Развёрнутое объяснение правильного ответа
    val answers: List<AnswerRequest> = emptyList() // Варианты ответов
)

@Serializable
data class AnswerRequest(
    val answerText: String,
    val isCorrect: Boolean = false,
    val orderIndex: Int? = null
)

@Serializable
data class QuestionResponse(
    val id: String,
    val questionText: String,
    val questionType: String,
    val points: Int,
    val orderIndex: Int,
    val explanation: String?, // Показывается после ответа
    val answers: List<AnswerResponse> = emptyList()
)

@Serializable
data class AnswerResponse(
    val id: String,
    val answerText: String,
    val isCorrect: Boolean, // В ответе пользователю НЕ показываем до проверки!
    val orderIndex: Int
)

// Запрос на проверку ответа
@Serializable
data class CheckAnswerRequest(
    val questionId: String,
    val selectedAnswerIds: List<String> // ID выбранных пользователем ответов
)

// Ответ после проверки
@Serializable
data class CheckAnswerResponse(
    val questionId: String,
    val isCorrect: Boolean,
    val message: String, // "Верно! 🎉" или "Неверно 😔"
    val explanation: String?, // Развёрнутое объяснение
    val correctAnswers: List<CorrectAnswerInfo>, // Правильные ответы
    val pointsEarned: Int,
    val maxPoints: Int
)

@Serializable
data class CorrectAnswerInfo(
    val id: String,
    val answerText: String
)

// Ответ с вариантами для отображения (без isCorrect)
@Serializable
data class QuestionForUserResponse(
    val id: String,
    val questionText: String,
    val questionType: String,
    val points: Int,
    val orderIndex: Int,
    val answers: List<AnswerForUserResponse> // Без isCorrect!
)

@Serializable
data class AnswerForUserResponse(
    val id: String,
    val answerText: String,
    val orderIndex: Int
)

// Sprint Models
@Serializable
data class SprintRequest(
    val title: String,
    val description: String? = null,
    val coverImage: String? = null,
    val discount: Int? = null,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean = true
)

@Serializable
data class SprintResponse(
    val id: String,
    val title: String,
    val description: String?,
    val coverImage: String?,
    val discount: Int?,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean,
    val courses: List<CourseResponse> = emptyList()
)

// Interview Models
@Serializable
data class InterviewQuestionResponse(
    val id: String,
    val category: String,
    val questionText: String,
    val answer: String?,
    val difficulty: String,
    val tags: List<String> = emptyList(),
    val status: String,
    val createdAt: String,
    val answeredAt: String?
)

@Serializable
data class InterviewQuestionRequest(
    val category: String,
    val questionText: String,
    val difficulty: String,
    val tags: List<String> = emptyList()
)

// Roadmap Models
@Serializable
data class RoadmapRequest(
    val title: String,
    val description: String?,
    val category: String,
    val stages: List<RoadmapStageRequest> = emptyList()
)

@Serializable
data class RoadmapStageRequest(
    val title: String,
    val description: String?,
    val orderIndex: Int,
    val estimatedDuration: Int,
    val courseIds: List<String> = emptyList()
)

@Serializable
data class RoadmapResponse(
    val id: String,
    val title: String,
    val description: String?,
    val category: String,
    val stages: List<RoadmapStageResponse> = emptyList()
)

@Serializable
data class RoadmapStageResponse(
    val id: String,
    val title: String,
    val description: String?,
    val orderIndex: Int,
    val estimatedDuration: Int,
    val courses: List<CourseResponse> = emptyList()
)

// Additional Models for Routes
@Serializable
data class CoursesResponse(
    val courses: List<CourseResponse>
)

@Serializable
data class LessonsResponse(
    val lessons: List<LessonResponse>
)

@Serializable
data class TestsResponse(
    val tests: List<TestResponse>
)

@Serializable
data class QuestionsResponse(
    val questions: List<QuestionResponse>
)

@Serializable
data class SprintsResponse(
    val sprints: List<SprintResponse>
)

@Serializable
data class InterviewQuestionsResponse(
    val questions: List<InterviewQuestionResponse>,
    val pagination: PaginationResponse
)

@Serializable
data class InterviewAnswerRequest(
    val questionId: String,
    val answer: String
)

@Serializable
data class InterviewAnswerResponse(
    val id: String,
    val questionId: String,
    val answer: String,
    val submittedAt: Long,
    val feedback: String,
    val status: String
)

@Serializable
data class TestSubmissionRequest(
    val answers: List<TestAnswerRequest>
)

@Serializable
data class TestAnswerRequest(
    val questionId: String,
    val answerIds: List<String>
)

@Serializable
data class TestSubmissionResponse(
    val sessionId: String,
    val score: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val passed: Boolean,
    val completedAt: Long
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatar: String?,
    val level: String,
    val experience: Int,
    val completedCourses: Int,
    val completedTests: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val joinDate: Long,
    val lastActivity: Long,
    val role: String
)

@Serializable
data class UpdateUserProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val avatar: String?
)

@Serializable
data class CourseCompletionProgress(
    val courseId: String,
    val completedLessons: Int,
    val totalLessons: Int,
    val courseCompletionPercentage: Int,
    val courseCompleted: Boolean
)

@Serializable
data class LessonCompletionResponse(
    val message: String,
    val progress: CourseCompletionProgress
)

@Serializable
data class UserProgressResponse(
    val userId: String,
    val totalCourses: Int,
    val completedCourses: Int,
    val inProgressCourses: Int,
    val totalLessons: Int,
    val completedLessons: Int,
    val totalTests: Int,
    val completedTests: Int,
    val averageScore: Double,
    val totalStudyTime: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val achievements: List<String>
)

@Serializable
data class SprintCourseResponse(
    val courseId: String,
    val courseTitle: String,
    val orderIndex: Int
)

@Serializable
data class RoadmapStageCourseResponse(
    val courseId: String,
    val courseTitle: String,
    val orderIndex: Int
)

// Common Models
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val details: String? = null
)

// Email Verification & Password Reset Models
@Serializable
data class VerifyEmailRequest(
    val token: String
)

@Serializable
data class PasswordResetRequestModel(
    val email: String
)

@Serializable
data class PasswordResetConfirmRequest(
    val token: String,
    val newPassword: String
)

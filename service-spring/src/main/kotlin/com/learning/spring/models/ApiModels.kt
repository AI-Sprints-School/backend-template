package com.learning.spring.models

// Контракт API — копия моделей Ktor-версии (service/src/main/kotlin/com/learning/models/ApiModels.kt)
// без @Serializable: JSON здесь пишет Jackson. Готово в шаблоне, править не нужно.

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 3600,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePhoto: String? = null,
    val emailVerified: Boolean = false
)

data class MessageResponse(
    val message: String
)

data class CourseRequest(
    val title: String,
    val description: String,
    val coverImage: String? = null,
    val category: String,
    val difficulty: String,
    val duration: Int,
    val price: Double,
    val isPremium: Boolean = false,
    /** null: при создании черновик, при обновлении не менять */
    val isPublished: Boolean? = null
)

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

data class CourseListResponse(
    val data: List<CourseResponse>,
    val pagination: PaginationResponse
)

data class PaginationResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

data class LessonRequest(
    val courseId: String,
    val title: String,
    val description: String? = null,
    val orderIndex: Int,
    val duration: Int,
    val videoUrl: String? = null,
    val isPreview: Boolean = false,
    /** null: при создании черновик, при обновлении не менять */
    val isPublished: Boolean? = null
)

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

data class LessonContentResponse(
    val id: String,
    val contentType: String,
    val content: String,
    val orderIndex: Int
)

data class CourseCompletionProgress(
    val courseId: String,
    val completedLessons: Int,
    val totalLessons: Int,
    val courseCompletionPercentage: Int,
    val courseCompleted: Boolean
)

data class LessonCompletionResponse(
    val message: String,
    val progress: CourseCompletionProgress
)

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

data class ErrorResponse(
    val error: String,
    val message: String,
    val details: String? = null
)

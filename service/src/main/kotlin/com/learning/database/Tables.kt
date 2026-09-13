package com.learning.database

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Users : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 255).uniqueIndex("users_email_unique")
    val passwordHash = varchar("password_hash", 255)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val isEmailVerified = bool("is_email_verified").default(false)
    val emailVerificationToken = varchar("email_verification_token", 255).nullable()
    val passwordResetToken = varchar("password_reset_token", 255).nullable()
    val passwordResetExpires = timestamp("password_reset_expires").nullable()
    val googleId = varchar("google_id", 255).nullable().uniqueIndex("users_google_id_unique")
    val avatarUrl = text("avatar_url").nullable()
    val role = varchar("role", 20).default("student")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_users_email", false, email)
        index("idx_users_google_id", false, googleId)
    }
}

object Courses : Table("courses") {
    val id = javaUUID("id")
    val title = varchar("title", 255)
    val description = text("description")
    val shortDescription = varchar("short_description", 500).nullable()
    val thumbnailUrl = text("thumbnail_url").nullable()
    val price = decimal("price", 10, 2)
    val originalPrice = decimal("original_price", 10, 2).nullable()
    val duration = integer("duration") // in minutes
    val difficulty = varchar("difficulty", 20) // beginner, intermediate, advanced
    val isPublished = bool("is_published").default(false)
    val lessonsCount = integer("lessons_count").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_courses_difficulty", false, difficulty)
        index("idx_courses_is_published", false, isPublished)
    }
}

object Lessons : Table("lessons") {
    val id = javaUUID("id")
    val courseId = reference("course_id", Courses.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val content = text("content")
    val videoUrl = text("video_url").nullable()
    val duration = integer("duration") // in minutes
    val order = integer("order")
    val isPublished = bool("is_published").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_lessons_course_id", false, courseId)
        index("idx_lessons_order", false, courseId, order)
    }
}

object Quizzes : Table("quizzes") {
    val id = javaUUID("id")
    val courseId = reference("course_id", Courses.id).nullable()
    val lessonId = reference("lesson_id", Lessons.id).nullable()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val timeLimit = integer("time_limit") // in minutes
    val passingScore = integer("passing_score") // percentage
    val questionsCount = integer("questions_count").default(0)
    val isPublished = bool("is_published").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_quizzes_course_id", false, courseId)
        index("idx_quizzes_lesson_id", false, lessonId)
        index("idx_quizzes_is_published", false, isPublished)
    }
}

object QuizQuestions : Table("quiz_questions") {
    val id = javaUUID("id")
    val quizId = reference("quiz_id", Quizzes.id)
    val question = text("question")
    val questionType = varchar("question_type", 20) // single_choice, multiple_choice, text
    val explanation = text("explanation").nullable() // Развёрнутое объяснение правильного ответа
    val points = integer("points").default(1)
    val order = integer("order")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_quiz_questions_quiz_id", false, quizId)
        index("idx_quiz_questions_order", false, quizId, order)
    }
}

object QuizAnswers : Table("quiz_answers") {
    val id = javaUUID("id")
    val questionId = reference("question_id", QuizQuestions.id)
    val answerText = text("answer_text")
    val isCorrect = bool("is_correct").default(false)
    val order = integer("order")
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_quiz_answers_question_id", false, questionId)
        index("idx_quiz_answers_order", false, questionId, order)
    }
}

object Sprints : Table("sprints") {
    val id = javaUUID("id")
    val title = varchar("title", 255)
    val description = text("description")
    val discountPercentage = integer("discount_percentage")
    val startDate = timestamp("start_date")
    val endDate = timestamp("end_date")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_sprints_is_active", false, isActive)
        index("idx_sprints_dates", false, startDate, endDate)
    }
}

object SprintCourses : Table("sprint_courses") {
    val id = javaUUID("id")
    val sprintId = reference("sprint_id", Sprints.id)
    val courseId = reference("course_id", Courses.id)
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_sprint_courses_sprint_id", false, sprintId)
        index("idx_sprint_courses_course_id", false, courseId)
        uniqueIndex("sprint_courses_sprint_id_course_id_key", sprintId, courseId)
    }
}

object InterviewQuestions : Table("interview_questions") {
    val id = javaUUID("id")
    val question = text("question")
    val answer = text("answer")
    val category = varchar("category", 100)
    val difficulty = varchar("difficulty", 20) // easy, medium, hard
    val tags = text("tags").nullable() // JSON array
    val isPublished = bool("is_published").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_interview_questions_category", false, category)
        index("idx_interview_questions_difficulty", false, difficulty)
        index("idx_interview_questions_is_published", false, isPublished)
    }
}

object Roadmaps : Table("roadmaps") {
    val id = javaUUID("id")
    val title = varchar("title", 255)
    val description = text("description")
    val category = varchar("category", 100)
    val isPublished = bool("is_published").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_roadmaps_category", false, category)
        index("idx_roadmaps_is_published", false, isPublished)
    }
}

object RoadmapSteps : Table("roadmap_steps") {
    val id = javaUUID("id")
    val roadmapId = reference("roadmap_id", Roadmaps.id)
    val title = varchar("title", 255)
    val description = text("description")
    val order = integer("order")
    val courseId = reference("course_id", Courses.id).nullable()
    val isCompleted = bool("is_completed").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_roadmap_steps_roadmap_id", false, roadmapId)
        index("idx_roadmap_steps_order", false, roadmapId, order)
    }
}

object UserProgress : Table("user_progress") {
    val id = javaUUID("id")
    val userId = reference("user_id", Users.id)
    val courseId = reference("course_id", Courses.id).nullable()
    val lessonId = reference("lesson_id", Lessons.id).nullable()
    val quizId = reference("quiz_id", Quizzes.id).nullable()
    val progressType = varchar("progress_type", 20) // lesson, quiz, course
    val isCompleted = bool("is_completed").default(false)
    val score = integer("score").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_user_progress_user_id", false, userId)
        index("idx_user_progress_course_id", false, courseId)
        index("idx_user_progress_lesson_id", false, lessonId)
        index("idx_user_progress_quiz_id", false, quizId)
    }
}

object UserQuizSessions : Table("user_quiz_sessions") {
    val id = javaUUID("id")
    val userId = reference("user_id", Users.id)
    val quizId = reference("quiz_id", Quizzes.id)
    val status = varchar("status", 20) // started, completed, abandoned
    val score = integer("score").nullable()
    val totalQuestions = integer("total_questions")
    val correctAnswers = integer("correct_answers").default(0)
    val startedAt = timestamp("started_at").default(Instant.now())
    val completedAt = timestamp("completed_at").nullable()
    val timeSpent = integer("time_spent").nullable() // in seconds

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_user_quiz_sessions_user_id", false, userId)
        index("idx_user_quiz_sessions_quiz_id", false, quizId)
        index("idx_user_quiz_sessions_status", false, status)
    }
}

object UserQuizAnswers : Table("user_quiz_answers") {
    val id = javaUUID("id")
    val sessionId = reference("session_id", UserQuizSessions.id)
    val questionId = reference("question_id", QuizQuestions.id)
    val answer = text("answer")
    val isCorrect = bool("is_correct")
    val answeredAt = timestamp("answered_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_user_quiz_answers_session_id", false, sessionId)
        index("idx_user_quiz_answers_question_id", false, questionId)
    }
}

object EmailVerificationTokens : Table("email_verification_tokens") {
    val id = javaUUID("id")
    val userId = reference("user_id", Users.id)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

object PasswordResetTokens : Table("password_reset_tokens") {
    val id = javaUUID("id")
    val userId = reference("user_id", Users.id)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokens : Table("refresh_tokens") {
    val id = javaUUID("id")
    val userId = reference("user_id", Users.id)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val isRevoked = bool("is_revoked").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

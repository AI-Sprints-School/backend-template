package com.learning

import org.junit.jupiter.api.Tag
import com.learning.models.CourseRequest
import com.learning.models.InterviewQuestionRequest
import com.learning.models.LoginRequest
import com.learning.models.RegisterRequest
import com.learning.models.SprintRequest
import com.learning.models.TestRequest
import com.learning.validation.Validators
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Тесты валидации моделей
 */
class ValidationTest {

    // ==================== Auth Validation Tests ====================

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `register request with valid data should pass validation`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "Password123",
            firstName = "Иван",
            lastName = "Иванов"
        )

        val result = Validators.registerRequestValidator.validate(request)
        assertTrue(result is Valid, "Valid registration data should pass validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `register request with invalid email should fail validation`() {
        val request = RegisterRequest(
            email = "invalid-email",
            password = "Password123",
            firstName = "Иван",
            lastName = "Иванов"
        )

        val result = Validators.registerRequestValidator.validate(request)
        assertTrue(result is Invalid, "Invalid email should fail validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `register request with weak password should fail validation`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "weak",
            firstName = "Иван",
            lastName = "Иванов"
        )

        val result = Validators.registerRequestValidator.validate(request)
        assertTrue(result is Invalid, "Weak password should fail validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `register request with short first name should fail validation`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "Password123",
            firstName = "A",
            lastName = "Иванов"
        )

        val result = Validators.registerRequestValidator.validate(request)
        assertTrue(result is Invalid, "Short first name should fail validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `login request with valid data should pass validation`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "Password123"
        )

        val result = Validators.loginRequestValidator.validate(request)
        assertTrue(result is Valid, "Valid login data should pass validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `login request with invalid email should fail validation`() {
        val request = LoginRequest(
            email = "not-an-email",
            password = "Password123"
        )

        val result = Validators.loginRequestValidator.validate(request)
        assertTrue(result is Invalid, "Invalid email should fail validation")
    }

    // ==================== Course Validation Tests ====================

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `course request with valid data should pass validation`() {
        val request = CourseRequest(
            title = "Kotlin для начинающих",
            description = "Полный курс по языку программирования Kotlin для начинающих разработчиков",
            category = "Programming",
            difficulty = "beginner",
            duration = 120,
            price = 1999.99
        )

        val result = Validators.courseRequestValidator.validate(request)
        assertTrue(result is Valid, "Valid course data should pass validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `course request with short title should fail validation`() {
        val request = CourseRequest(
            title = "Ab",
            description = "Полный курс по языку программирования Kotlin",
            category = "Programming",
            difficulty = "beginner",
            duration = 120,
            price = 1999.99
        )

        val result = Validators.courseRequestValidator.validate(request)
        assertTrue(result is Invalid, "Short title should fail validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `course request with invalid difficulty should fail validation`() {
        val request = CourseRequest(
            title = "Kotlin для начинающих",
            description = "Полный курс по языку программирования Kotlin",
            category = "Programming",
            difficulty = "ultra-hard",
            duration = 120,
            price = 1999.99
        )

        val result = Validators.courseRequestValidator.validate(request)
        assertTrue(result is Invalid, "Invalid difficulty should fail validation")
    }

    @Test
    @Tag("core")
    @Tag("chapter2")
    fun `course request with negative price should fail validation`() {
        val request = CourseRequest(
            title = "Kotlin для начинающих",
            description = "Полный курс по языку программирования Kotlin",
            category = "Programming",
            difficulty = "beginner",
            duration = 120,
            price = -100.0
        )

        val result = Validators.courseRequestValidator.validate(request)
        assertTrue(result is Invalid, "Negative price should fail validation")
    }

    // ==================== Test Validation Tests ====================

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `test request with valid data should pass validation`() {
        val request = TestRequest(
            title = "Kotlin Basics Test",
            description = "Test your knowledge of Kotlin basics",
            passingScore = 70,
            timeLimit = 30
        )

        val result = Validators.testRequestValidator.validate(request)
        assertTrue(result is Valid, "Valid test data should pass validation")
    }

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `test request with invalid passing score should fail validation`() {
        val request = TestRequest(
            title = "Kotlin Basics Test",
            description = "Test your knowledge of Kotlin basics",
            passingScore = 150,
            timeLimit = 30
        )

        val result = Validators.testRequestValidator.validate(request)
        assertTrue(result is Invalid, "Invalid passing score should fail validation")
    }

    // ==================== Sprint Validation Tests ====================

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `sprint request with valid data should pass validation`() {
        val request = SprintRequest(
            title = "Android Development Sprint",
            description = "Intensive sprint for Android development",
            discount = 20,
            startDate = "2025-10-01T00:00:00Z",
            endDate = "2025-10-31T23:59:59Z"
        )

        val result = Validators.sprintRequestValidator.validate(request)
        assertTrue(result is Valid, "Valid sprint data should pass validation")
    }

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `sprint request with invalid discount should fail validation`() {
        val request = SprintRequest(
            title = "Android Development Sprint",
            description = "Intensive sprint for Android development",
            discount = 150,
            startDate = "2025-10-01T00:00:00Z",
            endDate = "2025-10-31T23:59:59Z"
        )

        val result = Validators.sprintRequestValidator.validate(request)
        assertTrue(result is Invalid, "Invalid discount should fail validation")
    }

    // ==================== Interview Question Validation Tests ====================

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `interview question request with valid data should pass validation`() {
        val request = InterviewQuestionRequest(
            category = "Kotlin",
            questionText = "Что такое корутины в Kotlin?",
            difficulty = "middle",
            tags = listOf("kotlin", "coroutines", "async")
        )

        val result = Validators.interviewQuestionValidator.validate(request)
        assertTrue(result is Valid, "Valid interview question should pass validation")
    }

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `interview question with invalid difficulty should fail validation`() {
        val request = InterviewQuestionRequest(
            category = "Kotlin",
            questionText = "Что такое корутины в Kotlin?",
            difficulty = "expert",
            tags = listOf("kotlin", "coroutines")
        )

        val result = Validators.interviewQuestionValidator.validate(request)
        assertTrue(result is Invalid, "Invalid difficulty should fail validation")
    }

    @Test
    @Tag("extension")
    @Tag("chapter2")
    fun `interview question with short question text should fail validation`() {
        val request = InterviewQuestionRequest(
            category = "Kotlin",
            questionText = "Что?",
            difficulty = "junior",
            tags = listOf("kotlin")
        )

        val result = Validators.interviewQuestionValidator.validate(request)
        assertTrue(result is Invalid, "Short question text should fail validation")
    }
}


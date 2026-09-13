package com.learning.services

import com.learning.domain.models.InterviewQuestion
import com.learning.models.InterviewAnswerRequest
import com.learning.models.InterviewQuestionRequest
import com.learning.repositories.InterviewRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Unit тесты для InterviewService.
 *
 * До 13.09.2026 тесты проверяли заглушку. Переписаны на мок репозитория;
 * имена и утверждения сохранены, кроме одного теста, противоречащего работе
 * с базой, — журнал docs/modernization/test-changes.md.
 */
@Tag("extension")
@Tag("chapter4")
class InterviewServiceTest {

    private lateinit var interviewRepository: InterviewRepository
    private lateinit var interviewService: InterviewService

    @BeforeEach
    fun setUp() {
        interviewRepository = mockk(relaxed = true)
        interviewService = InterviewService(interviewRepository)
        every { interviewRepository.findAll() } returns listOf(question(), question(), question())
    }

    // ==================== Get Interview Questions Tests ====================

    @Test
    fun `getInterviewQuestions should return questions with pagination`() {
        val result = interviewService.getInterviewQuestions(page = 1, limit = 10)

        assertNotNull(result)
        assertEquals(3, result.questions.size)
        assertEquals(3, result.pagination.total)
    }

    @Test
    fun `getInterviewQuestions should respect page parameter`() {
        val result = interviewService.getInterviewQuestions(page = 2, limit = 10)

        assertEquals(2, result.pagination.page)
    }

    @Test
    fun `getInterviewQuestions should respect limit parameter`() {
        val result = interviewService.getInterviewQuestions(page = 1, limit = 20)

        assertEquals(20, result.pagination.limit)
    }

    @Test
    fun `getInterviewQuestions should return valid question structure`() {
        val result = interviewService.getInterviewQuestions()
        val question = result.questions.first()

        assertNotNull(question.id)
        assertNotNull(question.questionText)
        assertNotNull(question.category)
        assertNotNull(question.difficulty)
        assertNotNull(question.status)
    }

    @Test
    fun `getInterviewQuestions should return questions with tags`() {
        val result = interviewService.getInterviewQuestions()
        val question = result.questions.first()

        assertNotNull(question.tags)
        assertTrue(question.tags.isNotEmpty())
    }

    // ==================== Get Interview Question By Id Tests ====================

    @Test
    fun `getInterviewQuestionById should return question when found`() {
        val stored = question()
        every { interviewRepository.findById(stored.id) } returns stored

        val result = interviewService.getInterviewQuestionById(stored.id.toString())

        assertNotNull(result)
        assertEquals(stored.id.toString(), result?.id)
    }

    @Test
    fun `getInterviewQuestionById should return question with valid fields`() {
        val stored = question()
        every { interviewRepository.findById(stored.id) } returns stored

        val result = interviewService.getInterviewQuestionById(stored.id.toString())

        assertNotNull(result?.questionText)
        assertNotNull(result?.category)
        assertNotNull(result?.difficulty)
        assertNotNull(result?.tags)
    }

    // ==================== Create Interview Question Tests ====================

    @Test
    fun `createInterviewQuestion should create question with provided data`() {
        val request = InterviewQuestionRequest(
            questionText = "What is Kotlin?",
            category = "Kotlin",
            difficulty = "junior",
            tags = listOf("kotlin", "basics")
        )
        stubCreateFromArguments()

        val result = interviewService.createInterviewQuestion(request)

        assertEquals(request.questionText, result.questionText)
        assertEquals(request.category, result.category)
        assertEquals(request.difficulty, result.difficulty)
        assertEquals(request.tags, result.tags)
    }

    @Test
    fun `createInterviewQuestion should generate unique id`() {
        val request = InterviewQuestionRequest(
            questionText = "Test question",
            category = "Test",
            difficulty = "middle",
            tags = listOf("test")
        )
        stubCreateFromArguments()

        val result = interviewService.createInterviewQuestion(request)

        assertNotNull(result.id)
        assertTrue(result.id.isNotBlank())
    }

    @Test
    fun `createInterviewQuestion should set status to ACTIVE`() {
        val request = InterviewQuestionRequest(
            questionText = "Another question",
            category = "Java",
            difficulty = "senior",
            tags = listOf("java")
        )
        stubCreateFromArguments()

        val result = interviewService.createInterviewQuestion(request)

        assertEquals("ACTIVE", result.status)
    }

    @Test
    fun `createInterviewQuestion should have null answer initially`() {
        val request = InterviewQuestionRequest(
            questionText = "Question without answer",
            category = "General",
            difficulty = "junior",
            tags = emptyList()
        )
        stubCreateFromArguments()

        val result = interviewService.createInterviewQuestion(request)

        assertNull(result.answer)
        assertNull(result.answeredAt)
    }

    // ==================== Update Interview Question Tests ====================

    @Test
    fun `updateInterviewQuestion should update question with provided data`() {
        val questionId = UUID.randomUUID()
        val request = InterviewQuestionRequest(
            questionText = "Updated question text",
            category = "Updated Category",
            difficulty = "senior",
            tags = listOf("updated", "tags")
        )
        every { interviewRepository.findById(questionId) } returns question(
            id = questionId, text = request.questionText, category = request.category,
            difficulty = request.difficulty, tags = request.tags
        )

        val result = interviewService.updateInterviewQuestion(questionId.toString(), request)

        assertEquals(questionId.toString(), result.id)
        assertEquals(request.questionText, result.questionText)
        assertEquals(request.category, result.category)
        assertEquals(request.difficulty, result.difficulty)
        assertEquals(request.tags, result.tags)
        verify { interviewRepository.updateQuestion(questionId, request.questionText, null, request.category, request.difficulty, request.tags, null) }
    }

    @Test
    fun `updateInterviewQuestion should preserve id`() {
        val questionId = UUID.randomUUID()
        val request = InterviewQuestionRequest(
            questionText = "New text",
            category = "New category",
            difficulty = "middle",
            tags = listOf("new")
        )
        every { interviewRepository.findById(questionId) } returns question(id = questionId)

        val result = interviewService.updateInterviewQuestion(questionId.toString(), request)

        assertEquals(questionId.toString(), result.id)
    }

    // ==================== Delete Interview Question Tests ====================

    @Test
    fun `deleteInterviewQuestion should return true for existing question`() {
        val questionId = UUID.randomUUID()
        every { interviewRepository.deleteQuestion(questionId) } returns true

        val result = interviewService.deleteInterviewQuestion(questionId.toString())

        assertTrue(result)
    }

    @Test
    fun `deleteInterviewQuestion should return false for unknown or invalid id`() {
        every { interviewRepository.deleteQuestion(any()) } returns false

        val unknown = interviewService.deleteInterviewQuestion(UUID.randomUUID().toString())
        val invalid = interviewService.deleteInterviewQuestion("id-2")

        assertFalse(unknown)
        assertFalse(invalid)
    }

    // ==================== Submit Interview Answer Tests ====================

    @Test
    fun `submitInterviewAnswer should return answer response`() {
        val request = InterviewAnswerRequest(
            questionId = UUID.randomUUID().toString(),
            answer = "This is my answer"
        )

        val result = interviewService.submitInterviewAnswer(request)

        assertNotNull(result)
        assertEquals(request.questionId, result.questionId)
        assertEquals(request.answer, result.answer)
    }

    @Test
    fun `submitInterviewAnswer should set status to SUBMITTED`() {
        val request = InterviewAnswerRequest(
            questionId = UUID.randomUUID().toString(),
            answer = "Answer text"
        )

        val result = interviewService.submitInterviewAnswer(request)

        assertEquals("SUBMITTED", result.status)
    }

    @Test
    fun `submitInterviewAnswer should generate unique id`() {
        val request = InterviewAnswerRequest(
            questionId = UUID.randomUUID().toString(),
            answer = "Test answer"
        )

        val result = interviewService.submitInterviewAnswer(request)

        assertNotNull(result.id)
        assertTrue(result.id.isNotBlank())
    }

    @Test
    fun `submitInterviewAnswer should set submittedAt timestamp`() {
        val request = InterviewAnswerRequest(
            questionId = UUID.randomUUID().toString(),
            answer = "Answer"
        )

        val result = interviewService.submitInterviewAnswer(request)

        assertTrue(result.submittedAt > 0)
    }

    private fun stubCreateFromArguments() {
        every { interviewRepository.createQuestion(any(), any(), any(), any(), any(), any()) } answers {
            question(text = arg(0), answer = arg(1), category = arg(2), difficulty = arg(3), tags = arg(4), published = arg(5))
        }
    }

    private fun question(
        id: UUID = UUID.randomUUID(),
        text: String = "Расскажите о своем опыте работы с Kotlin",
        answer: String = "",
        category: String = "PROGRAMMING",
        difficulty: String = "INTERMEDIATE",
        tags: List<String> = listOf("kotlin", "programming"),
        published: Boolean = true,
    ) = InterviewQuestion(id, text, answer, category, difficulty, tags, published)
}

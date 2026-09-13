package com.learning.services

import org.junit.jupiter.api.Tag
import com.learning.domain.models.Lesson
import com.learning.models.LessonRequest
import com.learning.repositories.LessonRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*

/**
 * Unit тесты для LessonService
 */
@Tag("core")
@Tag("chapter4")
class LessonServiceTest {

    private lateinit var lessonService: LessonService
    private lateinit var lessonRepository: LessonRepository

    @BeforeEach
    fun setUp() {
        lessonRepository = mockk(relaxed = true)
        lessonService = LessonService()
        
        // Inject mock repository using reflection
        val field = LessonService::class.java.getDeclaredField("lessonRepository")
        field.isAccessible = true
        field.set(lessonService, lessonRepository)
    }

    // ==================== Get Lesson By Id Tests ====================

    @Test
    fun `getLessonById should return null when lesson not found`() {
        val lessonId = UUID.randomUUID()
        every { lessonRepository.findById(lessonId) } returns null

        val result = lessonService.getLessonById(lessonId.toString())

        assertNull(result)
    }

    @Test
    fun `getLessonById should return lesson response when found`() {
        val lessonId = UUID.randomUUID()
        val courseId = UUID.randomUUID()
        val lesson = createTestLesson(lessonId, courseId, "Test Lesson", 1)
        every { lessonRepository.findById(lessonId) } returns lesson

        val result = lessonService.getLessonById(lessonId.toString())

        assertNotNull(result)
        assertEquals(lessonId.toString(), result?.id)
        assertEquals("Test Lesson", result?.title)
    }

    @Test
    fun `getLessonById should throw exception for invalid UUID`() {
        assertThrows<IllegalArgumentException> {
            lessonService.getLessonById("invalid-uuid")
        }
    }

    // ==================== Get Course Lessons Tests ====================

    @Test
    fun `getCourseLessons should return empty list when no lessons exist`() {
        val courseId = UUID.randomUUID()
        every { lessonRepository.findByCourseId(courseId) } returns emptyList()

        val result = lessonService.getCourseLessons(courseId.toString())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCourseLessons should return list of lesson responses`() {
        val courseId = UUID.randomUUID()
        val lessons = listOf(
            createTestLesson(UUID.randomUUID(), courseId, "Lesson 1", 1),
            createTestLesson(UUID.randomUUID(), courseId, "Lesson 2", 2)
        )
        every { lessonRepository.findByCourseId(courseId) } returns lessons

        val result = lessonService.getCourseLessons(courseId.toString())

        assertEquals(2, result.size)
        assertEquals("Lesson 1", result[0].title)
        assertEquals("Lesson 2", result[1].title)
    }

    @Test
    fun `getCourseLessons should preserve lesson order`() {
        val courseId = UUID.randomUUID()
        val lessons = listOf(
            createTestLesson(UUID.randomUUID(), courseId, "First", 1),
            createTestLesson(UUID.randomUUID(), courseId, "Second", 2),
            createTestLesson(UUID.randomUUID(), courseId, "Third", 3)
        )
        every { lessonRepository.findByCourseId(courseId) } returns lessons

        val result = lessonService.getCourseLessons(courseId.toString())

        assertEquals(1, result[0].orderIndex)
        assertEquals(2, result[1].orderIndex)
        assertEquals(3, result[2].orderIndex)
    }

    // ==================== Create Lesson Tests ====================

    @Test
    fun `createLesson should create and return new lesson`() {
        val courseId = UUID.randomUUID()
        val lessonId = UUID.randomUUID()
        val request = LessonRequest(
            courseId = courseId.toString(),
            title = "New Lesson",
            description = "Lesson description",
            orderIndex = 1,
            duration = 30,
            videoUrl = "https://example.com/video.mp4"
        )

        val createdLesson = createTestLesson(lessonId, courseId, request.title, request.orderIndex)
        every {
            lessonRepository.createLesson(any(), any(), any(), any(), any(), any(), any(), any())
        } returns createdLesson

        val result = lessonService.createLesson(request)

        assertEquals(request.title, result.title)
        assertEquals(request.orderIndex, result.orderIndex)
    }

    @Test
    fun `createLesson should throw exception when repository returns null`() {
        val request = LessonRequest(
            courseId = UUID.randomUUID().toString(),
            title = "New Lesson",
            description = "Description",
            orderIndex = 1,
            duration = 15
        )

        every {
            lessonRepository.createLesson(any(), any(), any(), any(), any(), any(), any(), any())
        } returns null

        assertThrows<Exception> {
            lessonService.createLesson(request)
        }
    }

    // ==================== Update Lesson Tests ====================

    @Test
    fun `updateLesson should update and return updated lesson`() {
        val lessonId = UUID.randomUUID()
        val courseId = UUID.randomUUID()
        val request = LessonRequest(
            courseId = courseId.toString(),
            title = "Updated Lesson",
            description = "Updated description",
            orderIndex = 2,
            duration = 45,
            videoUrl = "https://example.com/new-video.mp4"
        )

        val updatedLesson = createTestLesson(lessonId, courseId, request.title, request.orderIndex)
        every { lessonRepository.updateLesson(any(), any(), any(), any(), any(), any(), any(), any()) } returns true
        every { lessonRepository.findById(lessonId) } returns updatedLesson

        val result = lessonService.updateLesson(lessonId.toString(), request)

        assertEquals(request.title, result.title)
        assertEquals(request.orderIndex, result.orderIndex)
    }

    @Test
    fun `updateLesson should throw exception when update fails`() {
        val lessonId = UUID.randomUUID()
        val request = LessonRequest(
            courseId = UUID.randomUUID().toString(),
            title = "Updated Lesson",
            description = "Description",
            orderIndex = 1,
            duration = 30
        )

        every { lessonRepository.updateLesson(any(), any(), any(), any(), any(), any(), any(), any()) } returns false

        assertThrows<Exception> {
            lessonService.updateLesson(lessonId.toString(), request)
        }
    }

    // ==================== Delete Lesson Tests ====================

    @Test
    fun `deleteLesson should return true when lesson deleted successfully`() {
        val lessonId = UUID.randomUUID()
        every { lessonRepository.deleteLesson(lessonId) } returns true

        val result = lessonService.deleteLesson(lessonId.toString())

        assertTrue(result)
        verify { lessonRepository.deleteLesson(lessonId) }
    }

    @Test
    fun `deleteLesson should return false when lesson not found`() {
        val lessonId = UUID.randomUUID()
        every { lessonRepository.deleteLesson(lessonId) } returns false

        val result = lessonService.deleteLesson(lessonId.toString())

        assertFalse(result)
    }

    // ==================== Get All Lessons Tests ====================

    @Test
    fun `getAllLessons should return empty list`() {
        every { lessonRepository.findAll() } returns emptyList()

        val result = lessonService.getAllLessons()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllLessons should return lessons from repository`() {
        val courseId = UUID.randomUUID()
        every { lessonRepository.findAll() } returns listOf(
            createTestLesson(UUID.randomUUID(), courseId, "Lesson 1", 1),
            createTestLesson(UUID.randomUUID(), courseId, "Lesson 2", 2)
        )

        val result = lessonService.getAllLessons()

        assertEquals(listOf("Lesson 1", "Lesson 2"), result.map { it.title })
    }

    // ==================== Get Lesson Content Tests ====================

    @Test
    fun `getLessonContent should return empty list`() {
        val lessonId = UUID.randomUUID()
        every { lessonRepository.findById(lessonId) } returns null

        val result = lessonService.getLessonContent(lessonId.toString())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getLessonContent should return text block when lesson has content`() {
        val lessonId = UUID.randomUUID()
        every { lessonRepository.findById(lessonId) } returns createTestLesson(lessonId, UUID.randomUUID(), "Lesson", 1)

        val result = lessonService.getLessonContent(lessonId.toString())

        assertEquals(1, result.size)
        assertEquals("TEXT", result.first().contentType)
        assertEquals("Test content", result.first().content)
    }

    // ==================== Helper Functions ====================

    private fun createTestLesson(
        id: UUID,
        courseId: UUID,
        title: String,
        order: Int
    ): Lesson {
        return Lesson(
            id = id,
            courseId = courseId,
            title = title,
            description = "Test Lesson Description",
            order = order,
            duration = 30,
            videoUrl = null,
            content = "Test content",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

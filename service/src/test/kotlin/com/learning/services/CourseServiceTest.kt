package com.learning.services

import org.junit.jupiter.api.Tag
import com.learning.domain.models.Course
import com.learning.domain.models.Lesson
import com.learning.models.CourseRequest
import com.learning.repositories.CourseRepository
import com.learning.repositories.LessonRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Unit тесты для CourseService
 */
@Tag("core")
@Tag("chapter4")
class CourseServiceTest {

    private lateinit var courseService: CourseService
    private lateinit var courseRepository: CourseRepository
    private lateinit var lessonRepository: LessonRepository

    @BeforeEach
    fun setUp() {
        courseRepository = mockk(relaxed = true)
        lessonRepository = mockk(relaxed = true)
        courseService = CourseService(courseRepository, lessonRepository)
    }

    // ==================== Get All Courses Tests ====================

    @Test
    fun `getAllCourses should return empty list when no courses exist`() {
        every { courseRepository.findPublished() } returns emptyList()

        val result = courseService.getAllCourses()

        assertTrue(result.isEmpty())
        verify { courseRepository.findPublished() }
    }

    @Test
    fun `getAllCourses should return list of course responses`() {
        val courses = listOf(
            createTestCourse(UUID.randomUUID(), "Kotlin Basics"),
            createTestCourse(UUID.randomUUID(), "Advanced Kotlin")
        )
        every { courseRepository.findPublished() } returns courses

        val result = courseService.getAllCourses()

        assertEquals(2, result.size)
        assertEquals("Kotlin Basics", result[0].title)
        assertEquals("Advanced Kotlin", result[1].title)
    }

    @Test
    fun `getAllCourses should map course fields correctly`() {
        val courseId = UUID.randomUUID()
        val course = createTestCourse(
            id = courseId,
            title = "Test Course",
            description = "Test Description",
            price = BigDecimal("99.99"),
            duration = 120,
            difficulty = "beginner"
        )
        every { courseRepository.findPublished() } returns listOf(course)

        val result = courseService.getAllCourses()

        assertEquals(1, result.size)
        val response = result[0]
        assertEquals(courseId.toString(), response.id)
        assertEquals("Test Course", response.title)
        assertEquals("Test Description", response.description)
        assertEquals(99.99, response.price)
        assertEquals(120, response.duration)
        assertEquals("BEGINNER", response.difficulty)
    }

    // ==================== Get Course By Id Tests ====================

    @Test
    fun `getCourseById should return null when course not found`() {
        val courseId = UUID.randomUUID()
        every { courseRepository.findById(courseId) } returns null

        val result = courseService.getCourseById(courseId.toString())

        assertNull(result)
    }

    @Test
    fun `getCourseById should return course response when found`() {
        val courseId = UUID.randomUUID()
        val course = createTestCourse(courseId, "Test Course")
        every { courseRepository.findById(courseId) } returns course

        val result = courseService.getCourseById(courseId.toString())

        assertNotNull(result)
        assertEquals(courseId.toString(), result?.id)
        assertEquals("Test Course", result?.title)
    }

    @Test
    fun `getCourseById should return null for draft course`() {
        val courseId = UUID.randomUUID()
        val draft = createTestCourse(courseId, "Черновик", isPublished = false)
        every { courseRepository.findById(courseId) } returns draft

        val result = courseService.getCourseById(courseId.toString())

        assertNull(result, "Черновик без пользователя не отдаётся — маршрут ответит 404")
    }

    @Test
    fun `getCourseById should throw exception for invalid UUID`() {
        assertThrows<IllegalArgumentException> {
            courseService.getCourseById("not-a-uuid")
        }
    }

    // ==================== Get Course Lessons Tests ====================

    @Test
    fun `getCourseLessons should return empty list when no lessons exist`() {
        val courseId = UUID.randomUUID()
        every { lessonRepository.findByCourseId(courseId) } returns emptyList()

        val result = courseService.getCourseLessons(courseId.toString())

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

        val result = courseService.getCourseLessons(courseId.toString())

        assertEquals(2, result.size)
        assertEquals("Lesson 1", result[0].title)
        assertEquals("Lesson 2", result[1].title)
    }

    // ==================== Create Course Tests ====================

    @Test
    fun `createCourse should create and return new course`() {
        val request = CourseRequest(
            title = "New Course",
            description = "Course Description",
            category = "Programming",
            difficulty = "beginner",
            duration = 60,
            price = 49.99
        )

        val createdCourse = createTestCourse(
            UUID.randomUUID(),
            request.title,
            request.description,
            BigDecimal.valueOf(request.price),
            request.duration,
            request.difficulty
        )

        every {
            courseRepository.createCourse(
                title = request.title,
                description = request.description,
                shortDescription = null,
                thumbnailUrl = null,
                price = BigDecimal.valueOf(request.price),
                originalPrice = null,
                duration = request.duration,
                difficulty = request.difficulty,
                isPublished = false
            )
        } returns createdCourse

        val result = courseService.createCourse(request)

        assertEquals(request.title, result.title)
        assertEquals(request.description, result.description)
    }

    @Test
    fun `createCourse should throw exception when repository returns null`() {
        val request = CourseRequest(
            title = "New Course",
            description = "Course Description",
            category = "Programming",
            difficulty = "beginner",
            duration = 60,
            price = 49.99
        )

        every {
            courseRepository.createCourse(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns null

        assertThrows<Exception> {
            courseService.createCourse(request)
        }
    }

    // ==================== Update Course Tests ====================

    @Test
    fun `updateCourse should update and return updated course`() {
        val courseId = UUID.randomUUID()
        val request = CourseRequest(
            title = "Updated Course",
            description = "Updated Description",
            category = "Programming",
            difficulty = "intermediate",
            duration = 90,
            price = 79.99
        )

        val updatedCourse = createTestCourse(
            courseId,
            request.title,
            request.description,
            BigDecimal.valueOf(request.price),
            request.duration,
            request.difficulty
        )

        every {
            courseRepository.updateCourse(
                courseId = courseId,
                title = request.title,
                description = request.description,
                price = BigDecimal.valueOf(request.price),
                duration = request.duration,
                difficulty = request.difficulty
            )
        } returns true
        every { courseRepository.findById(courseId) } returns updatedCourse

        val result = courseService.updateCourse(courseId.toString(), request)

        assertEquals(request.title, result.title)
        assertEquals(request.description, result.description)
    }

    @Test
    fun `updateCourse should throw exception when update fails`() {
        val courseId = UUID.randomUUID()
        val request = CourseRequest(
            title = "Updated Course",
            description = "Updated Description",
            category = "Programming",
            difficulty = "intermediate",
            duration = 90,
            price = 79.99
        )

        every {
            courseRepository.updateCourse(any(), any(), any(), any(), any(), any())
        } returns false

        assertThrows<Exception> {
            courseService.updateCourse(courseId.toString(), request)
        }
    }

    // ==================== Delete Course Tests ====================

    @Test
    fun `deleteCourse should return true when course deleted successfully`() {
        val courseId = UUID.randomUUID()
        every { courseRepository.deleteCourse(courseId) } returns true

        val result = courseService.deleteCourse(courseId.toString())

        assertTrue(result)
        verify { courseRepository.deleteCourse(courseId) }
    }

    @Test
    fun `deleteCourse should return false when course not found`() {
        val courseId = UUID.randomUUID()
        every { courseRepository.deleteCourse(courseId) } returns false

        val result = courseService.deleteCourse(courseId.toString())

        assertFalse(result)
    }

    // ==================== Helper Functions ====================

    private fun createTestCourse(
        id: UUID,
        title: String,
        description: String = "Test Description",
        price: BigDecimal = BigDecimal("99.99"),
        duration: Int = 60,
        difficulty: String = "beginner",
        isPublished: Boolean = true
    ): Course {
        return Course(
            id = id,
            title = title,
            description = description,
            shortDescription = null,
            thumbnailUrl = null,
            price = price,
            originalPrice = null,
            duration = duration,
            difficulty = difficulty,
            isPublished = isPublished,
            lessonsCount = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

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

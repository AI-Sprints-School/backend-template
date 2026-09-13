package com.learning.integration

import org.junit.jupiter.api.Tag
import com.learning.repositories.CourseRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.util.*

/**
 * Интеграционные тесты для CourseRepository с реальной PostgreSQL
 */
@Tag("core")
@Tag("chapter3")
class CourseRepositoryIntegrationTest : DatabaseTestBase() {

    private val courseRepository = CourseRepository()

    // ==================== Create Course Tests ====================

    @Test
    fun `createCourse should create course and return it`() {
        val course = courseRepository.createCourse(
            title = "Kotlin Basics",
            description = "Learn Kotlin from scratch",
            shortDescription = "Quick intro to Kotlin",
            thumbnailUrl = "https://example.com/kotlin.jpg",
            price = BigDecimal("99.99"),
            originalPrice = BigDecimal("149.99"),
            duration = 120,
            difficulty = "beginner",
            isPublished = true
        )

        assertNotNull(course)
        assertEquals("Kotlin Basics", course?.title)
        assertEquals("Learn Kotlin from scratch", course?.description)
        assertEquals("beginner", course?.difficulty)
        assertEquals(120, course?.duration)
        assertTrue(course!!.isPublished)
    }

    @Test
    fun `createCourse should set default values`() {
        val course = courseRepository.createCourse(
            title = "Test Course",
            description = "Test Description",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("50.00"),
            originalPrice = null,
            duration = 60,
            difficulty = "intermediate",
            isPublished = false
        )

        assertNotNull(course)
        assertEquals(0, course?.lessonsCount)
        assertFalse(course!!.isPublished)
        assertNull(course.thumbnailUrl)
        assertNull(course.originalPrice)
    }

    // ==================== Find By Id Tests ====================

    @Test
    fun `findById should return course when exists`() {
        val created = courseRepository.createCourse(
            title = "Find Me",
            description = "Description",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("25.00"),
            originalPrice = null,
            duration = 30,
            difficulty = "beginner"
        )
        assertNotNull(created)

        val found = courseRepository.findById(created!!.id)

        assertNotNull(found)
        assertEquals(created.id, found?.id)
        assertEquals("Find Me", found?.title)
    }

    @Test
    fun `findById should return null when not exists`() {
        val found = courseRepository.findById(UUID.randomUUID())

        assertNull(found)
    }

    // ==================== Find All Tests ====================

    @Test
    fun `findAll should return all courses`() {
        courseRepository.createCourse("Course 1", "Desc 1", null, null, BigDecimal("10"), null, 10, "beginner")
        courseRepository.createCourse("Course 2", "Desc 2", null, null, BigDecimal("20"), null, 20, "intermediate")
        courseRepository.createCourse("Course 3", "Desc 3", null, null, BigDecimal("30"), null, 30, "advanced")

        val all = courseRepository.findAll()

        assertEquals(3, all.size)
    }

    @Test
    fun `findAll should return empty list when no courses`() {
        val all = courseRepository.findAll()

        assertTrue(all.isEmpty())
    }

    // ==================== Find Published Tests ====================

    @Test
    fun `findPublished should return only published courses`() {
        courseRepository.createCourse("Published", "Desc", null, null, BigDecimal("10"), null, 10, "beginner", true)
        courseRepository.createCourse("Draft", "Desc", null, null, BigDecimal("20"), null, 20, "beginner", false)
        courseRepository.createCourse("Another Published", "Desc", null, null, BigDecimal("30"), null, 30, "intermediate", true)

        val published = courseRepository.findPublished()

        assertEquals(2, published.size)
        assertTrue(published.all { it.isPublished })
    }

    @Test
    fun `findPublished should return empty when no published courses`() {
        courseRepository.createCourse("Draft 1", "Desc", null, null, BigDecimal("10"), null, 10, "beginner", false)
        courseRepository.createCourse("Draft 2", "Desc", null, null, BigDecimal("20"), null, 20, "intermediate", false)

        val published = courseRepository.findPublished()

        assertTrue(published.isEmpty())
    }

    // ==================== Find By Difficulty Tests ====================

    @Test
    fun `findByDifficulty should return courses with matching difficulty`() {
        courseRepository.createCourse("Beginner 1", "Desc", null, null, BigDecimal("10"), null, 10, "beginner")
        courseRepository.createCourse("Intermediate 1", "Desc", null, null, BigDecimal("20"), null, 20, "intermediate")
        courseRepository.createCourse("Beginner 2", "Desc", null, null, BigDecimal("30"), null, 30, "beginner")

        val beginnerCourses = courseRepository.findByDifficulty("beginner")

        assertEquals(2, beginnerCourses.size)
        assertTrue(beginnerCourses.all { it.difficulty == "beginner" })
    }

    // ==================== Update Course Tests ====================

    @Test
    fun `updateCourse should update specified fields`() {
        val course = courseRepository.createCourse(
            title = "Original Title",
            description = "Original Desc",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("50.00"),
            originalPrice = null,
            duration = 60,
            difficulty = "beginner"
        )
        assertNotNull(course)

        val success = courseRepository.updateCourse(
            courseId = course!!.id,
            title = "Updated Title",
            description = "Updated Desc",
            price = BigDecimal("75.00")
        )

        assertTrue(success)
        val updated = courseRepository.findById(course.id)
        assertEquals("Updated Title", updated?.title)
        assertEquals("Updated Desc", updated?.description)
        assertEquals(BigDecimal("75.00").setScale(2), updated?.price?.setScale(2))
    }

    @Test
    fun `updateCourse should return false when course not exists`() {
        val success = courseRepository.updateCourse(
            courseId = UUID.randomUUID(),
            title = "New Title"
        )

        assertFalse(success)
    }

    @Test
    fun `updateCourse should only update provided fields`() {
        val course = courseRepository.createCourse(
            title = "Original",
            description = "Original Desc",
            shortDescription = "Short",
            thumbnailUrl = "https://example.com/thumb.jpg",
            price = BigDecimal("100.00"),
            originalPrice = null,
            duration = 60,
            difficulty = "beginner"
        )
        assertNotNull(course)

        courseRepository.updateCourse(
            courseId = course!!.id,
            title = "New Title"
        )

        val updated = courseRepository.findById(course.id)
        assertEquals("New Title", updated?.title)
        assertEquals("Original Desc", updated?.description) // Unchanged
        assertEquals("Short", updated?.shortDescription) // Unchanged
    }

    // ==================== Delete Course Tests ====================

    @Test
    fun `deleteCourse should delete course and return true`() {
        val course = courseRepository.createCourse(
            title = "To Delete",
            description = "Desc",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("10.00"),
            originalPrice = null,
            duration = 10,
            difficulty = "beginner"
        )
        assertNotNull(course)

        val success = courseRepository.deleteCourse(course!!.id)

        assertTrue(success)
        val deleted = courseRepository.findById(course.id)
        assertNull(deleted)
    }

    @Test
    fun `deleteCourse should return false when course not exists`() {
        val success = courseRepository.deleteCourse(UUID.randomUUID())

        assertFalse(success)
    }

    // ==================== Update Lessons Count Tests ====================

    @Test
    fun `updateLessonsCount should update count`() {
        val course = courseRepository.createCourse(
            title = "With Lessons",
            description = "Desc",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("50.00"),
            originalPrice = null,
            duration = 60,
            difficulty = "intermediate"
        )
        assertNotNull(course)
        assertEquals(0, course?.lessonsCount)

        val success = courseRepository.updateLessonsCount(course!!.id, 10)

        assertTrue(success)
        val updated = courseRepository.findById(course.id)
        assertEquals(10, updated?.lessonsCount)
    }

    // ==================== Search Courses Tests ====================

    @Test
    fun `searchCourses should find by title`() {
        courseRepository.createCourse("Kotlin for Android", "Desc", null, null, BigDecimal("10"), null, 10, "beginner")
        courseRepository.createCourse("Java Basics", "Desc", null, null, BigDecimal("20"), null, 20, "beginner")
        courseRepository.createCourse("Advanced Kotlin", "Desc", null, null, BigDecimal("30"), null, 30, "advanced")

        val results = courseRepository.searchCourses("Kotlin")

        assertEquals(2, results.size)
    }

    @Test
    fun `searchCourses should find by description`() {
        courseRepository.createCourse("Course 1", "Learn Kotlin programming", null, null, BigDecimal("10"), null, 10, "beginner")
        courseRepository.createCourse("Course 2", "Learn Java programming", null, null, BigDecimal("20"), null, 20, "beginner")

        val results = courseRepository.searchCourses("Kotlin")

        assertEquals(1, results.size)
        assertEquals("Course 1", results[0].title)
    }

    // ==================== Discount Tests ====================

    @Test
    fun `getCoursesWithDiscount should return courses with original price`() {
        courseRepository.createCourse("Discounted", "Desc", null, null, BigDecimal("50"), BigDecimal("100"), 30, "beginner")
        courseRepository.createCourse("Regular", "Desc", null, null, BigDecimal("50"), null, 30, "beginner")

        val discounted = courseRepository.getCoursesWithDiscount()

        assertEquals(1, discounted.size)
        assertEquals("Discounted", discounted[0].title)
    }

    @Test
    fun `course hasDiscount should be true when originalPrice is set`() {
        val course = courseRepository.createCourse(
            title = "On Sale",
            description = "Desc",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("49.99"),
            originalPrice = BigDecimal("99.99"),
            duration = 60,
            difficulty = "beginner"
        )

        assertTrue(course!!.hasDiscount)
        assertEquals(50, course.discountPercentage)
    }
}

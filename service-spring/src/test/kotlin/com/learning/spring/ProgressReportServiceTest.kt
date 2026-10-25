package com.learning.spring

import com.learning.spring.legacy.CourseProgress
import com.learning.spring.legacy.ProgressEntry
import com.learning.spring.legacy.ProgressReportService
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Урок 20: отчёт прогресса. Тест работает и с Java-сервисом на Stream API, и с Kotlin-версией
 * в том же пакете `legacy` (записи `ProgressEntry`, `CourseProgress` остаются как есть).
 * `topCourse` вызывается через рефлексию: у Java он возвращает `Optional`, у Kotlin-версии — `CourseProgress?`.
 */
@Tag("core")
@Tag("chapter5")
class ProgressReportServiceTest {

    private val service = ProgressReportService()

    private val entries = listOf(
        ProgressEntry("u1", "c1", "Kotlin", true),
        ProgressEntry("u1", "c1", "Kotlin", false),
        ProgressEntry("u1", "c2", "Compose", true),
        ProgressEntry("u2", "c3", "Architecture", false),
    )

    private fun topCourse(list: List<ProgressEntry>): CourseProgress? {
        val result = ProgressReportService::class.java.getMethod("topCourse", List::class.java).invoke(service, list)
        return (if (result is Optional<*>) result.orElse(null) else result) as CourseProgress?
    }

    @Test
    fun `report counts completed share per course`() {
        val kotlin = service.report(entries).single { it.courseId == "c1" }
        assertEquals(2, kotlin.total)
        assertEquals(1, kotlin.completed)
        assertEquals(50, kotlin.percent)
    }

    @Test
    fun `report is sorted by percent descending then by title`() {
        assertEquals(listOf("Compose", "Kotlin", "Architecture"), service.report(entries).map { it.courseTitle })
    }

    @Test
    fun `top course is the one with the biggest share and absent on empty input`() {
        assertEquals("Compose", topCourse(entries)?.courseTitle)
        assertNull(topCourse(emptyList()))
    }

    @Test
    fun `completed course titles are only fully completed courses`() {
        assertEquals(listOf("Compose"), service.completedCourseTitles(entries))
    }
}

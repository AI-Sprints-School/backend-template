package com.learning.services

import com.learning.domain.models.Sprint
import com.learning.models.SprintRequest
import com.learning.repositories.SprintRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

/**
 * Unit тесты для SprintService.
 *
 * До 13.09.2026 тесты проверяли заглушку (сервис без базы возвращал константы).
 * Переписаны на мок репозитория, как CourseServiceTest; имена и утверждения
 * сохранены везде, где они не противоречат работе с базой — журнал
 * docs/modernization/test-changes.md, строки 13–23.
 */
@Tag("extension")
@Tag("chapter4")
class SprintServiceTest {

    private lateinit var sprintRepository: SprintRepository
    private lateinit var sprintService: SprintService

    @BeforeEach
    fun setUp() {
        sprintRepository = mockk(relaxed = true)
        sprintService = SprintService(sprintRepository)
    }

    // ==================== Get All Sprints Tests ====================

    @Test
    fun `getAllSprints should return list of sprints`() {
        every { sprintRepository.findAll() } returns listOf(sprint())

        val result = sprintService.getAllSprints()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getAllSprints should return sprints with valid fields`() {
        val stored = sprint(title = "Весенний спринт", description = "Скидки на курсы")
        every { sprintRepository.findAll() } returns listOf(stored)

        val result = sprintService.getAllSprints()
        val sprint = result.first()

        assertEquals(stored.id.toString(), sprint.id)
        assertEquals("Весенний спринт", sprint.title)
        assertEquals("Скидки на курсы", sprint.description)
        assertEquals(stored.startDate.toString(), sprint.startDate)
        assertEquals(stored.endDate.toString(), sprint.endDate)
    }

    // ==================== Get Sprint By Id Tests ====================

    @Test
    fun `getSprintById should return sprint when found`() {
        val stored = sprint()
        every { sprintRepository.findById(stored.id) } returns stored

        val result = sprintService.getSprintById(stored.id.toString())

        assertNotNull(result)
        assertEquals(stored.id.toString(), result?.id)
    }

    @Test
    fun `getSprintById should return sprint with correct structure`() {
        val stored = sprint(discount = 20)
        every { sprintRepository.findById(stored.id) } returns stored

        val result = sprintService.getSprintById(stored.id.toString())

        assertNotNull(result)
        assertEquals(stored.title, result?.title)
        assertEquals(stored.description, result?.description)
        assertEquals(20, result?.discount)
        assertNotNull(result?.startDate)
        assertNotNull(result?.endDate)
    }

    // ==================== Create Sprint Tests ====================

    @Test
    fun `createSprint should create sprint with provided data`() {
        val request = SprintRequest(
            title = "New Sprint",
            description = "Sprint description",
            discount = 25,
            startDate = "2024-04-01T00:00:00Z",
            endDate = "2024-04-30T23:59:59Z",
            isActive = true
        )
        every {
            sprintRepository.createSprint(
                title = "New Sprint",
                description = "Sprint description",
                discountPercentage = 25,
                startDate = Instant.parse(request.startDate),
                endDate = Instant.parse(request.endDate),
                isActive = true
            )
        } answers { sprint(title = arg(0), description = arg(1), discount = arg(2), start = arg(3), end = arg(4)) }

        val result = sprintService.createSprint(request)

        assertEquals(request.title, result.title)
        assertEquals(request.description, result.description)
        assertEquals(request.discount, result.discount)
        assertEquals(request.startDate, result.startDate)
        assertEquals(request.endDate, result.endDate)
    }

    @Test
    fun `createSprint should generate unique id`() {
        val request = SprintRequest(
            title = "Test Sprint",
            description = "Description",
            discount = 10,
            startDate = "2024-05-01T00:00:00Z",
            endDate = "2024-05-31T23:59:59Z"
        )
        every { sprintRepository.createSprint(any(), any(), any(), any(), any(), any()) } returns sprint()

        val result = sprintService.createSprint(request)

        assertNotNull(result.id)
        assertTrue(result.id.isNotBlank())
    }

    @Test
    fun `createSprint should handle null discount`() {
        val request = SprintRequest(
            title = "Test Sprint",
            description = "Description",
            discount = null,
            startDate = "2024-05-01T00:00:00Z",
            endDate = "2024-05-31T23:59:59Z"
        )
        every { sprintRepository.createSprint(any(), any(), 0, any(), any(), any()) } returns sprint(discount = 0)

        val result = sprintService.createSprint(request)

        assertEquals(0, result.discount)
        verify { sprintRepository.createSprint(any(), any(), 0, any(), any(), any()) }
    }

    // ==================== Update Sprint Tests ====================

    @Test
    fun `updateSprint should update sprint with provided data`() {
        val sprintId = UUID.randomUUID()
        val request = SprintRequest(
            title = "Updated Sprint",
            description = "Updated description",
            discount = 30,
            startDate = "2024-06-01T00:00:00Z",
            endDate = "2024-06-30T23:59:59Z",
            isActive = false
        )
        every { sprintRepository.updateSprint(sprintId, any(), any(), any(), any(), any(), any()) } returns true
        every { sprintRepository.findById(sprintId) } returns sprint(
            id = sprintId, title = "Updated Sprint", description = "Updated description", discount = 30
        )

        val result = sprintService.updateSprint(sprintId.toString(), request)

        assertEquals(sprintId.toString(), result.id)
        assertEquals(request.title, result.title)
        assertEquals(request.description, result.description)
        assertEquals(request.discount, result.discount)
    }

    @Test
    fun `updateSprint should preserve id`() {
        val sprintId = UUID.randomUUID()
        val request = SprintRequest(
            title = "New Title",
            description = "New Description",
            discount = 15,
            startDate = "2024-07-01T00:00:00Z",
            endDate = "2024-07-31T23:59:59Z"
        )
        every { sprintRepository.findById(sprintId) } returns sprint(id = sprintId)

        val result = sprintService.updateSprint(sprintId.toString(), request)

        assertEquals(sprintId.toString(), result.id)
    }

    // ==================== Delete Sprint Tests ====================

    @Test
    fun `deleteSprint should return true for existing sprint`() {
        val sprintId = UUID.randomUUID()
        every { sprintRepository.deleteSprint(sprintId) } returns true

        val result = sprintService.deleteSprint(sprintId.toString())

        assertTrue(result)
    }

    @Test
    fun `deleteSprint should return false for unknown or invalid id`() {
        every { sprintRepository.deleteSprint(any()) } returns false

        val unknown = sprintService.deleteSprint(UUID.randomUUID().toString())
        val invalid = sprintService.deleteSprint("id-2")

        assertFalse(unknown)
        assertFalse(invalid)
    }

    private fun sprint(
        id: UUID = UUID.randomUUID(),
        title: String = "Весенний спринт 2024",
        description: String = "Специальные предложения на весенние курсы",
        discount: Int = 20,
        start: Instant = Instant.parse("2024-03-01T00:00:00Z"),
        end: Instant = Instant.parse("2024-03-31T23:59:59Z"),
    ) = Sprint(id, title, description, discount, start, end)
}

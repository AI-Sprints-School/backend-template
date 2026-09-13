package com.learning.routes

import com.learning.*
import com.learning.integration.DatabaseTestBase
import com.learning.repositories.UserRepository
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Tag
import java.util.*
import kotlin.test.*

/**
 * HTTP-тесты прогресса и профиля: `POST /lessons/{id}/complete`,
 * `GET /user/progress`, `GET|PUT /user/profile`.
 */
@Tag("core")
@Tag("chapter5")
class ProgressRoutesTest : DatabaseTestBase() {

    @Test
    fun `GET progress without token should return 401`() = apiTest {
        val response = client.get("/api/v1/user/progress")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET progress for new user should return zeros`() = apiTest {
        val student = TestUsers.student()

        val response = client.get("/api/v1/user/progress") { header(HttpHeaders.Authorization, student.bearer) }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.json()
        assertEquals(student.id, body.str("userId"))
        assertEquals(0, body.int("totalCourses"))
        assertEquals(0, body.int("completedLessons"))
        assertEquals(0, body.arr("achievements").size)
    }

    @Test
    fun `POST complete without token should return 401`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1)

        val response = client.post("/api/v1/lessons/${lesson.id}/complete")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST complete for unknown lesson should return 404`() = apiTest {
        val student = TestUsers.student()

        val response = client.post("/api/v1/lessons/${UUID.randomUUID()}/complete") {
            header(HttpHeaders.Authorization, student.bearer)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST complete with invalid lesson id should return 400`() = apiTest {
        val student = TestUsers.student()

        val response = client.post("/api/v1/lessons/not-a-uuid/complete") {
            header(HttpHeaders.Authorization, student.bearer)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST complete should return course completion percentage`() = apiTest {
        val course = TestData.course()
        val first = TestData.lesson(course, 1)
        TestData.lesson(course, 2)
        TestData.lesson(course, 3)
        TestData.lesson(course, 4)
        val student = TestUsers.student()

        val response = client.post("/api/v1/lessons/${first.id}/complete") {
            header(HttpHeaders.Authorization, student.bearer)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val progress = response.json().obj("progress")
        assertEquals(course.id.toString(), progress.str("courseId"))
        assertEquals(1, progress.int("completedLessons"))
        assertEquals(4, progress.int("totalLessons"))
        assertEquals(25, progress.int("courseCompletionPercentage"))
        assertFalse(progress.bool("courseCompleted"))
    }

    @Test
    fun `completing all lessons should mark course completed in progress`() = apiTest {
        val course = TestData.course()
        val lessons = listOf(TestData.lesson(course, 1, duration = 10), TestData.lesson(course, 2, duration = 25))
        val student = TestUsers.student()

        lessons.forEach { lesson ->
            val response = client.post("/api/v1/lessons/${lesson.id}/complete") {
                header(HttpHeaders.Authorization, student.bearer)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
        val progress = client.get("/api/v1/user/progress") { header(HttpHeaders.Authorization, student.bearer) }.json()

        assertEquals(1, progress.int("totalCourses"))
        assertEquals(1, progress.int("completedCourses"))
        assertEquals(0, progress.int("inProgressCourses"))
        assertEquals(2, progress.int("totalLessons"))
        assertEquals(2, progress.int("completedLessons"))
        assertEquals(35, progress.int("totalStudyTime"))
        assertEquals(1, progress.int("currentStreak"))
    }

    @Test
    fun `progress of one user should not be visible to another`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1)
        val alice = TestUsers.student()
        val bob = TestUsers.student()

        client.post("/api/v1/lessons/${lesson.id}/complete") { header(HttpHeaders.Authorization, alice.bearer) }
        val bobProgress = client.get("/api/v1/user/progress") { header(HttpHeaders.Authorization, bob.bearer) }.json()

        assertEquals(bob.id, bobProgress.str("userId"))
        assertEquals(0, bobProgress.int("completedLessons"))
    }

    // ==================== Профиль ====================

    @Test
    fun `GET profile should return user from database`() = apiTest {
        val student = TestUsers.student()

        val response = client.get("/api/v1/user/profile") { header(HttpHeaders.Authorization, student.bearer) }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.json()
        assertEquals(student.id, body.str("id"))
        assertEquals(student.user.email, body.str("email"))
        assertEquals("student", body.str("role"))
        assertEquals("BEGINNER", body.str("level"))
    }

    @Test
    fun `PUT profile should update names in database`() = apiTest {
        val student = TestUsers.student()

        val response = client.put("/api/v1/user/profile") {
            header(HttpHeaders.Authorization, student.bearer)
            contentType(ContentType.Application.Json)
            setBody("""{"firstName": "Пётр", "lastName": "Сидоров", "email": null, "avatar": null}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Пётр", response.json().str("firstName"))
        val stored = UserRepository().findById(student.user.id)!!
        assertEquals("Пётр", stored.firstName)
        assertEquals("Сидоров", stored.lastName)
    }

    @Test
    fun `PUT profile with email of another user should return 409`() = apiTest {
        val alice = TestUsers.student()
        val bob = TestUsers.student()

        val response = client.put("/api/v1/user/profile") {
            header(HttpHeaders.Authorization, bob.bearer)
            contentType(ContentType.Application.Json)
            setBody("""{"firstName": null, "lastName": null, "email": "${alice.user.email}", "avatar": null}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(bob.user.email, UserRepository().findById(bob.user.id)?.email)
    }
}

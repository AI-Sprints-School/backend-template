package com.learning

import com.learning.domain.models.Course
import com.learning.domain.models.Lesson
import com.learning.repositories.CourseRepository
import com.learning.repositories.LessonRepository
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.util.UUID

/** Курсы и уроки для тестов — прямо в базе, минуя HTTP. */
object TestData {
    private val courses = CourseRepository()
    private val lessons = LessonRepository()

    fun course(title: String = "Kotlin Basics", published: Boolean = true, authorId: UUID? = null): Course =
        courses.createCourse(
            title = title,
            description = "Описание курса $title",
            shortDescription = null,
            thumbnailUrl = null,
            price = BigDecimal("990.00"),
            originalPrice = null,
            duration = 120,
            difficulty = "beginner",
            isPublished = published,
            authorId = authorId
        ) ?: error("Не удалось создать курс")

    fun lesson(course: Course, order: Int, title: String = "Урок $order", duration: Int = 15, content: String = "Текст урока $order"): Lesson =
        lessons.createLesson(
            courseId = course.id,
            title = title,
            description = "Описание урока $order",
            content = content,
            videoUrl = null,
            duration = duration,
            order = order,
            isPublished = true
        ) ?: error("Не удалось создать урок")
}

/** Тело ответа как JSON-объект. */
suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content
fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int
fun JsonObject.bool(key: String): Boolean = getValue(key).jsonPrimitive.boolean
fun JsonObject.arr(key: String): JsonArray = getValue(key).jsonArray
fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

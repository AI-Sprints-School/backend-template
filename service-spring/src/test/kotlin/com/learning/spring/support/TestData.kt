package com.learning.spring.support

import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/** Данные для тестов: пишутся прямо в базу через JDBC. Готово в шаблоне. */
class TestData(private val jdbc: JdbcTemplate) {

    data class Course(val id: UUID, val title: String)
    data class Lesson(val id: UUID, val courseId: UUID, val title: String)

    fun course(
        title: String = "Курс ${UUID.randomUUID()}",
        published: Boolean = true,
        authorId: UUID? = null,
    ): Course {
        val id = UUID.randomUUID()
        jdbc.update(
            """INSERT INTO courses (id, title, description, price, duration, difficulty, is_published, author_id)
               VALUES (?, ?, 'Описание курса для теста', 1990.00, 10, 'BEGINNER', ?, ?)""",
            id, title, published, authorId,
        )
        return Course(id, title)
    }

    fun lesson(course: Course, order: Int, title: String = "Урок $order", duration: Int = 10): Lesson {
        val id = UUID.randomUUID()
        jdbc.update(
            """INSERT INTO lessons (id, course_id, title, content, duration, "order", is_published)
               VALUES (?, ?, ?, 'Текст урока', ?, ?, true)""",
            id, course.id, title, duration, order,
        )
        jdbc.update("UPDATE courses SET lessons_count = lessons_count + 1 WHERE id = ?", course.id)
        return Lesson(id, course.id, title)
    }
}

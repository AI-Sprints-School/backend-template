package com.learning.spring

import com.learning.spring.catalog.CourseRepository
import com.learning.spring.catalog.CourseService
import com.learning.spring.catalog.LessonService
import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Урок 2: сервисы каталога и доступ к данным — бины контекста, а не объекты ручной сборки. */
@Tag("core")
@Tag("chapter1")
class BeansContextTest : IntegrationTest() {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `course service is a bean`() {
        assertNotNull(context.getBeanProvider(CourseService::class.java).getIfAvailable(), "Нет бина CourseService: класс без стереотипа?")
    }

    @Test
    fun `lesson service is a bean`() {
        assertNotNull(context.getBeanProvider(LessonService::class.java).getIfAvailable(), "Нет бина LessonService: класс без стереотипа?")
    }

    @Test
    fun `course repository is a single bean`() {
        val repositories = context.getBeansOfType(CourseRepository::class.java)
        assertEquals(1, repositories.size, "Нужен ровно один бин CourseRepository, найдено: ${repositories.keys}")
    }
}

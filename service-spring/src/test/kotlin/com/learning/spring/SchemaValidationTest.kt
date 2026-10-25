package com.learning.spring

import com.learning.spring.support.IntegrationTest
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Урок 8: сущности `Course` и `Lesson` легли на схему Flyway, Hibernate её только проверяет (`validate`). */
@Tag("core")
@Tag("chapter2")
class SchemaValidationTest : IntegrationTest() {

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `hibernate validates schema instead of creating it`() {
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"), "Нужно spring.jpa.hibernate.ddl-auto: validate")
    }

    @Test
    fun `course and lesson entities are mapped`() {
        val names = entityManagerFactory.metamodel.entities.map { it.name }.toSet()
        assertTrue(names.containsAll(setOf("Course", "Lesson")), "Сущности в метамодели: $names")
    }
}

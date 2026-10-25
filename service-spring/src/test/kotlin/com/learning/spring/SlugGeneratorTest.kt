package com.learning.spring

import com.learning.spring.legacy.SlugGenerator
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Урок 19: генератор слага. Тест вызывает `SlugGenerator.slugify(...)` и работает и с выданным
 * Java-классом, и с Kotlin-версией (`object SlugGenerator` в том же пакете `legacy`).
 * Контракт Kotlin-версии: возвращает `String`, не `null`; название без латинских букв и цифр — `untitled`.
 */
@Tag("core")
@Tag("chapter5")
class SlugGeneratorTest {

    @Test
    fun `latin words are joined with dashes`() = assertEquals("kotlin-android", SlugGenerator.slugify("Kotlin для Android"))

    @Test
    fun `case is lowered and edges are trimmed`() = assertEquals("advanced-android-architecture", SlugGenerator.slugify("  Advanced   Android, Architecture! "))

    @Test
    fun `digits are kept`() = assertEquals("spring-boot-4-1", SlugGenerator.slugify("Spring Boot 4.1"))

    @Test
    fun `title without latin letters and digits gives untitled`() =
        assertEquals("untitled", SlugGenerator.slugify("Основы программирования"), "Название только из кириллицы: платформенный тип отдал null")

    @Test
    fun `slug is at most 60 characters and has no trailing dash`() {
        val slug = SlugGenerator.slugify("word ".repeat(30))
        assertTrue(slug.length <= 60 && !slug.endsWith("-"), "Слаг: '$slug'")
    }
}

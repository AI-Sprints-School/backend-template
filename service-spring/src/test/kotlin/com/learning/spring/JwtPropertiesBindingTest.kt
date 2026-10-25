package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ClassPathResource
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Урок 3: настройки JWT привязаны к классу `JwtProperties` (префикс `jwt`, поля `secret`,
 * `issuer`, `audience`, `accessTokenTtl: Duration`), порт задан в `application.yaml`.
 * Класс ищется по аннотации, а не по имени, чтобы тест компилировался на чистом шаблоне.
 */
@Tag("core")
@Tag("chapter1")
class JwtPropertiesBindingTest : IntegrationTest() {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `jwt properties are bound to a configuration properties bean`() {
        val bean = context.getBeansWithAnnotation(ConfigurationProperties::class.java).values
            .firstOrNull { context.findAnnotationOnBean(context.getBeanNamesForType(it.javaClass).first(), ConfigurationProperties::class.java)?.let { a -> a.prefix.ifEmpty { a.value } } == "jwt" }
            ?: error("Нет бина с @ConfigurationProperties(\"jwt\"): класс JwtProperties не создан или не найден сканированием")
        fun property(name: String): Any? = bean.javaClass.methods.firstOrNull { it.name == "get" + name.replaceFirstChar(Char::uppercase) }?.invoke(bean)
            ?: error("В ${bean.javaClass.simpleName} нет поля $name")

        listOf("secret", "issuer", "audience").forEach {
            assertTrue((property(it) as String).isNotBlank(), "jwt.$it пуст")
        }
        assertEquals(environment.getProperty("jwt.secret"), property("secret"), "Секрет в классе не совпал с настройкой jwt.secret")
        assertTrue((property("accessTokenTtl") as Duration) > Duration.ZERO, "jwt.access-token-ttl должен быть положительным")
    }

    @Test
    fun `server port is 8081 in application yaml`() {
        val sources = YamlPropertySourceLoader().load("application", ClassPathResource("application.yaml"))
        val port = sources.firstNotNullOfOrNull { it.getProperty("server.port") }
        assertEquals(8081, port.toString().toInt(), "Порт сервиса — 8081: на 8080 живёт Ktor-двойник")
    }
}

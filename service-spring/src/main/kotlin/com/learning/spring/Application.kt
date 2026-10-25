package com.learning.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Точка входа сервиса. Контейнер сканирует этот пакет и все пакеты ниже,
 * поэтому все классы курса лежат под `com.learning.spring`.
 * `@ConfigurationPropertiesScan` находит классы с `@ConfigurationProperties` (урок 3).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

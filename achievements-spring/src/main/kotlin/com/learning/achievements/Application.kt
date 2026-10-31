package com.learning.achievements

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Сервис достижений: читает событие «урок пройден» из Kafka и выдаёт достижение «курс пройден». */
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

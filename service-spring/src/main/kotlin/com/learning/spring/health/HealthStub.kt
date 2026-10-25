package com.learning.spring.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Заглушка каркаса: ручной маршрут `/health`. Запрос доходит сюда и падает на `TODO` —
 * это стартовое состояние (урок 1). Урок 4 заменяет маршрут группами проб Actuator
 * (`/health`, `/health/live`, `/health/ready`), этот файл удаляется.
 */
@RestController
class HealthStub {
    @GetMapping("/health")
    fun health(): Map<String, String> =
        TODO("Глава 1, урок 4: /health, /health/live и /health/ready отдаёт Actuator, ручной маршрут убрать")
}

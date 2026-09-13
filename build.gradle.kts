// Версии плагинов — одни на все модули. Сами плагины подключают модули
// (service/build.gradle.kts, позже coroutines/build.gradle.kts).
plugins {
    kotlin("jvm") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
    id("io.ktor.plugin") version "3.5.2" apply false
    id("com.adarshr.test-logger") version "4.0.0" apply false
}

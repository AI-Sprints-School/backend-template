import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Второй сервис курса «Spring Boot на Kotlin», глава 6: получатель события «урок пройден»,
// выдаёт достижение «курс пройден». Версии — как у service-spring/: модуль подключается
// копированием папки и одной строки include в settings.gradle.kts.
val kotlin_version = "2.4.20"
val postgres_version = "42.7.13"
val testcontainers_version = "2.0.5"

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.spring") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.jpa") version "2.4.20"
    id("org.springframework.boot") version "4.1.2"
    id("com.adarshr.test-logger")
}

group = "com.learning"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    // JDK 25 LTS; машина без JDK 25: -PjavaToolchain=24
    jvmToolchain(providers.gradleProperty("javaToolchain").map(String::toInt).getOrElse(25))
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlin_version"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // Spring for Apache Kafka — с первого дня: модуль существует ради консьюмера
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    runtimeOnly("org.postgresql:postgresql:$postgres_version")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Тестовый стартер Kafka Boot 4: spring-kafka-test (ContainerTestUtils, ожидание назначения партиций)
    testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainers_version")
    testImplementation("org.testcontainers:testcontainers-kafka:$testcontainers_version")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainers_version")
    // Ожидание асинхронного результата с таймаутом вместо Thread.sleep (урок 25); версия — из BOM Boot
    testImplementation("org.awaitility:awaitility-kotlin")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 5000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showStandardStreams = false
}

// Отчёт прогона один на оба сервиса — его пишет :service-spring:testReport -Pchapter=6 и дальше.
// Здесь только теги: build/test-results/tags.tsv. Готовая часть шаблона — править не нужно.
val tagsFile = layout.buildDirectory.file("test-results/tags.tsv")

tasks.test {
    useJUnitPlatform()
    // С -Pchapter красные тесты не роняют сборку: отчёт должен записаться всегда
    ignoreFailures = providers.gradleProperty("chapter").isPresent
    systemProperty("testReport.tagsFile", tagsFile.get().asFile.absolutePath)
    outputs.file(tagsFile)
}

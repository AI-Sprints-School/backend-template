import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Сервис платформы обучения на Spring Boot 4.1 — курс «Spring Boot на Kotlin».
// Версии плагинов Spring заданы здесь, а не в корневом build.gradle.kts: модуль подключается
// студенту программы копированием папки и одной строки include в settings.gradle.kts.
// Kotlin-версия задана явно (плагин корня, 2.4.20) поверх BOM: Boot 4.1 управляет
// Kotlin 2.3.21, а курс идёт на 2.4.20 — как и остальные модули шаблона.
val kotlin_version = "2.4.20"
val postgres_version = "42.7.13"
val testcontainers_version = "2.0.5"

plugins {
    kotlin("jvm")
    // allopen для @Component/@Service/@Configuration/@Transactional и т.п. (урок 8 курса)
    id("org.jetbrains.kotlin.plugin.spring") version "2.4.20"
    // no-arg конструктор для @Entity (урок 8 курса)
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
    // Целевой JDK — 25 LTS, студент ставит только его.
    // Локальная машина без JDK 25: -PjavaToolchain=24.
    jvmToolchain(providers.gradleProperty("javaToolchain").map(String::toInt).getOrElse(25))
}

dependencies {
    // BOM Boot 4.1 + явная версия Kotlin поверх него (BOM тянет 2.3.21)
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlin_version"))

    // Стартеры Boot 4 — по технологиям
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.postgresql:postgresql:$postgres_version")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Kotlin: рефлексия для Spring и Jackson (JSON-модуль Kotlin подключает стартер webmvc)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Глава 3 (уроки 11–14): раскомментируйте на уроке 11 — стартер Security и JWT
    // implementation("org.springframework.boot:spring-boot-starter-security")
    // implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Урок 15: раскомментируйте — клиент OAuth 2.0 для входа через Keycloak (тянет за собой Security,
    // поэтому до урока 11 включать нельзя)
    // implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Глава 6 (уроки 22–23): раскомментируйте на уроке 22 — Spring for Apache Kafka.
    // До урока 22 стартер выключен: иначе каждый запуск сервиса и тестов ждал бы брокер.
    // implementation("org.springframework.boot:spring-boot-starter-kafka")

    // Тесты: стартеры Boot 4 с суффиксом -test по технологиям
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainers_version")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainers_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Глава 6: брокер в Testcontainers (тот же образ apache/kafka:4.2.2, что в Compose), клиент Kafka
    // для чтения топика в тестах и ожидание асинхронного результата. Версии kafka-clients и
    // Awaitility — из BOM Boot. Работают и до урока 22: тесты главы 6 читают топик сами.
    testImplementation("org.testcontainers:testcontainers-kafka:$testcontainers_version")
    testImplementation("org.apache.kafka:kafka-clients")
    testImplementation("org.awaitility:awaitility-kotlin")
    // launcher — для слушателя тегов отчёта прогона (src/test/.../report/TagReportListener.kt)
    testImplementation("org.junit.platform:junit-platform-launcher")
}

testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 2000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showStandardStreams = false
}

// ---------- Отчёт прогона для pull request ----------
// Готовая часть шаблона — править не нужно.
// ./gradlew :service-spring:testReport -Pchapter=N  →  service-spring/reports/tests-0N.txt
// Короткая выдержка (~1 400 знаков): итог, ядро глав 1–N, упавшие тесты ядра главы N.
// С главы 6 в отчёт входят и тесты второго сервиса achievements-spring/ (один отчёт на оба модуля).
// Отчёт детерминирован — без даты, коммита и версии JDK: CI перегенерирует его
// и сверяет с закоммиченным (git diff --exit-code reports/).
val reportChapter = providers.gradleProperty("chapter").map(String::toInt)
val tagsFile = layout.buildDirectory.file("test-results/tags.tsv")

tasks.test {
    // С -Pchapter красные тесты не роняют сборку: отчёт должен записаться всегда
    ignoreFailures = reportChapter.isPresent
    systemProperty("testReport.tagsFile", tagsFile.get().asFile.absolutePath)
    outputs.file(tagsFile)
}

abstract class TestReportTask : DefaultTask() {
    @get:InputFiles abstract val tags: ConfigurableFileCollection
    @get:Input abstract val chapter: Property<Int>
    @get:Input abstract val failOnCore: Property<Boolean>
    @get:OutputFile abstract val report: RegularFileProperty

    @TaskAction
    fun write() {
        data class Row(val cls: String, val name: String, val tags: Set<String>, val status: String) {
            val chapter: Int? = tags.firstNotNullOfOrNull { it.removePrefix("chapter").takeIf { t -> t != it }?.toIntOrNull() }
            val core get() = "core" in tags
            val passed get() = status == "PASS"
        }
        val n = chapter.get()
        val rows = tags.files.filter { it.exists() }.flatMap { it.readLines() }.filter { it.isNotBlank() }.map { line ->
            val (cls, name, tagList, status) = line.split('\t')
            Row(cls, name, tagList.split(',').filter(String::isNotBlank).toSet(), status)
        }.sortedWith(compareBy({ it.chapter ?: 99 }, { it.cls }, { it.name }))

        fun score(list: List<Row>) = "${list.count { it.passed }} из ${list.size}"
        val core = rows.filter { it.core && (it.chapter ?: 99) <= n }
        val out = StringBuilder()
        out.appendLine("# Прогон тестов — глава $n")
        out.appendLine("Выполнено ${rows.count { it.status != "SKIPPED" }} из ${rows.size}")
        out.appendLine("Executed ${rows.size}, passed ${rows.count { it.passed }}, failed ${rows.count { it.status == "FAIL" }}")
        out.appendLine((if (n == 1) "Ядро главы 1: " else "Ядро глав 1–$n: ") + score(core))
        if (n > 1) out.appendLine("По главам: " + (1..n).filter { ch -> core.any { it.chapter == ch } }.joinToString(" · ") { ch ->
            val r = core.filter { it.chapter == ch }; "$ch — ${r.count { it.passed }}/${r.size}"
        })
        out.appendLine("Расширение: ${score(rows.filter { "extension" in it.tags })} · звёздочка: ${score(rows.filter { "star" in it.tags })}" +
            " · ядро следующих глав: ${score(rows.filter { it.core && (it.chapter ?: 99) > n })}")
        out.appendLine()
        out.appendLine("## Классы ядра главы $n")
        core.filter { it.chapter == n }.groupBy { it.cls }.forEach { (cls, list) ->
            out.appendLine("[${if (list.all { it.passed }) "PASS" else "FAIL"}] $cls ${list.count { it.passed }}/${list.size}")
        }
        // Упавшие: сначала глава N, затем регресс прошлых глав; отчёт не длиннее ~1 400 знаков
        val failed = core.filter { !it.passed }.sortedBy { if (it.chapter == n) 0 else 1 }
        if (failed.isNotEmpty()) {
            out.appendLine()
            out.appendLine("## Упавшие тесты ядра (${failed.size})")
            val budget = 1400 - "… и ещё 000\n".length
            var shown = 0
            for (row in failed) {
                val line = "- гл.${row.chapter} ${row.cls} › ${row.name}\n"
                if (out.length + line.length > budget) break
                out.append(line); shown++
            }
            if (shown < failed.size) out.appendLine("… и ещё ${failed.size - shown}")
        }
        val target = report.get().asFile
        target.parentFile.mkdirs()
        target.writeText(out.toString())
        logger.lifecycle("Отчёт: ${target.path} (${out.length} знаков)")
        if (failOnCore.get() && failed.isNotEmpty()) {
            throw GradleException("Ядро глав 1–$n: красных ${failed.size} тестов — см. ${target.name}")
        }
    }
}

tasks.register<TestReportTask>("testReport") {
    group = "verification"
    description = "Прогон тестов и короткий отчёт reports/tests-NN.txt; -Pchapter=N обязателен"
    dependsOn(tasks.test)
    tags.from(tagsFile)
    // Глава 6 и дальше: второй сервис прогоняется вместе с этим и попадает в тот же отчёт
    val achievements = rootProject.findProject(":achievements-spring")
    if (achievements != null && reportChapter.getOrElse(0) >= 6) {
        dependsOn(":achievements-spring:test")
        tags.from(achievements.layout.buildDirectory.file("test-results/tags.tsv"))
    }
    chapter.set(reportChapter.orElse(providers.provider { throw GradleException("Укажите главу: -Pchapter=N") }))
    failOnCore.set(providers.gradleProperty("failOnCore").map(String::toBoolean).orElse(false))
    report.set(reportChapter.map { layout.projectDirectory.file("reports/tests-%02d.txt".format(it)) })
}

// Конфигурация для тестов
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
        showCauses = true
        showExceptions = true
    }
}

// Профиль dev одной опцией: gradle :service-spring:bootRun -Pprofile=dev
// (то же самое — переменная окружения SPRING_PROFILES_ACTIVE=dev в конфигурации запуска IDE)
tasks.bootRun {
    providers.gradleProperty("profile").orNull?.let { systemProperty("spring.profiles.active", it) }
}

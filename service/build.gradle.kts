val ktor_version = "3.5.2"
val kotlin_version = "2.4.20"
val logback_version = "1.6.3"
val postgres_version = "42.7.13"
val exposed_version = "1.5.0"
val bcrypt_version = "0.10.2"
val hikari_version = "7.1.0"
val flyway_version = "13.6.0"
val mockk_version = "1.14.11"
val testcontainers_version = "2.0.5"
val simplejavamail_version = "9.3.4"
val lettuce_version = "7.7.0.RELEASE"

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.adarshr.test-logger")
}

group = "com.learning"
version = "0.0.1"

application {
    mainClass.set("com.learning.ApplicationKt")
}

repositories {
    mavenCentral()
}

kotlin {
    // Целевой JDK — 25 LTS (паспорт §7а), студент ставит только его.
    // Локальная машина без JDK 25: -PjavaToolchain=24.
    jvmToolchain(providers.gradleProperty("javaToolchain").map(String::toInt).getOrElse(25))
}

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")

    // Ktor Plugins
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-di:$ktor_version")
    implementation("io.ktor:ktor-server-request-validation:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")


    // Password hashing
    implementation("at.favre.lib:bcrypt:$bcrypt_version")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")

    // Migrations
    implementation("org.flywaydb:flyway-core:$flyway_version")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flyway_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Rate Limiting
    implementation("io.ktor:ktor-server-rate-limit:$ktor_version")

    // Email
    implementation("org.simplejavamail:simple-java-mail:$simplejavamail_version")

    // Redis (Caching)
    implementation("io.lettuce:lettuce-core:$lettuce_version")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-core:$ktor_version")
    testImplementation("io.ktor:ktor-client-cio:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("io.ktor:ktor-client-logging:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    // launcher — для слушателя тегов отчёта прогона (src/test/.../report/TagReportListener.kt)
    testImplementation("org.junit.platform:junit-platform-launcher")

    // MockK for mocking
    testImplementation("io.mockk:mockk:$mockk_version")

    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainers_version")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainers_version")
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
    showPassedStandardStreams = true
    showSkippedStandardStreams = true
    showFailedStandardStreams = true
}

// ---------- Отчёт прогона для pull request ----------
// Готовая часть шаблона — править не нужно.
// ./gradlew testReport -Pchapter=N  →  reports/tests-0N.txt
// Короткая выдержка (~1 500 знаков): итог, ядро глав 1–N, упавшие тесты ядра главы N.
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
    @get:InputFile abstract val tags: RegularFileProperty
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
        val rows = tags.get().asFile.readLines().filter { it.isNotBlank() }.map { line ->
            val (cls, name, tagList, status) = line.split('\t')
            Row(cls, name, tagList.split(',').filter(String::isNotBlank).toSet(), status)
        }.sortedWith(compareBy({ it.chapter ?: 99 }, { it.cls }, { it.name }))

        fun score(list: List<Row>) = "${list.count { it.passed }} из ${list.size}"
        val core = rows.filter { it.core && (it.chapter ?: 99) <= n }
        val out = StringBuilder()
        out.appendLine("# Прогон тестов — глава $n")
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
        // Упавшие: сначала глава N, затем регресс прошлых глав; отчёт не длиннее ~1 500 знаков
        val failed = core.filter { !it.passed }.sortedBy { if (it.chapter == n) 0 else 1 }
        if (failed.isNotEmpty()) {
            out.appendLine()
            out.appendLine("## Упавшие тесты ядра (${failed.size})")
            val budget = 1500 - "… и ещё 000\n".length
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
    tags.set(tagsFile)
    chapter.set(reportChapter.orElse(providers.provider { throw GradleException("Укажите главу: -Pchapter=N") }))
    failOnCore.set(providers.gradleProperty("failOnCore").map(String::toBoolean).orElse(false))
    report.set(reportChapter.map { layout.projectDirectory.file("reports/tests-%02d.txt".format(it)) })
}

// Конфигурация для тестов
tasks.test {
    useJUnitPlatform()
    // MockK подгружает агент ByteBuddy на лету; с JDK 21+ без флага — предупреждение
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        showCauses = true
        showExceptions = true
    }
}

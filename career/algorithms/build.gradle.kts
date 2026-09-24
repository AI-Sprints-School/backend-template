// Модуль курса «Карьерная подготовка бэкенд-разработчика», глава 4
// («Алгоритмы и live-coding»). Готовая часть шаблона — править не нужно.
//
//   ./gradlew :career:test                        → все тесты главы 4
//   ./gradlew :career:testReport -Plesson=13       → тесты урока, career/reports/algorithms-13.txt
//
// Проект подключён под именем :career (settings.gradle.kts), а физически лежит
// в career/algorithms/ — рядом, в career/, живут файлы без кода: search-log.md,
// vacancies.md и другие накопительные артефакты курса. Отчёт пишется в
// career/reports/, а не внутрь career/algorithms/reports/ — так же на него
// ссылается PROGRAM.md курса.
//
// Задача — в группе course окна Gradle. Запускать из IDE через Run Anything:
// так Gradle берёт JDK из настройки Gradle JVM.

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    kotlin("jvm")
}

group = "career"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    // JDK 25 LTS. На машине без JDK 25 можно временно собрать с -PjavaToolchain=24.
    jvmToolchain(providers.gradleProperty("javaToolchain").map(String::toInt).getOrElse(25))
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---------- Номер урока: -Plesson=NN (13..16) ----------

val lessonNumber: Provider<String> = providers.gradleProperty("lesson").map { raw ->
    val n = raw.trim()
    if (!Regex("""\d{2}""").matches(n) || n.toInt() !in 13..16) {
        throw GradleException("укажите урок главы 4 двумя цифрами от 13 до 16: -Plesson=13 (получено: $raw)")
    }
    n
}
val lessonRequired: Provider<String> = providers.gradleProperty("lesson").orElse("").map { raw ->
    if (raw.isBlank()) throw GradleException("укажите урок: -Plesson=13")
    val n = raw.trim()
    if (!Regex("""\d{2}""").matches(n) || n.toInt() !in 13..16) {
        throw GradleException("укажите урок главы 4 двумя цифрами от 13 до 16: -Plesson=13 (получено: $raw)")
    }
    n
}

// ---------- test и testReport ----------

tasks.test {
    useJUnitPlatform()
    val lesson = lessonNumber.orNull
    if (lesson != null) {
        filter {
            includeTestsMatching("algorithms.l$lesson.*")
            isFailOnNoMatchingTests = false
        }
    }
    // отчёт пишется и при красных тестах
    ignoreFailures = true
}

abstract class LessonTestReport : DefaultTask() {
    @get:Input abstract val lesson: Property<String>
    @get:InputDirectory abstract val results: DirectoryProperty
    @get:OutputFile abstract val report: RegularFileProperty

    @TaskAction
    fun write() {
        val n = lesson.get()
        val prefix = "algorithms.l$n."
        data class Case(val cls: String, val name: String, val failure: String?)

        val cases = mutableListOf<Case>()
        val factory = DocumentBuilderFactory.newInstance()
        results.get().asFile.listFiles { f -> f.name.endsWith(".xml") }.orEmpty().forEach { file ->
            val doc = factory.newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("testcase")
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as Element
                val cls = el.getAttribute("classname")
                if (!cls.startsWith(prefix)) continue
                val problem = (el.getElementsByTagName("failure").item(0)
                    ?: el.getElementsByTagName("error").item(0)) as Element?
                val skipped = el.getElementsByTagName("skipped").length > 0
                val failure = when {
                    problem != null -> describe(problem)
                    skipped -> "тест пропущен"
                    else -> null
                }
                cases += Case(cls, el.getAttribute("name").removeSuffix("()"), failure)
            }
        }
        cases.sortWith(compareBy({ it.cls }, { it.name }))

        val red = cases.count { it.failure != null }
        fun render(messageLimit: Int): String = buildString {
            appendLine("Урок $n · career/algorithms · Kotlin 2.4.20")
            appendLine("Выполнено ${cases.size}, зелёных ${cases.size - red}, красных $red")
            cases.groupBy { it.cls }.forEach { (cls, list) ->
                appendLine()
                appendLine(cls)
                list.forEach { c ->
                    if (c.failure == null) appendLine("  [PASS] ${c.name}")
                    else if (messageLimit == 0) appendLine("  [FAIL] ${c.name}")
                    else appendLine("  [FAIL] ${c.name}: ${shorten(c.failure, messageLimit)}")
                }
            }
            appendLine(
                when {
                    cases.isEmpty() -> "Итог: тестов урока не найдено — урок не сдан"
                    red == 0 -> "Итог: все тесты урока зелёные"
                    else -> "Итог: красных $red — урок не сдан"
                }
            )
        }
        // лимит отчёта — 1500 знаков: при превышении сообщения об ошибках укорачиваются
        val text = listOf(100, 70, 40, 0).map(::render).firstOrNull { it.length <= 1500 } ?: render(0)

        val target = report.get().asFile
        target.parentFile.mkdirs()
        target.writeText(text)
        logger.lifecycle("Отчёт: career/reports/${target.name} (${text.length} знаков)")
        logger.lifecycle(text.trimEnd().lines().last())
    }

    private fun describe(problem: Element): String {
        val type = problem.getAttribute("type")
        val message = problem.getAttribute("message").ifBlank { problem.textContent.lineSequence().firstOrNull().orEmpty() }
            .removePrefix("$type: ").removePrefix(type)
        val assertion = type.endsWith("AssertionFailedError") || type.endsWith("AssertionError")
        val text = if (assertion) message else "${type.substringAfterLast('.')}: $message"
        return text
            .replace("An operation is not implemented: ", "")
            .replace(Regex("""\b(?:[a-z][a-z0-9]*\.)+([A-Z]\w*(?:Exception|Error|Failure))"""), "$1")
            .replace(Regex("""expected: <(.*)> but was: <(.*)>""", RegexOption.DOT_MATCHES_ALL), "expected:<$1> but was:<$2>")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun shorten(text: String, limit: Int) = if (text.length <= limit) text else text.take(limit - 1) + "…"
}

tasks.register<LessonTestReport>("testReport") {
    group = "course"
    description = "Прогоняет тесты урока и пишет career/reports/algorithms-NN.txt; -Plesson=NN обязателен"
    dependsOn(tasks.test)
    lesson.set(lessonRequired)
    results.set(layout.buildDirectory.dir("test-results/test"))
    // career/reports/ — на уровень выше career/algorithms/, рядом с остальными
    // накопительными файлами курса; отсюда ../reports относительно projectDir.
    val reportsDir = layout.projectDirectory.dir("../reports")
    report.set(lessonRequired.map { reportsDir.file("algorithms-$it.txt") })
}

// Модуль курса «Практикум по Java для Kotlin-разработчика» (java-praktikum).
// Готовая часть шаблона — править не нужно.
//
//   ./gradlew :java-core:test                      → все тесты модуля
//   ./gradlew :java-core:testReport -Plesson=05     → тесты урока, java-core/reports/tests-05.txt
//
// Уроки с задачей в репозитории: 05, 10, 12, 15, 17, 22, 24, 25, 28, 29. Код урока NN
// лежит в пакете lessonNN (src/main/java/lessonNN, тесты — src/test/java/lessonNN).
// Kotlin в модуле нужен только урокам 28 и 29 (src/main/kotlin).
//
// Задача — в группе course окна Gradle. Запускать из IDE через Run Anything:
// так Gradle берёт JDK из настройки Gradle JVM.

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    kotlin("jvm")
}

group = "javacore"
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
    // Урок 28: сюда переносится зависимость из maven/lesson28/pom.xml.
}

// ---------- Номер урока: -Plesson=NN (только уроки с задачей в репозитории) ----------

val prLessons = setOf(5, 10, 12, 15, 17, 22, 24, 25, 28, 29)

// Номер проверяется сразу при конфигурации: так в задачах нет ссылок на скрипт
// (иначе кеш конфигурации Gradle не сериализует задачу).
val lessonProp: String? = providers.gradleProperty("lesson").orNull?.trim()?.padStart(2, '0')
if (lessonProp != null && (!Regex("""\d{2}""").matches(lessonProp) || lessonProp.toInt() !in prLessons)) {
    throw GradleException(
        "укажите урок с задачей в репозитории: -Plesson=05 или -Plesson=5 " +
            "(доступны: ${prLessons.sorted().joinToString { "%02d".format(it) }}; получено: $lessonProp)"
    )
}

// ---------- test и testReport ----------

tasks.test {
    useJUnitPlatform()
    val lesson = lessonProp
    if (lesson != null) {
        filter {
            includeTestsMatching("lesson$lesson.*")
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
        if (n.isBlank()) throw GradleException("укажите урок: -Plesson=05")
        val prefix = "lesson$n."
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
            appendLine("Урок $n · java-core · Java 25")
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
        logger.lifecycle("Отчёт: java-core/reports/${target.name} (${text.length} знаков)")
        logger.lifecycle(text.trimEnd().lines().last())
    }

    private fun describe(problem: Element): String {
        val type = problem.getAttribute("type")
        val message = problem.getAttribute("message").ifBlank { problem.textContent.lineSequence().firstOrNull().orEmpty() }
            .removePrefix("$type: ").removePrefix(type)
        val assertion = type.endsWith("AssertionFailedError") || type.endsWith("AssertionError")
        val text = if (assertion) message else "${type.substringAfterLast('.')}: $message"
        return text
            .replace(Regex("""\b(?:[a-z][a-z0-9]*\.)+([A-Z]\w*(?:Exception|Error|Failure))"""), "$1")
            .replace(Regex("""expected: <(.*)> but was: <(.*)>""", RegexOption.DOT_MATCHES_ALL), "expected:<$1> but was:<$2>")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun shorten(text: String, limit: Int) = if (text.length <= limit) text else text.take(limit - 1) + "…"
}

tasks.register<LessonTestReport>("testReport") {
    group = "course"
    description = "Прогоняет тесты урока и пишет java-core/reports/tests-NN.txt; -Plesson=NN обязателен"
    dependsOn(tasks.test)
    lesson.set(lessonProp ?: "")
    results.set(layout.buildDirectory.dir("test-results/test"))
    val reportsDir = layout.projectDirectory.dir("reports")
    report.set(reportsDir.file("tests-${lessonProp ?: "NN"}.txt"))
}

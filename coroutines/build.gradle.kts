// Модуль курса «Корутины и Flow на сервере». Готовая часть шаблона — править не нужно.
//
//   gradle :coroutines:runLesson -Plesson=01    → лог урока, reports/run-01.txt
//   gradle :coroutines:testReport -Plesson=01   → тесты урока, reports/tests-01.txt
//
// Обе задачи — в группе course окна Gradle. Запускать из IDE через Run Anything:
// так Gradle берёт JDK из настройки Gradle JVM.

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    kotlin("jvm")
    application
}

group = "loader"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    // JDK 25 LTS. На машине без JDK 25 можно временно собрать с -PjavaToolchain=24.
    jvmToolchain(providers.gradleProperty("javaToolchain").map(String::toInt).getOrElse(25))
}

val coroutinesVersion = "1.11.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    // урок 9 вызывает DebugProbes из Main.kt
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

// ---------- Номер урока: -Plesson=NN ----------

val lessonNumber: Provider<String> = providers.gradleProperty("lesson").map { raw ->
    val n = raw.trim()
    if (!Regex("""\d{2}""").matches(n) || n.toInt() !in 1..15) {
        throw GradleException("укажите урок двумя цифрами от 01 до 15: -Plesson=01 (получено: $raw)")
    }
    n
}
val lessonRequired: Provider<String> = providers.gradleProperty("lesson").orElse("").map { raw ->
    if (raw.isBlank()) throw GradleException("укажите урок: -Plesson=01")
    val n = raw.trim()
    if (!Regex("""\d{2}""").matches(n) || n.toInt() !in 1..15) {
        throw GradleException("укажите урок двумя цифрами от 01 до 15: -Plesson=01 (получено: $raw)")
    }
    n
}

application {
    mainClass.set(lessonNumber.map { "loader.lesson$it.MainKt" }.orElse("loader.lesson01.MainKt"))
}

// ---------- runLesson ----------


tasks.register<JavaExec>("runLesson") {
    group = "course"
    description = "Запускает сценарий урока и пишет лог в reports/run-NN.txt; -Plesson=NN обязателен"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(lessonRequired.map { "loader.lesson$it.MainKt" })
    workingDir = projectDir
    // режим отладки дописывает к имени потока имя корутины: main @coroutine#2
    // --enable-native-access: DebugProbes урока 9 подгружает агент через JNA, без флага JDK 24+ печатает предупреждение
    jvmArgs("-Dkotlinx.coroutines.debug", "-XX:+EnableDynamicAgentLoading", "--enable-native-access=ALL-UNNAMED")
    val reportsDir = layout.projectDirectory.dir("reports")
    val reportFile = lessonRequired.map { reportsDir.file("run-$it.txt").asFile.absolutePath }
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Dloader.report=${reportFile.get()}") })
}

// ---------- test и testReport ----------

tasks.test {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "--enable-native-access=ALL-UNNAMED")
    // имя корутины в имени потока — его проверяет тест урока 6
    systemProperty("kotlinx.coroutines.debug", "on")
    val lesson = lessonNumber.orNull
    if (lesson != null) {
        filter {
            includeTestsMatching("loader.lesson$lesson.*")
            isFailOnNoMatchingTests = false
        }
        // отчёт пишется и при красных тестах
        ignoreFailures = true
    }
}

abstract class LessonTestReport : DefaultTask() {
    @get:Input abstract val lesson: Property<String>
    @get:InputDirectory abstract val results: DirectoryProperty
    @get:OutputFile abstract val report: RegularFileProperty

    @TaskAction
    fun write() {
        val n = lesson.get()
        val prefix = "loader.lesson$n."
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
            appendLine("Урок $n · kotlinx.coroutines 1.11.0 · Kotlin 2.4.20")
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
        logger.lifecycle("Отчёт: reports/${target.name} (${text.length} знаков)")
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
    description = "Прогоняет тесты урока и пишет reports/tests-NN.txt; -Plesson=NN обязателен"
    dependsOn(tasks.test)
    lesson.set(lessonRequired)
    results.set(layout.buildDirectory.dir("test-results/test"))
    val reportsDir = layout.projectDirectory.dir("reports")
    report.set(lessonRequired.map { reportsDir.file("tests-$it.txt") })
}

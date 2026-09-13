package com.learning.report

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File

/**
 * Записывает итог каждого теста вместе с тегами (`core`, `chapterN`, `extension`,
 * `star`) в `build/test-results/tags.tsv`. Файл читает Gradle-цель `testReport`:
 * в XML-отчётах JUnit тегов нет.
 *
 * Слушатель подключается сам — через `META-INF/services`, править его не нужно.
 */
class TagReportListener : TestExecutionListener {

    private val file: File? = System.getProperty("testReport.tagsFile")?.let(::File)

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        file?.apply { parentFile.mkdirs(); writeText("") }
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String) {
        if (testIdentifier.isTest) write(testIdentifier, "SKIPPED")
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (!testIdentifier.isTest) return
        write(testIdentifier, if (testExecutionResult.status == TestExecutionResult.Status.SUCCESSFUL) "PASS" else "FAIL")
    }

    @Synchronized
    private fun write(test: TestIdentifier, status: String) {
        val target = file ?: return
        val source = test.source.orElse(null) as? MethodSource
        val className = source?.className?.substringAfterLast('.') ?: "?"
        val name = source?.methodName ?: test.displayName
        val tags = test.tags.joinToString(",") { it.name }
        target.appendText("$className\t$name\t$tags\t$status\n")
    }
}

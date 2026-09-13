package loader.lesson02

import kotlinx.coroutines.runBlocking
import loader.core.FakeJournal
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 2 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=02

private val SOURCES = listOf("alpha", "beta", "gamma", "epsilon", "omega")

fun main() = lessonMain {
    runBlocking {
        Log.plain("опрос 1: параллельно, pollSources")
        val catalog = SourceCatalog()
        val journal = FakeJournal()
        var reports: List<SourceReport>
        var millis = measureTimeMillis { reports = pollSources(SOURCES, catalog, journal) }
        Log.plain("параллельно занял ${seconds(millis)} с, ответы: ${reports.joinToString { it.sourceId }}")
        Log.plain("строк в журнале: ${journal.records.size} — ${journal.records.joinToString(" | ")}")

        Log.plain("опрос 2: подряд, цикл шаблона")
        val second = SourceCatalog()
        millis = measureTimeMillis { reports = pollSequentially(SOURCES, second) }
        Log.plain("подряд занял ${seconds(millis)} с, ответы: ${reports.joinToString { it.sourceId }}")
    }
}

/** Последовательный опрос — точка сравнения. */
private suspend fun pollSequentially(sourceIds: List<String>, catalog: SourceCatalog): List<SourceReport> {
    val result = mutableListOf<SourceReport>()
    for (id in sourceIds) {
        result += SourceReport(id, catalog.source(id).download(catalog.firstFileOf(id)))
    }
    return result
}

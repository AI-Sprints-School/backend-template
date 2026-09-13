package loader.lesson10

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 10 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=10
// Снимок для ревью — snapshot/JobSchedulerSnapshot.kt, ревью — coroutines/review/job-scheduler-review.md

fun main() = lessonMain {
    runBlocking {
        Log.plain("пакет 1: a.csv, d.csv, broken.csv, c.csv")
        val catalog = SourceCatalog()
        val files = listOf("a.csv", "d.csv", "broken.csv", "c.csv").map(catalog::file)
        var outcome: BatchOutcome? = null
        val millis = measureTimeMillis { outcome = downloadIsolated(files, catalog) }
        Log.plain("пакет 1: ${seconds(millis)} с, успех: ${outcome?.succeeded?.joinToString { it.file.name }}")
        outcome?.failed?.forEach { (name, e) -> Log.plain("пакет 1: сбой $name — ${e.message}") }

        Log.plain("пакет 2: broken.csv, отмена через 0,6 с — во время паузы перед второй попыткой")
        val second = SourceCatalog()
        val job = launch { downloadIsolated(listOf(second.file("broken.csv")), second) }
        delay(600)
        job.cancelAndJoin()
        Log.plain("пакет 2: отменён, попыток sigma: ${second.source("sigma").stats.attempts}")
        delay(1000)
        Log.plain("через 1 с после отмены попыток sigma: ${second.source("sigma").stats.attempts}")
    }
}

package loader.lesson07

import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.Payload
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

// Сценарий урока 7 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=07

fun main() = lessonMain {
    runBlocking {
        Log.plain("пакет 1: b.csv, c.csv")
        val catalog = SourceCatalog()
        val completed = AtomicInteger()
        var result: List<Payload> = emptyList()
        var millis = measureTimeMillis {
            result = downloadBatch(listOf("b.csv", "c.csv").map(catalog::file), catalog, completed)
        }
        Log.plain("пакет 1: ${seconds(millis)} с, результатов ${result.size}, completed = ${completed.get()}")

        Log.plain("пакет 2: b.csv, broken.csv, c.csv")
        val second = SourceCatalog()
        val completedSecond = AtomicInteger()
        millis = measureTimeMillis {
            try {
                downloadBatch(listOf("b.csv", "broken.csv", "c.csv").map(second::file), second, completedSecond)
                Log.plain("пакет 2: исключения нет")
            } catch (e: IOException) {
                Log.plain("пакет 2: исключение — ${e.message}")
            }
        }
        Log.plain("пакет 2: ${seconds(millis)} с, completed = ${completedSecond.get()}")
    }
}

package loader.lesson03

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.ThreadedCallbackClient
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 3 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=03

fun main() = lessonMain {
    runBlocking {
        val catalog = SourceCatalog()
        val client = ThreadedCallbackClient(catalog)
        val files = listOf(catalog.file("a.csv"), catalog.file("c.csv"))

        Log.plain("режим 1: две цепочки, fetchFile")
        var millis = measureTimeMillis {
            coroutineScope { files.forEach { file -> launch { fetchFile(client, "student", file) } } }
        }
        Log.plain("режим 1 занял ${seconds(millis)} с")

        Log.plain("режим 2: две цепочки, fetchFileBlocking")
        millis = measureTimeMillis {
            coroutineScope { files.forEach { file -> launch { fetchFileBlocking(client, "student", file) } } }
        }
        Log.plain("режим 2 занял ${seconds(millis)} с")
    }
}

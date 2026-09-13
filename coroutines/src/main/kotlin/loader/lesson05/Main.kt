package loader.lesson05

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 5 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=05

fun main() = lessonMain {
    runBlocking {
        val catalog = SourceCatalog()
        val registry = DownloadRegistry(this, catalog)
        Log.plain("три загрузки, через 0,3 с отмена c")
        val millis = measureTimeMillis {
            registry.start("b", catalog.file("b.csv"))
            registry.start("c", catalog.file("c.csv"))
            registry.start("e", catalog.file("e.csv"))
            delay(300)
            Log.plain("cancel(c) = ${registry.cancel("c")}")
            registry.awaitAll()
        }
        Log.plain("awaitAll вернулся через ${seconds(millis)} с")
        registry.states().forEach { (id, state) -> Log.plain("$id: $state") }
        Log.plain("cancel(c) после завершения = ${registry.cancel("c")}, cancel(нет) = ${registry.cancel("нет")}")
    }
}

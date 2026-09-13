package loader.lesson13

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.log

// Сценарий урока 13 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=13

fun main() = lessonMain {
    runBlocking {
        val catalog = SourceCatalog()
        val loader = StatefulLoader(this, catalog)
        val observer = launch {
            loader.state.collect { log("состояние: $it") }
        }
        delay(50)
        Log.plain("start: a.csv, broken.csv, c.csv")
        loader.start(listOf("a.csv", "broken.csv", "c.csv").map(catalog::file))
        delay(700)
        Log.plain("stop через 0,7 с")
        loader.stop()
        delay(50)
        observer.cancel()
        Log.plain("итог: ${loader.state.value}")
    }
}

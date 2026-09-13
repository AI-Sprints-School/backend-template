package loader.lesson04

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain

// Сценарий урока 4 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=04

fun main() = lessonMain {
    runBlocking {
        Log.plain("сервис 1: LoaderService")
        val catalog = SourceCatalog()
        val service = LoaderService(catalog)
        service.start(listOf(catalog.file("a.csv"), catalog.file("b.csv")))
        delay(2500)
        service.stop()
        delay(2000)
        Log.plain("после stop прошло 2 с, загрузок начато: ${catalog.stats.started}")

        Log.plain("сервис 2: GlobalLoader, первые 2,5 с — лог скрыт")
        val second = SourceCatalog()
        val global = GlobalLoader(second)
        Log.muted {
            global.start(listOf(second.file("a.csv"), second.file("b.csv")))
            delay(2500)
        }
        Log.plain("до stop загрузок начато: ${second.stats.started}")
        global.stop()
        delay(2000)
        Log.plain("после stop прошло 2 с, загрузок начато: ${second.stats.started}")
    }
}

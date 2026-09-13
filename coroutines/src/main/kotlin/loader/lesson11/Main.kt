package loader.lesson11

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import loader.core.FakeDisk
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.log

// Сценарий урока 11 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=11

fun main() = lessonMain {
    runBlocking {
        val catalog = SourceCatalog()
        val flow = progressFlow(catalog.file("c.csv"), catalog)
        log("поток создан")
        delay(500)
        log("прошло 0,5 с, загрузок начато: ${catalog.stats.started}")

        Log.plain("collect 1")
        flow.collect { log("прогресс ${it.file}: ${it.percent} %") }
        Log.plain("collect 2")
        flow.collect { }
        Log.plain("после двух collect загрузок начато: ${catalog.stats.started}")

        Log.plain("storedLines на Dispatchers.IO")
        val disk = FakeDisk()
        disk.putLines("report.txt", listOf("строка 1", "строка 2", "строка 3"))
        storedLines(disk, "report.txt", Dispatchers.IO).collect { log("получена $it") }
        Log.plain("чтение шло на потоках: ${disk.readThreads}")
    }
}

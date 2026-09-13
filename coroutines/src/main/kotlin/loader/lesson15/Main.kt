package loader.lesson15

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.log
import loader.lesson13.StatefulLoader
import loader.lesson14.DownloadEvent
import loader.lesson14.EventBus

// Сценарий урока 15 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=15

fun main() = lessonMain {
    runBlocking {
        Log.plain("сводка сервиса")
        val catalog = SourceCatalog(quiet = true)
        val loader = StatefulLoader(this, catalog)
        val bus = EventBus(replay = 0)
        val report = launch { summary(loader, bus, this).collect { log("сводка: $it") } }
        delay(50)
        loader.start(listOf(catalog.file("a.csv")))
        delay(50)
        bus.publish(DownloadEvent.Completed("a.csv", 400))
        bus.publish(DownloadEvent.Failed("broken.csv", "источник sigma недоступен"))
        delay(50)
        loader.stop()
        delay(50)
        report.cancel()

        Log.plain("лента: FlakyFeed(2) и FlakyFeed(5)")
        for (failures in listOf(2, 5)) {
            val feed = FlakyFeed(failures)
            val items = resilientFeed(feed).toList()
            Log.plain("FlakyFeed($failures): попыток ${feed.attempts}, элементы: $items")
        }

        Log.plain("события из колбэков")
        val source = CallbackEventSource()
        val listener = launch { callbackEvents(source).collect { log("событие: $it") } }
        delay(50)
        Log.plain("слушателей: ${source.listeners}")
        source.fire("диск заполнен на 80 %")
        delay(50)
        listener.cancel()
        listener.join()
        Log.plain("после отмены сборщика слушателей: ${source.listeners}")
    }
}

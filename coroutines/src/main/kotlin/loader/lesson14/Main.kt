package loader.lesson14

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loader.core.FakeJournal
import loader.core.Log
import loader.core.lessonMain
import loader.core.log

// Сценарий урока 14 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=14

private val SIX = listOf(
    DownloadEvent.Completed("a.csv", 400),
    DownloadEvent.Completed("b.csv", 700),
    DownloadEvent.Failed("broken.csv", "источник sigma недоступен"),
    DownloadEvent.Completed("c.csv", 1000),
    DownloadEvent.Failed("d.csv", "источник delta недоступен"),
    DownloadEvent.Completed("e.csv", 800),
)

fun main() = lessonMain {
    runBlocking {
        Log.plain("шина 1: replay = 1, два подписчика, шесть событий")
        val bus = EventBus()
        val journal = FakeJournal()
        val stats = DownloadStats()
        val journalJob = launchJournal(bus, journal)
        val statsJob = launchStats(bus, stats)
        delay(50)
        SIX.forEach { bus.publish(it) }
        delay(1000)
        Log.plain("журнал: ${journal.records.size} строк, статистика: готово ${stats.completed}, сбоев ${stats.failed}")
        Log.plain("опоздавший подписчик получил: ${bus.events.first()}")
        journalJob.cancel()
        statsJob.cancel()

        Log.plain("шина 2: replay = 0, буфер 2, DROP_OLDEST, медленный подписчик")
        val lossy = EventBus(replay = 0, extraBufferCapacity = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val slow = launch {
            lossy.events.collect {
                log("медленный получил $it")
                delay(300)
            }
        }
        delay(50)
        SIX.forEach { lossy.publish(it) }
        delay(1500)
        slow.cancel()
    }
}

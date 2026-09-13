package loader.lesson14

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeoutOrNull
import loader.core.FakeJournal
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val SIX = listOf(
    DownloadEvent.Completed("a.csv", 400),
    DownloadEvent.Completed("b.csv", 700),
    DownloadEvent.Failed("broken.csv", "источник sigma недоступен"),
    DownloadEvent.Completed("c.csv", 1000),
    DownloadEvent.Failed("d.csv", "источник delta недоступен"),
    DownloadEvent.Completed("e.csv", 800),
)

class EventBusTest {

    @Test
    fun `оба подписчика получают все события`() = coTest {
        val bus = EventBus()
        val journal = FakeJournal()
        val stats = DownloadStats()
        backgroundScope.launchJournal(bus, journal)
        backgroundScope.launchStats(bus, stats)
        runCurrent()
        SIX.forEach { bus.publish(it) }
        // подписчики живут в backgroundScope: advanceUntilIdle их не ждёт, время двигаем явно
        advanceTimeBy(1_000)
        assertEquals(6, journal.records.size, "строк журнала")
        assertEquals(4 to 2, stats.completed to stats.failed, "статистика: готово и сбоев")
    }

    @Test
    fun `опоздавший подписчик получает последнее событие`() = coTest {
        val bus = EventBus(replay = 1)
        SIX.take(3).forEach { bus.publish(it) }
        assertEquals(SIX[2], bus.events.first())
    }

    @Test
    fun `без подписчиков и без replay событие теряется`() = coTest {
        val bus = EventBus(replay = 0)
        SIX.forEach { bus.publish(it) }
        assertNull(withTimeoutOrNull(1000) { bus.events.first() })
    }

    @Test
    fun `tryPublish при полном буфере возвращает false`() = coTest {
        val bus = EventBus(replay = 0, extraBufferCapacity = 2)
        backgroundScope.launch { bus.events.collect { awaitCancellation() } }
        runCurrent()
        val accepted = SIX.map { bus.tryPublish(it) }
        assertTrue(accepted.first(), "первое событие не принято")
        assertFalse(accepted.last(), "шестое событие принято при полном буфере")
    }

    @Test
    fun `publish при полном буфере ждёт, а не теряет`() = coTest {
        val bus = EventBus(replay = 0, extraBufferCapacity = 2)
        val gate = CompletableDeferred<Unit>()
        val received = mutableListOf<DownloadEvent>()
        backgroundScope.launch {
            bus.events.collect {
                gate.await()
                received += it
            }
        }
        runCurrent()
        val publisher = launch { SIX.forEach { bus.publish(it) } }
        runCurrent()
        assertFalse(publisher.isCompleted, "publish не ждал места в полном буфере")
        gate.complete(Unit)
        publisher.join()
        advanceUntilIdle()
        assertEquals(SIX, received)
    }
}

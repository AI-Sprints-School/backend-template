package loader.lesson15

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import loader.core.SourceCatalog
import loader.lesson13.StatefulLoader
import loader.lesson14.DownloadEvent
import loader.lesson14.EventBus
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Сбой сборщика в тесте: его и только его должен получить вызывающий. */
private class CollectorFailure : RuntimeException("сборщик упал")

private class SummaryFixture(test: TestScope) {
    val catalog = SourceCatalog(quiet = true)
    val loader = StatefulLoader(test.backgroundScope, catalog)
    val bus = EventBus(replay = 0)
    val lines = mutableListOf<String>()

    init {
        test.backgroundScope.launch(UnconfinedTestDispatcher(test.testScheduler)) {
            summary(loader, bus, test.backgroundScope).collect { lines += it }
        }
    }
}

class ServiceReportTest {

    @Test
    fun `сводка обновляется при смене состояния`() = coTest {
        val f = SummaryFixture(this)
        runCurrent()
        val before = f.lines.size
        f.loader.start(emptyList())
        runCurrent()
        assertTrue(f.lines.size > before, "после start новой строки сводки нет")
        assertTrue("RUNNING" in f.lines.last(), "последняя строка сводки: ${f.lines.lastOrNull()}")
    }

    @Test
    fun `сводка обновляется при новом событии`() = coTest {
        val f = SummaryFixture(this)
        runCurrent()
        val before = f.lines.size
        f.bus.publish(DownloadEvent.Completed("a.csv", 400))
        runCurrent()
        assertTrue(f.lines.size > before, "после события Completed новой строки сводки нет")
        assertTrue("готово 1" in f.lines.last(), "последняя строка сводки: ${f.lines.lastOrNull()}")
    }

    @Test
    fun `нестабильная лента повторяется дважды`() = coTest {
        val feed = FlakyFeed(2)
        val items = resilientFeed(feed).toList()
        assertEquals(3, feed.attempts, "попыток ленты")
        assertEquals(listOf(FeedItem.Item("новость 1"), FeedItem.Item("новость 2")), items)
    }

    @Test
    fun `после исчерпания повторов лента отдаёт Unavailable`() = coTest {
        val feed = FlakyFeed(5)
        val items = resilientFeed(feed).toList()
        assertEquals(FeedItem.Unavailable, items.lastOrNull())
        assertEquals(3, feed.attempts, "попыток ленты")
    }

    @Test
    fun `catch не перехватывает ошибку сборщика`() = coTest {
        assertFailsWith<CollectorFailure> {
            resilientFeed(FlakyFeed(0)).collect { throw CollectorFailure() }
        }
    }

    @Test
    fun `отмена сборщика снимает подписку`() = coTest {
        val source = CallbackEventSource()
        val job = launch { callbackEvents(source).collect() }
        runCurrent()
        assertEquals(1, source.listeners, "слушателей во время сбора")
        job.cancel()
        job.join()
        assertEquals(0, source.listeners, "слушателей после отмены сборщика")
    }
}

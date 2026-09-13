package loader.lesson12

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LogPipelineTest {

    private val lines = serviceLogLines()

    @Test
    fun `битые строки пропускаются`() = coTest {
        assertEquals(204, lines.size, "строк в data/service.log")
        assertEquals(200, parse(lines.asFlow()).toList().size)
    }

    @Test
    fun `фильтр оставляет WARN и ERROR`() = coTest {
        val events = warningsAndErrors(parse(lines.asFlow())).toList()
        assertEquals(50, events.size)
        assertEquals(setOf(Level.WARN, Level.ERROR), events.map { it.level }.toSet())
    }

    @Test
    fun `подсчёт по типам совпадает с журналом`() = coTest {
        val expected = mapOf(
            EventType.DOWNLOAD_STARTED to 60,
            EventType.DOWNLOAD_DONE to 55,
            EventType.SERVICE_EVENT to 35,
            EventType.RETRY to 25,
            EventType.TIMEOUT to 10,
            EventType.DOWNLOAD_FAILED to 15,
        )
        assertEquals(expected.toSortedMap(), countByType(parse(lines.asFlow())).toSortedMap())
    }

    @Test
    fun `обогащение идёт не больше чем в четыре потока`() = coTest {
        val directory = SourceDirectory()
        enrich(warningsAndErrors(parse(lines.asFlow())), directory).toList()
        assertEquals(4, directory.maxInFlight)
    }

    @Test
    fun `обогащение отдаёт все события`() = coTest {
        val enriched = enrich(warningsAndErrors(parse(lines.asFlow())), SourceDirectory()).toList()
        assertEquals(50, enriched.size)
        assertEquals(enriched.size, enriched.count { it.sourceDescription == "источник ${it.event.sourceId}" }, "описаний источника")
    }

    @Test
    fun `обогащение параллельно, а не подряд`() = coTest {
        enrich(warningsAndErrors(parse(lines.asFlow())), SourceDirectory()).toList()
        assertEquals(1300L, currentTime)
    }
}

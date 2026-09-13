package loader.lesson02

import kotlinx.coroutines.test.currentTime
import loader.core.FakeJournal
import loader.core.SourceCatalog
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BatchLoadTest {

    private val ids = listOf("alpha", "beta", "gamma", "epsilon", "omega")

    @Test
    fun `пять источников опрашиваются параллельно`() = coTest {
        pollSources(ids, SourceCatalog(), FakeJournal())
        assertEquals(3000L, currentTime)
    }

    @Test
    fun `ответы в порядке списка источников`() = coTest {
        val reports = pollSources(ids, SourceCatalog(), FakeJournal())
        assertEquals(ids, reports.map { it.sourceId })
    }

    @Test
    fun `запись в журнал завершена до возврата`() = coTest {
        val journal = FakeJournal()
        pollSources(ids, SourceCatalog(), journal)
        assertEquals(listOf("опрос: 5 источников"), journal.records)
    }

    @Test
    fun `после возврата загрузок нет`() = coTest {
        val catalog = SourceCatalog()
        pollSources(ids, catalog, FakeJournal())
        assertEquals(List(5) { 0 }, ids.map { catalog.source(it).stats.inFlight })
    }

    @Test
    fun `каждый источник опрошен один раз`() = coTest {
        val catalog = SourceCatalog()
        pollSources(ids, catalog, FakeJournal())
        assertEquals(List(5) { 1 }, ids.map { catalog.source(it).stats.attempts })
    }

    @Test
    fun `другой seed`() = coTest {
        val catalog = SourceCatalog(seed = 7)
        pollSources(ids, catalog, FakeJournal())
        assertEquals(ids.maxOf { catalog.source(it).latencyMs }, currentTime)
    }
}

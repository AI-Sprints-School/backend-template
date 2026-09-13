package loader.lesson02

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import loader.core.FakeJournal
import loader.core.Payload
import loader.core.SourceCatalog

// Урок 2. Параллельный опрос источников с записью в журнал.

/** Строка сводки: источник и загруженный файл. */
data class SourceReport(val sourceId: String, val payload: Payload)

/**
 * Внутри одного coroutineScope: запись «опрос: N источников» через launch и journal.write,
 * по async на источник — catalog.source(id).download(catalog.firstFileOf(id)), затем один awaitAll.
 * Ответы — в порядке sourceIds.
 */
suspend fun pollSources(sourceIds: List<String>, catalog: SourceCatalog, journal: FakeJournal): List<SourceReport> {
    TODO("Урок 02: опросить источники параллельно через async и записать опрос в журнал через launch")
}

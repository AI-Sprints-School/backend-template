package loader.lesson12

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.mapNotNull

// Урок 12. Конвейер журнала сервиса на операторах Flow.
// Строка журнала: «2026-09-01T10:00:00.400 INFO  DOWNLOAD_DONE alpha a.csv 400» — время, уровень, тип, источник, файл, мс.

enum class Level { INFO, WARN, ERROR }

enum class EventType { DOWNLOAD_STARTED, DOWNLOAD_DONE, SERVICE_EVENT, RETRY, TIMEOUT, DOWNLOAD_FAILED }

data class LogEvent(val level: Level, val type: EventType, val sourceId: String, val file: String, val ms: Long)

data class EnrichedEvent(val event: LogEvent, val sourceDescription: String)

/** Строки журнала → события; битые строки пропускаются (mapNotNull). */
fun parse(lines: Flow<String>): Flow<LogEvent> {
    TODO("Урок 12: разобрать строки журнала в события и пропустить битые")
}

/** Только WARN и ERROR (filter). */
fun warningsAndErrors(events: Flow<LogEvent>): Flow<LogEvent> {
    TODO("Урок 12: оставить события WARN и ERROR")
}

/** Число событий каждого типа — терминальным оператором fold, без toList. */
suspend fun countByType(events: Flow<LogEvent>): Map<EventType, Int> {
    TODO("Урок 12: посчитать события по типам через fold")
}

/** К каждому событию — описание источника directory.describe; не больше concurrency запросов одновременно (flatMapMerge). */
fun enrich(events: Flow<LogEvent>, directory: SourceDirectory, concurrency: Int = 4): Flow<EnrichedEvent> {
    TODO("Урок 12: обогатить события параллельно через flatMapMerge")
}

package loader.lesson12

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 12 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=12

fun main() = lessonMain {
    runBlocking {
        val lines = serviceLogLines()
        Log.plain("журнал data/service.log: ${lines.size} строк")
        Log.plain("событий после parse: ${parse(lines.asFlow()).count()}")
        Log.plain("WARN и ERROR: ${warningsAndErrors(parse(lines.asFlow())).count()}")
        Log.plain("по типам:")
        countByType(parse(lines.asFlow())).toSortedMap().forEach { (type, n) -> Log.plain("  $type — $n") }

        val directory = SourceDirectory()
        var enriched = 0
        val millis = measureTimeMillis {
            enriched = enrich(warningsAndErrors(parse(lines.asFlow())), directory).count()
        }
        Log.plain("обогащено $enriched за ${seconds(millis)} с, одновременных запросов: ${directory.maxInFlight}")
    }
}

package loader.lesson10

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import loader.core.Payload
import loader.core.RemoteFile
import loader.core.SourceCatalog

// Урок 10. Изоляция сбоев и повторы.

data class BatchOutcome(val succeeded: List<Payload>, val failed: Map<String, Throwable>)

/** До times попыток; пауза backoffMs * номер попытки; CancellationException — сразу наружу, без повтора. */
suspend fun <T> retrying(times: Int, backoffMs: Long, block: suspend (attempt: Int) -> T): T {
    TODO("Урок 10: повторять блок с растущей паузой, не повторяя отмену")
}

/** supervisorScope; async на файл; retrying(retries + 1, backoffMs). Сбой одного файла — в failed, соседи продолжают. */
suspend fun downloadIsolated(
    files: List<RemoteFile>,
    catalog: SourceCatalog,
    retries: Int = 2,
    backoffMs: Long = 200,
): BatchOutcome {
    TODO("Урок 10: загрузить пакет в supervisorScope с повторами и собрать итог")
}

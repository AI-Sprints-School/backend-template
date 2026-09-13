package loader.lesson09

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import loader.core.FakeDisk
import loader.core.Payload
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.StoredFile

// Урок 9. Остановка без мусора: отмена в вычислении, уборка после отмены, таймаут.

/** Контрольная сумма циклом на iterations шагов; проверка отмены — ensureActive() на каждой 10 000-й итерации. */
suspend fun checksumLoop(payload: Payload, iterations: Int): Long {
    TODO("Урок 09: считать сумму циклом и проверять отмену через ensureActive")
}

/** createTemp → download → writeBlocking; временный файл удаляется в finally через withContext(NonCancellable) { disk.deleteTemp(temp) }. */
suspend fun downloadToTemp(file: RemoteFile, catalog: SourceCatalog, disk: FakeDisk): StoredFile {
    TODO("Урок 09: загрузить во временный файл и убрать его даже при отмене")
}

/** Загрузка не дольше timeoutMs, иначе null — через withTimeoutOrNull. */
suspend fun downloadWithTimeout(file: RemoteFile, catalog: SourceCatalog, timeoutMs: Long = 2000): Payload? {
    TODO("Урок 09: оборвать загрузку по таймауту и вернуть null")
}

package loader.lesson11

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import loader.core.FakeDisk
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.log

// Урок 11. Холодные потоки: прогресс загрузки и чтение с диска.

data class Progress(val file: String, val percent: Int)

/**
 * flow: log("поток прогресса: старт <имя файла>"), затем chunks частей через source.downloadChunk(file, index, chunks),
 * по Progress на часть.
 */
fun progressFlow(file: RemoteFile, catalog: SourceCatalog, chunks: Int = 5): Flow<Progress> {
    TODO("Урок 11: построить холодный поток прогресса загрузки")
}

/** flow со строками disk.readLinesBlocking(name); чтение — на диспетчере io через flowOn. */
fun storedLines(disk: FakeDisk, name: String, io: CoroutineDispatcher): Flow<String> {
    TODO("Урок 11: читать строки с диска в потоке на диспетчере io")
}

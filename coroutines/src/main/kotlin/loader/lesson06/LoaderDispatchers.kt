package loader.lesson06

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import loader.core.Checksum
import loader.core.FakeDisk
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.StoredFile
import loader.core.log

// Урок 6. Диспетчеры: загрузка с ограничением, запись на IO, контрольная сумма на пуле вычислений.

class LoaderDispatchers(val io: CoroutineDispatcher = Dispatchers.IO, val cpu: CoroutineDispatcher = Dispatchers.Default)

/**
 * По файлу — async(CoroutineName("download-<имя файла>")); загрузка — под Semaphore(maxConcurrent).withPermit;
 * запись — withContext(dispatchers.io) { disk.writeBlocking }; контрольная сумма —
 * withContext(dispatchers.cpu) { Checksum.compute }; log после каждого шага. Результаты — в порядке файлов.
 */
suspend fun downloadAndStore(
    files: List<RemoteFile>,
    catalog: SourceCatalog,
    disk: FakeDisk,
    dispatchers: LoaderDispatchers,
    maxConcurrent: Int = 3,
): List<StoredFile> {
    TODO("Урок 06: загрузить, записать и посчитать контрольную сумму на своих диспетчерах")
}

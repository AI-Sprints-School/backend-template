package loader.lesson04

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.log

// Готово в шаблоне, не правится: тот же сервис на GlobalScope — для сравнения в сценарии.
// Так делать не надо: stop() только ставит флаг, а задачи без владельца живут дальше.

@OptIn(DelicateCoroutinesApi::class)
class GlobalLoader(private val catalog: SourceCatalog, private val intervalMs: Long = 1000) {
    @Volatile private var running = false

    fun start(files: List<RemoteFile>) {
        running = true
        files.forEach { file ->
            GlobalScope.launch {
                while (true) {
                    catalog.source(file.sourceId).download(file)
                    delay(intervalMs)
                }
            }
        }
    }

    fun stop() {
        log("stop")
        running = false
    }

    val isRunning: Boolean get() = running
}

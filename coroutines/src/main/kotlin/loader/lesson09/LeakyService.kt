package loader.lesson09

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import loader.core.SourceCatalog
import loader.core.log

// Сервис урока 9. Одна из его задач переживает stop() — найдите её по логу и дампу корутин и исправьте здесь.

class LeakyService(private val catalog: SourceCatalog) {
    private val scope = CoroutineScope(Dispatchers.Default + Job() + CoroutineName("leaky-service"))

    fun start() {
        scope.launch(CoroutineName("leaky-download")) {
            while (true) {
                catalog.source("alpha").download(catalog.file("a.csv"))
                delay(300)
            }
        }
        CoroutineScope(Dispatchers.Default).launch(CoroutineName("heartbeat")) {
            while (true) {
                log("heartbeat: сервис жив")
                delay(500)
            }
        }
    }

    fun stop() {
        log("stop")
        scope.cancel()
    }
}

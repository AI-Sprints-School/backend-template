package loader.lesson08

import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.lesson08.fragments.fragmentAsync
import loader.lesson08.fragments.fragmentLaunch
import loader.lesson08.fragments.fragmentNested
import java.io.IOException

// Сценарий урока 8 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=08

fun main() = lessonMain {
    runBlocking {
        val handler = rootHandler()
        val fragments = listOf(::fragmentLaunch, ::fragmentAsync, ::fragmentNested)
        fragments.forEachIndexed { index, fragment ->
            Log.plain("фрагмент ${index + 1}")
            try {
                fragment(handler)
            } catch (e: IOException) {
                Log.plain("фрагмент ${index + 1}: исключение вылетело в main — ${e.message}")
            }
        }

        Log.plain("loadOrNull")
        val catalog = SourceCatalog()
        Log.plain("broken.csv → ${loadOrNull(catalog.file("broken.csv"), catalog)?.file?.name}")
        Log.plain("a.csv → ${loadOrNull(catalog.file("a.csv"), catalog)?.file?.name}")
    }
}

package loader.lesson15

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.scan
import loader.core.SourceUnavailableException
import loader.lesson13.StatefulLoader
import loader.lesson14.DownloadEvent
import loader.lesson14.EventBus

// Урок 15. Сводка сервиса: сборка уроков 13 и 14, устойчивая лента, события из колбэков.
// Импортирует ваш StatefulLoader (урок 13) и EventBus (урок 14) — они должны быть влиты в main.

/** combine(loader.state, статистика из bus.events через scan) → «<статус> · готово N · сбоев M». */
fun summary(loader: StatefulLoader, bus: EventBus, scope: CoroutineScope): Flow<String> {
    TODO("Урок 15: собрать сводку из состояния загрузчика и событий шины")
}

/** Элементы ленты: retry(2) только для SourceUnavailableException, после исчерпания — catch и FeedItem.Unavailable. Отмену не ловит. */
fun resilientFeed(feed: FlakyFeed): Flow<FeedItem> {
    TODO("Урок 15: повторить ленту при сбое источника и отдать Unavailable после исчерпания повторов")
}

/** callbackFlow: register слушателя, awaitClose { unregister }. */
fun callbackEvents(source: CallbackEventSource): Flow<String> {
    TODO("Урок 15: превратить колбэки источника событий в поток")
}

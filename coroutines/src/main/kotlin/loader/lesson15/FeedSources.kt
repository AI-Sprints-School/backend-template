package loader.lesson15

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import loader.core.SourceUnavailableException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

// Готово в шаблоне, не правится.

/** Элемент устойчивой ленты. */
sealed interface FeedItem {
    data class Item(val text: String) : FeedItem
    data object Unavailable : FeedItem
}

/** Лента, которая первые [failuresBeforeSuccess] попыток падает с SourceUnavailableException до первого элемента. */
class FlakyFeed(private val failuresBeforeSuccess: Int) {
    private val attemptCount = AtomicInteger()

    fun items(): Flow<String> = flow {
        val attempt = attemptCount.incrementAndGet()
        if (attempt <= failuresBeforeSuccess) throw SourceUnavailableException("feed", attempt)
        emit("новость 1")
        emit("новость 2")
    }

    /** Сколько раз лента начиналась. */
    val attempts: Int get() = attemptCount.get()
}

/** Источник событий со старым API слушателей. */
class CallbackEventSource {
    private val registered = CopyOnWriteArrayList<(String) -> Unit>()

    fun register(listener: (String) -> Unit) {
        registered += listener
    }

    fun unregister(listener: (String) -> Unit) {
        registered -= listener
    }

    /** Отправляет событие всем слушателям. */
    fun fire(event: String) = registered.forEach { it(event) }

    val listeners: Int get() = registered.size
}

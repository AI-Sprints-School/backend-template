package lesson28;

import java.util.List;

/**
 * Урок 28. Java-код, который вызывает Kotlin-сервис каталога. Заготовка:
 * напишите тела так, чтобы нигде не было `.Companion`, и расставьте
 * аннотации в CatalogService.kt.
 */
public final class CatalogClient {

    private CatalogClient() {
    }

    /** Поиск по демо-каталогу с лимитом по умолчанию (вызов search без второго аргумента). */
    public static List<String> firstMatches(String query) {
        // TODO урок 28: CatalogService.withDemoData().search(query)
        throw new UnsupportedOperationException("Урок 28: firstMatches не написан");
    }

    /** Текст ошибки пустого запроса — поле EMPTY_QUERY_MESSAGE, без геттера. */
    public static String emptyQueryMessage() {
        // TODO урок 28: CatalogService.EMPTY_QUERY_MESSAGE
        throw new UnsupportedOperationException("Урок 28: emptyQueryMessage не написан");
    }

    /** Заголовок по индексу или "нет": platform type — null проверяется явно. */
    public static String titleOrDash(CatalogService service, int index) {
        // TODO урок 28: titleOrNull может вернуть null
        throw new UnsupportedOperationException("Урок 28: titleOrDash не написан");
    }
}

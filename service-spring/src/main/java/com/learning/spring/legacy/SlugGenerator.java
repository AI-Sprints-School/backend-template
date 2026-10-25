package com.learning.spring.legacy;

import java.util.Locale;

/**
 * Генератор слага курса: «Kotlin для Android» -> «kotlin-android».
 *
 * Выдан как есть, из «соседнего модуля команды». Урок 19: вызвать из Kotlin,
 * найти, где платформенный тип роняет сервис, перенести в Kotlin, Java-файл удалить.
 */
public final class SlugGenerator {

    private SlugGenerator() {
    }

    /**
     * Оставляет в названии латинские буквы и цифры, остальное считает разделителем.
     * Слова склеиваются через «-», слаг не длиннее 60 символов.
     *
     * @return слаг; если в названии нет ни одной латинской буквы или цифры — {@code null}
     */
    public static String slugify(String title) {
        String lower = title.trim().toLowerCase(Locale.ROOT);
        String slug = lower.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            return null;
        }
        return slug.length() > 60 ? slug.substring(0, 60).replaceAll("-+$", "") : slug;
    }
}

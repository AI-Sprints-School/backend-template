package lesson25;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Урок 25, вторая часть. Счётчики просмотров по урокам. Коллекция
 * потокобезопасная, а подсчёт — нет: «прочитать, прибавить, записать»
 * теряет обновления. Перепишите hit на одну атомарную операцию merge.
 */
public final class ViewTally {

    private final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

    public void hit(String lessonId) {
        Integer current = counts.get(lessonId);
        counts.put(lessonId, current == null ? 1 : current + 1);
    }

    public int get(String lessonId) {
        return counts.getOrDefault(lessonId, 0);
    }
}

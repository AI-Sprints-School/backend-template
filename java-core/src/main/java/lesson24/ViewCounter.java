package lesson24;

/**
 * Урок 24. Счётчик просмотров урока. При вызовах из нескольких потоков
 * теряет обновления — покажите это тестом с пулом (ViewCounterRaceTest),
 * потом исправьте двумя способами: атомиком и synchronized.
 */
public class ViewCounter {

    private int views;

    public void increment() {
        views++;
    }

    public int get() {
        return views;
    }
}

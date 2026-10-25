package lesson25;

/**
 * Урок 25. Перевод баллов между студентами. Две встречные передачи
 * (a→b и b→a) в разных потоках зависают — покажите это тестом с таймаутом
 * (PointsTransferTest), потом исправьте порядком блокировок.
 */
public final class PointsTransfer {

    private PointsTransfer() {
    }

    public static void transfer(Student from, Student to, int points) {
        synchronized (from) {
            pause();
            synchronized (to) {
                from.setPoints(from.getPoints() - points);
                to.setPoints(to.getPoints() + points);
            }
        }
    }

    /** Короткая пауза «работы» между блокировками: делает зависание воспроизводимым. */
    private static void pause() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

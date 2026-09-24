package career.concurrency;

import java.util.concurrent.CountDownLatch;

/**
 * Прогон, подтверждающий взаимную блокировку между transfer() и
 * recordStats(): один поток крутит transfer, другой — recordStats,
 * оба непрерывно. Процесс не завершается в разумное время — это и есть
 * deadlock (в реальном коде его ищут по логам JVM thread dump, здесь —
 * по таймауту join).
 */
public class DeadlockDemo {
    public static void main(String[] args) throws InterruptedException {
        AccountCache cache = new AccountCache();
        cache.put("a", 100);
        cache.put("b", 0);

        CountDownLatch ready = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            ready.countDown();
            for (int i = 0; i < 200_000; i++) {
                cache.transfer("a", "b", 1);
            }
        }, "transfer-thread");

        Thread t2 = new Thread(() -> {
            ready.countDown();
            for (int i = 0; i < 200_000; i++) {
                cache.recordStats("a");
            }
        }, "recordStats-thread");

        t1.start();
        t2.start();

        long timeoutMs = 5000;
        t1.join(timeoutMs);
        t2.join(timeoutMs);

        boolean stuck = t1.isAlive() || t2.isAlive();
        System.out.println("t1 (transfer) жив по истечении " + timeoutMs + " мс: " + t1.isAlive());
        System.out.println("t2 (recordStats) жив по истечении " + timeoutMs + " мс: " + t2.isAlive());
        if (stuck) {
            System.out.println("deadlock воспроизведён: обратный порядок захвата balancesLock/statsLock "
                    + "в transfer() и statsLock/balancesLock в recordStats() застопорил оба потока");
        } else {
            System.out.println("в этом запуске потоки завершились — перезапустите несколько раз, "
                    + "порядок захвата остаётся обратным и рано или поздно застревает");
        }
        System.exit(0);
    }
}

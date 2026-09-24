package career.concurrency;

/**
 * Прогон, подтверждающий гонку в AccountCache.get(). 8 потоков по 10000
 * обращений каждый должны дать hits == 80000; несинхронизированный
 * инкремент даёт меньше.
 */
public class RaceDemo {
    public static void main(String[] args) throws InterruptedException {
        AccountCache cache = new AccountCache();
        cache.put("acc-1", 100);

        int threads = 8;
        int callsPerThread = 10_000;
        Thread[] pool = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            pool[i] = new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    cache.get("acc-1");
                }
            });
        }
        for (Thread t : pool) t.start();
        for (Thread t : pool) t.join();

        int expected = threads * callsPerThread;
        System.out.println("ожидали hits = " + expected + ", получили hits = " + cache.getHits());
        if (cache.getHits() != expected) {
            System.out.println("гонка воспроизведена: инкременты потеряны");
        } else {
            System.out.println("в этом запуске гонка не проявилась — перезапустите: hits++ не атомарна");
        }
    }
}

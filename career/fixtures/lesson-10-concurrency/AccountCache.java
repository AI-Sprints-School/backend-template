package career.concurrency;

import java.util.HashMap;
import java.util.Map;

/**
 * Урок 10 карьерного курса. Ревью многопоточного фрагмента: гонка и
 * взаимная блокировка. Учебный кеш баланса счетов.
 */
public class AccountCache {

    private final Map<String, Integer> balances = new HashMap<>();
    private int hits = 0; // счётчик обращений к кешу

    private final Object balancesLock = new Object();
    private final Object statsLock = new Object();

    public void put(String accountId, int balance) {
        synchronized (balancesLock) {
            balances.put(accountId, balance);
        }
    }

    // Дефект 1 (гонка): hits++ не атомарна и не синхронизирована —
    // чтение, инкремент и запись трёх потоков могут перекрыться.
    public Integer get(String accountId) {
        hits++;
        synchronized (balancesLock) {
            return balances.get(accountId);
        }
    }

    // Дефект 2 (deadlock): transfer берёт balancesLock, затем statsLock;
    // recordStats берёт statsLock, затем balancesLock — обратный порядок.
    // Два потока, вызванные с разных концов, блокируют друг друга навсегда.
    public void transfer(String fromId, String toId, int amount) {
        synchronized (balancesLock) {
            int from = balances.getOrDefault(fromId, 0);
            int to = balances.getOrDefault(toId, 0);
            balances.put(fromId, from - amount);
            balances.put(toId, to + amount);
            synchronized (statsLock) {
                hits++;
            }
        }
    }

    public void recordStats(String accountId) {
        synchronized (statsLock) {
            hits++;
            synchronized (balancesLock) {
                balances.computeIfPresent(accountId, (id, v) -> v);
            }
        }
    }

    public int getHits() {
        return hits;
    }
}

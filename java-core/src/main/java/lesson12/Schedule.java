package lesson12;

import java.time.LocalDate;
import java.util.List;

/**
 * Урок 12. Расписание занятий курса. Класс выдан как «неизменяемый», но таким
 * не является — покажите это тестом (ScheduleImmutabilityTest), потом исправьте.
 */
public final class Schedule {

    private final List<LocalDate> sessions;

    public Schedule(List<LocalDate> sessions) {
        this.sessions = sessions;
    }

    public List<LocalDate> getSessions() {
        return sessions;
    }

    public int count() {
        return sessions.size();
    }
}

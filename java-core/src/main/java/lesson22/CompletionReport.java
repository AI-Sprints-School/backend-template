package lesson22;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Урок 22. Отчёт по каталогу на Collectors. Методы 1 и 2 — заготовки,
 * метод 3 выдан с дефектом: на фикстуре падает с IllegalStateException.
 */
public final class CompletionReport {

    private CompletionReport() {
    }

    /**
     * 1. Доля пройденных уроков по курсам: сумма пройденных / сумма всех уроков
     * записей курса (groupingBy). Ключ — slug курса.
     */
    public static Map<String, Double> completionShareByCourse(List<Enrollment> enrollments) {
        // TODO урок 22: groupingBy
        throw new UnsupportedOperationException("Урок 22: completionShareByCourse не написан");
    }

    /**
     * 2. Сколько записей дошло до порога, а сколько нет (partitioningBy):
     * true — доля пройденного не ниже порога (граница включается), false — ниже.
     */
    public static Map<Boolean, Long> partitionByThreshold(List<Enrollment> enrollments, double threshold) {
        // TODO урок 22: partitioningBy
        throw new UnsupportedOperationException("Урок 22: partitionByThreshold не написан");
    }

    /**
     * 3. Названия курсов по slug. При дубле slug остаётся первое название.
     * Дефект: toMap без функции слияния.
     */
    public static Map<String, String> titlesBySlug(List<Course> courses) {
        return courses.stream().collect(Collectors.toMap(Course::slug, Course::title));
    }
}

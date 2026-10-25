package com.learning.spring.legacy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Отчёт «доля пройденных уроков по курсам» на Stream API.
 *
 * Выдан как есть. Урок 20: перенести в Kotlin на операциях коллекций, без
 * {@code stream()} и {@code Optional}; Java-файл удалить.
 */
public class ProgressReportService {

    /**
     * Строки отчёта по курсам: сначала с большей долей, при равенстве — по названию.
     * Курс без отметок в отчёт не попадает.
     */
    public List<CourseProgress> report(List<ProgressEntry> entries) {
        Map<String, List<ProgressEntry>> byCourse = entries.stream()
                .collect(Collectors.groupingBy(ProgressEntry::courseId));

        return byCourse.values().stream()
                .map(group -> {
                    int total = group.size();
                    int completed = (int) group.stream().filter(ProgressEntry::completed).count();
                    ProgressEntry first = group.get(0);
                    return new CourseProgress(
                            first.courseId(), first.courseTitle(), total, completed, completed * 100 / total);
                })
                .sorted(Comparator.comparingInt(CourseProgress::percent).reversed()
                        .thenComparing(CourseProgress::courseTitle))
                .toList();
    }

    /** Курс с наибольшей долей; пустой {@link Optional}, если отметок нет. */
    public Optional<CourseProgress> topCourse(List<ProgressEntry> entries) {
        return report(entries).stream().findFirst();
    }

    /** Названия курсов, пройденных целиком, по алфавиту. */
    public List<String> completedCourseTitles(List<ProgressEntry> entries) {
        return report(entries).stream()
                .filter(line -> line.percent() == 100)
                .map(CourseProgress::courseTitle)
                .sorted()
                .toList();
    }
}

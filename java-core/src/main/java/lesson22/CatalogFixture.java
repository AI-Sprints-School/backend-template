package lesson22;

import java.util.List;

/**
 * Фикстура каталога для отчёта главы 6. Данные фиксированы, тесты урока
 * сверяются с ними. Готовая часть шаблона — править не нужно.
 */
public final class CatalogFixture {

    private CatalogFixture() {
    }

    /** Курсы каталога; у slug "java-core" есть дубль — старая запись архива. */
    public static List<Course> courses() {
        return List.of(
                new Course("java-core", "Java Core", "backend"),
                new Course("spring-boot", "Spring Boot", "backend"),
                new Course("kotlin-coroutines", "Корутины", "backend"),
                new Course("java-core", "Java Core (архив 2025)", "archive"));
    }

    /** Записи студентов на курсы: пройдено уроков из всего. */
    public static List<Enrollment> enrollments() {
        return List.of(
                new Enrollment("s1", "java-core", 8, 10),
                new Enrollment("s2", "java-core", 4, 10),
                new Enrollment("s3", "java-core", 10, 10),
                new Enrollment("s1", "spring-boot", 3, 12),
                new Enrollment("s2", "spring-boot", 6, 12),
                new Enrollment("s3", "kotlin-coroutines", 5, 5));
    }
}

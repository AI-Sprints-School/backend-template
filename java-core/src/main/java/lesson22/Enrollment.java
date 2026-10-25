package lesson22;

/** Запись студента на курс с прогрессом. Готовая часть шаблона — править не нужно. */
public record Enrollment(String studentId, String courseSlug, int completedLessons, int totalLessons) {

    public double ratio() {
        return (double) completedLessons / totalLessons;
    }
}

package lesson15;

/**
 * Урок 15. Заголовок урока из первой строки файла. Код выдан с дефектами —
 * найдите их своим тестом (LessonLoaderTest), потом исправьте через
 * try-with-resources.
 */
public final class LessonLoader {

    private LessonLoader() {
    }

    public static String title(LessonFile file) {
        try {
            return file.readFirstLine();
        } catch (Exception e) {
            return "";
        }
    }
}

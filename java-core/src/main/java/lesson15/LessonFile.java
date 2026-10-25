package lesson15;

import java.io.IOException;

/**
 * Учебный ресурс: «файл» урока в памяти, который нужно закрывать.
 * Готовая часть шаблона — править не нужно.
 */
public final class LessonFile implements AutoCloseable {

    private final String content;
    private final boolean failOnRead;
    private boolean closed;

    private LessonFile(String content, boolean failOnRead) {
        this.content = content;
        this.failOnRead = failOnRead;
    }

    /** Файл с содержимым; строки разделены переводом строки. */
    public static LessonFile of(String content) {
        return new LessonFile(content, false);
    }

    /** Файл, чтение которого падает с IOException. */
    public static LessonFile failing() {
        return new LessonFile("", true);
    }

    public String readFirstLine() throws IOException {
        if (closed) throw new IOException("файл закрыт");
        if (failOnRead) throw new IOException("сбой чтения");
        int end = content.indexOf('\n');
        return end < 0 ? content : content.substring(0, end);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}

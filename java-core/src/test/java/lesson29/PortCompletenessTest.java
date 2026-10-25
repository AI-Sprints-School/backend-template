package lesson29;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Итог курса: таблица переноса и карта вопросов собеседования заполнены. */
class PortCompletenessTest {

    @Test
    void readmeHasPortingTable() throws IOException {
        List<List<String>> rows = tableRows(Path.of("README.md"), "Конструкция Kotlin");
        assertTrue(rows.size() >= 6, "в README.md таблица «Kotlin → Java»: нужно не меньше 6 строк, сейчас " + rows.size());
        rows.forEach(r -> assertEquals(3, r.size()));
        rows.forEach(r -> r.forEach(cell -> assertFalse(cell.isBlank(), "пустая ячейка в таблице переноса")));
    }

    @Test
    void interviewMapIsComplete() throws IOException {
        List<List<String>> rows = tableRows(Path.of("reports/java-interview.md"), "Урок");
        assertEquals(29, rows.size(), "в карте должна быть строка на каждый из 29 уроков");
        for (List<String> row : rows) {
            assertEquals(4, row.size());
            assertFalse(row.get(2).isBlank(), "урок " + row.get(0) + ": нет ответа");
            assertFalse(row.get(3).isBlank(), "урок " + row.get(0) + ": нет доказательства");
        }
    }

    /** Строки таблицы под заголовком, начинающимся с {@code headerStart}; ячейки без пробелов по краям. */
    private static List<List<String>> tableRows(Path file, String headerStart) throws IOException {
        List<String> lines = Files.readAllLines(file);
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("| " + headerStart)) {
                header = i;
                break;
            }
        }
        assertTrue(header >= 0, file + ": не найдена таблица с заголовком «" + headerStart + "…»");
        return lines.stream().skip(header + 2).takeWhile(l -> l.startsWith("|"))
                .map(l -> List.of(l.substring(1, l.length() - 1).split("\\|", -1)).stream().map(String::strip).toList())
                .toList();
    }
}

package lesson29;

import java.util.List;
import java.util.Map;

/**
 * Урок 29. Перенесите lesson29.original.CertificateService на Java 25.
 * Сообщения об отказе — те же, что в оригинале. issueAll обходит оценки
 * в порядке итерации переданной карты. Конструктор без проходной оценки
 * использует 60 (аргумент по умолчанию Kotlin → перегрузка).
 */
public final class CertificateService {

    public CertificateService(Map<String, Student> students, int passingGrade) {
        throw new UnsupportedOperationException("Урок 29: конструктор CertificateService не написан");
    }

    public CertificateService(Map<String, Student> students) {
        throw new UnsupportedOperationException("Урок 29: конструктор без проходной оценки не написан");
    }

    public IssueResult issue(String studentId, String courseSlug, int grade) {
        // TODO урок 29: Rejected("Студент не найден: …") / Rejected("Оценка ниже проходной: …") / Issued
        throw new UnsupportedOperationException("Урок 29: issue не написан");
    }

    public List<IssueResult> issueAll(Map<String, Integer> grades, String courseSlug) {
        throw new UnsupportedOperationException("Урок 29: issueAll не написан");
    }

    public List<String> emailsOfIssued(List<IssueResult> results) {
        throw new UnsupportedOperationException("Урок 29: emailsOfIssued не написан");
    }
}

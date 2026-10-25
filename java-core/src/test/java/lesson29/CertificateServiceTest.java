package lesson29;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CertificateServiceTest {

    private static Map<String, Student> students() {
        return Map.of(
                "s1", new Student("s1", "Анна", "anna@example.com"),
                "s2", new Student("s2", "Борис", null),
                "s3", new Student("s3", "Вера", "vera@example.com"));
    }

    @Test
    void issuesCertificateAtOrAbovePassingGrade() {
        CertificateService service = new CertificateService(students());
        IssueResult result = service.issue("s1", "java-core", 60);
        assertEquals(new IssueResult.Issued(new Certificate("s1", "java-core", 60)), result);
    }

    @Test
    void rejectsGradeBelowPassing() {
        CertificateService service = new CertificateService(students());
        assertEquals(new IssueResult.Rejected("Оценка ниже проходной: 59"), service.issue("s1", "java-core", 59));
    }

    @Test
    void customPassingGradeIsRespected() {
        CertificateService strict = new CertificateService(students(), 80);
        assertInstanceOf(IssueResult.Rejected.class, strict.issue("s1", "java-core", 79));
        assertInstanceOf(IssueResult.Issued.class, strict.issue("s1", "java-core", 80));
    }

    @Test
    void rejectsUnknownStudent() {
        CertificateService service = new CertificateService(students());
        assertEquals(new IssueResult.Rejected("Студент не найден: x9"), service.issue("x9", "java-core", 90));
    }

    @Test
    void issueAllKeepsOrderOfGrades() {
        CertificateService service = new CertificateService(students());
        Map<String, Integer> grades = new LinkedHashMap<>();
        grades.put("s3", 95);
        grades.put("s1", 40);
        grades.put("s2", 70);
        List<IssueResult> results = service.issueAll(grades, "java-core");
        assertEquals(3, results.size());
        assertEquals(new IssueResult.Issued(new Certificate("s3", "java-core", 95)), results.get(0));
        assertEquals(new IssueResult.Rejected("Оценка ниже проходной: 40"), results.get(1));
        assertEquals(new IssueResult.Issued(new Certificate("s2", "java-core", 70)), results.get(2));
    }

    @Test
    void emailsOfIssuedSkipsRejectedAndNullEmailsAndSorts() {
        CertificateService service = new CertificateService(students());
        Map<String, Integer> grades = new LinkedHashMap<>();
        grades.put("s3", 95);
        grades.put("s1", 40);
        grades.put("s2", 70);
        grades.put("s9", 99);
        List<IssueResult> results = service.issueAll(grades, "java-core");
        assertEquals(List.of("vera@example.com"), service.emailsOfIssued(results));
        grades.put("s1", 100);
        assertEquals(List.of("anna@example.com", "vera@example.com"),
                service.emailsOfIssued(service.issueAll(grades, "java-core")));
    }
}

package lesson29.original

/**
 * Урок 29. Kotlin-модуль «выдача сертификата» — выдан как есть. Перенесите
 * его в Java 25 в пакет lesson29 (src/main/java/lesson29): тесты
 * CertificateServiceTest проверяют Java-версию. Этот файл не правится.
 */
data class Student(val id: String, val name: String, val email: String?)

data class Certificate(val studentId: String, val courseSlug: String, val grade: Int)

sealed class IssueResult {
    data class Issued(val certificate: Certificate) : IssueResult()
    data class Rejected(val reason: String) : IssueResult()
}

class CertificateService(
    private val students: Map<String, Student>,
    private val passingGrade: Int = 60,
) {
    fun issue(studentId: String, courseSlug: String, grade: Int): IssueResult {
        val student = students[studentId] ?: return IssueResult.Rejected("Студент не найден: $studentId")
        if (grade < passingGrade) return IssueResult.Rejected("Оценка ниже проходной: $grade")
        return IssueResult.Issued(Certificate(student.id, courseSlug, grade))
    }

    fun issueAll(grades: Map<String, Int>, courseSlug: String): List<IssueResult> =
        grades.map { (studentId, grade) -> issue(studentId, courseSlug, grade) }

    fun emailsOfIssued(results: List<IssueResult>): List<String> =
        results.filterIsInstance<IssueResult.Issued>()
            .mapNotNull { students[it.certificate.studentId]?.email }
            .sorted()
}

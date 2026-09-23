package com.learning.validation

import com.learning.models.*
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult

/**
 * Правила проверки входящих запросов.
 *
 * Правила описываются здесь, а исполняет их плагин Ktor `RequestValidation`
 * (см. [installRequestValidation]): он вызывает валидатор при `call.receive<T>()`
 * и бросает `RequestValidationException`, если запрос не прошёл. Каждое
 * нарушение — строка вида `поле: сообщение`, все нарушения собираются разом.
 */
object Validators {

    /**
     * Формат почты — одна правда для валидаторов запросов и `AuthService.isValidEmail`:
     * непустое имя, `@`, домен с точкой, после последней точки — не меньше двух латинских букв.
     */
    val EMAIL = Regex("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val PERSON_NAME = Regex("[a-zA-Zа-яА-ЯёЁ\\s-]+")
    private val LETTERS_AND_SPACES = Regex("[a-zA-Zа-яА-ЯёЁ\\s]+")

    // ==================== Auth ====================

    val registerRequestValidator = validator<RegisterRequest> {
        todo("Глава 2, урок 7: правила регистрации по спецификации")
    }

    val loginRequestValidator = validator<LoginRequest> {
        todo("Глава 2, урок 7: правила входа")
    }

    val updateProfileRequestValidator = validator<UpdateUserProfileRequest> {
        todo("Глава 5, урок 22: правила обновления профиля")
    }

    // ==================== Courses ====================

    val courseRequestValidator = validator<CourseRequest> {
        todo("Глава 2, урок 7: правила курса")
    }

    // ==================== Lessons ====================

    val lessonRequestValidator = validator<LessonRequest> {
        todo("Глава 4, урок 17: правила урока")
    }

    // ==================== Tests (quizzes) ====================

    val testRequestValidator = validator<TestRequest> {
        field("title", { it.title }) {
            minLength(3, "Название теста должно содержать минимум 3 символа")
            maxLength(200, "Название теста слишком длинное (макс. 200 символов)")
        }
        field("description", { it.description }) {
            minLength(10, "Описание теста должно содержать минимум 10 символов")
            maxLength(1000, "Описание теста слишком длинное (макс. 1000 символов)")
        }
        number("passingScore", { it.passingScore }) {
            min(0, "Проходной балл не может быть отрицательным")
            max(100, "Проходной балл не может превышать 100")
        }
        number("timeLimit", { it.timeLimit }) {
            min(1, "Ограничение времени должно быть минимум 1 минута")
            max(300, "Ограничение времени не может превышать 300 минут")
        }
    }

    val questionRequestValidator = validator<QuestionRequest> {
        field("questionText", { it.questionText }) {
            minLength(5, "Текст вопроса должен содержать минимум 5 символов")
            maxLength(500, "Текст вопроса слишком длинный (макс. 500 символов)")
        }
        field("questionType", { it.questionType }) {
            oneOf(setOf("single_choice", "multiple_choice", "true_false"), "Тип вопроса должен быть: single_choice, multiple_choice или true_false")
        }
        number("points", { it.points }) {
            min(1, "Баллы за вопрос должны быть минимум 1")
            max(100, "Баллы за вопрос не могут превышать 100")
        }
        number("orderIndex", { it.orderIndex }) {
            min(0, "Индекс порядка не может быть отрицательным")
        }
        field("explanation", { it.explanation }) {
            minLength(10, "Объяснение должно содержать минимум 10 символов")
            maxLength(1000, "Объяснение слишком длинное (макс. 1000 символов)")
        }
    }

    val testSubmissionValidator = validator<TestSubmissionRequest> {
        rule("answers", "Должен быть хотя бы один ответ") { it.answers.isNotEmpty() }
    }

    val testAnswerValidator = validator<TestAnswerRequest> {
        field("questionId", { it.questionId }) { minLength(1, "ID вопроса обязателен") }
        rule("answerIds", "Должен быть выбран хотя бы один ответ") { it.answerIds.isNotEmpty() }
    }

    // ==================== Sprints ====================

    val sprintRequestValidator = validator<SprintRequest> {
        field("title", { it.title }) {
            minLength(3, "Название спринта должно содержать минимум 3 символа")
            maxLength(200, "Название спринта слишком длинное (макс. 200 символов)")
        }
        field("description", { it.description }) {
            minLength(10, "Описание спринта должно содержать минимум 10 символов")
            maxLength(2000, "Описание спринта слишком длинное (макс. 2000 символов)")
        }
        number("discount", { it.discount }) {
            min(0, "Скидка не может быть отрицательной")
            max(100, "Скидка не может превышать 100%")
        }
    }

    // ==================== Interview ====================

    val interviewQuestionValidator = validator<InterviewQuestionRequest> {
        field("category", { it.category }) {
            minLength(2, "Категория должна содержать минимум 2 символа")
            maxLength(50, "Категория слишком длинная")
        }
        field("questionText", { it.questionText }) {
            minLength(5, "Текст вопроса должен содержать минимум 5 символов")
            maxLength(1000, "Текст вопроса слишком длинный (макс. 1000 символов)")
        }
        field("difficulty", { it.difficulty }) {
            oneOf(setOf("junior", "middle", "senior"), "Уровень сложности должен быть: junior, middle или senior")
        }
    }

    val interviewAnswerValidator = validator<InterviewAnswerRequest> {
        field("questionId", { it.questionId }) { minLength(1, "ID вопроса обязателен") }
        field("answer", { it.answer }) {
            minLength(10, "Ответ должен содержать минимум 10 символов")
            maxLength(5000, "Ответ слишком длинный (макс. 5000 символов)")
        }
    }
}

/**
 * Правило пароля — одно для регистрации (валидатор запроса и `AuthService.register`)
 * и сброса пароля: от 8 до 100 символов, заглавная и строчная латинская буква, цифра.
 */
object PasswordPolicy {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 100
    const val MESSAGE = "Пароль должен содержать от 8 до 100 символов, заглавную и строчную латинскую букву и цифру"
    val UPPER = Regex(".*[A-Z].*")
    val LOWER = Regex(".*[a-z].*")
    val DIGIT = Regex(".*[0-9].*")

    fun isValid(password: String): Boolean =
        password.length in MIN_LENGTH..MAX_LENGTH &&
            UPPER.matches(password) && LOWER.matches(password) && DIGIT.matches(password)
}

/**
 * Регистрирует все валидаторы в плагине `RequestValidation`.
 */
fun RequestValidationConfig.registerValidators() {
    validate<RegisterRequest> { Validators.registerRequestValidator.validate(it) }
    validate<LoginRequest> { Validators.loginRequestValidator.validate(it) }
    validate<UpdateUserProfileRequest> { Validators.updateProfileRequestValidator.validate(it) }
    validate<CourseRequest> { Validators.courseRequestValidator.validate(it) }
    validate<LessonRequest> { Validators.lessonRequestValidator.validate(it) }
    validate<TestRequest> { Validators.testRequestValidator.validate(it) }
    validate<QuestionRequest> { Validators.questionRequestValidator.validate(it) }
    validate<TestSubmissionRequest> { Validators.testSubmissionValidator.validate(it) }
    validate<SprintRequest> { Validators.sprintRequestValidator.validate(it) }
    validate<InterviewQuestionRequest> { Validators.interviewQuestionValidator.validate(it) }
    validate<InterviewAnswerRequest> { Validators.interviewAnswerValidator.validate(it) }
}

// ==================== Мини-DSL правил ====================

/** Валидатор запроса: набор правил, каждое возвращает текст нарушения или null. */
class RequestValidator<T> internal constructor(private val rules: List<(T) -> String?>) {
    fun validate(value: T): ValidationResult {
        val reasons = rules.mapNotNull { it(value) }
        return if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
    }
}

fun <T> validator(block: RulesBuilder<T>.() -> Unit): RequestValidator<T> =
    RequestValidator(RulesBuilder<T>().apply(block).rules)

class RulesBuilder<T> internal constructor() {
    internal val rules = mutableListOf<(T) -> String?>()

    /** Заглушка шаблона: валидатор ещё не написан — вызов падает с текстом задания. */
    fun todo(message: String) {
        rules += { _ -> TODO(message) }
    }

    /** Строковое поле. Для null-значения правила не применяются (как `ifPresent`). */
    fun field(name: String, get: (T) -> String?, block: StringRules.() -> Unit) {
        val checks = StringRules().apply(block).checks
        checks.forEach { (ok, message) ->
            rules += { value -> get(value)?.let { if (ok(it)) null else "$name: $message" } }
        }
    }

    /** Числовое поле. Для null-значения правила не применяются. */
    fun <N : Comparable<N>> number(name: String, get: (T) -> N?, block: NumberRules<N>.() -> Unit) {
        val checks = NumberRules<N>().apply(block).checks
        checks.forEach { (ok, message) ->
            rules += { value -> get(value)?.let { if (ok(it)) null else "$name: $message" } }
        }
    }

    /** Произвольное правило над всем запросом. */
    fun rule(name: String, message: String, ok: (T) -> Boolean) {
        rules += { value -> if (ok(value)) null else "$name: $message" }
    }
}

class StringRules internal constructor() {
    internal val checks = mutableListOf<Pair<(String) -> Boolean, String>>()
    fun minLength(n: Int, message: String) { checks += { s: String -> s.length >= n } to message }
    fun maxLength(n: Int, message: String) { checks += { s: String -> s.length <= n } to message }
    fun pattern(regex: Regex, message: String) { checks += { s: String -> regex.matches(s) } to message }
    fun oneOf(allowed: Set<String>, message: String) { checks += { s: String -> s in allowed } to message }
}

class NumberRules<N : Comparable<N>> internal constructor() {
    internal val checks = mutableListOf<Pair<(N) -> Boolean, String>>()
    fun min(n: N, message: String) { checks += { v: N -> v >= n } to message }
    fun max(n: N, message: String) { checks += { v: N -> v <= n } to message }
}

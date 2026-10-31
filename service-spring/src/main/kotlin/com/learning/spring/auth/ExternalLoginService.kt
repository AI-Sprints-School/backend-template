package com.learning.spring.auth

import com.learning.spring.models.AuthResponse
import org.springframework.stereotype.Service

/**
 * Кто пришёл от провайдера: данные из `id_token` (OpenID Connect) после успешного входа.
 *
 * @property provider registrationId клиента в верхнем регистре (`KEYCLOAK`) — так он лежит в `users.auth_provider`
 * @property subject claim `sub` — постоянный id учётки у провайдера (`users.external_subject`)
 * @property emailVerified claim `email_verified`: почта подтверждена провайдером
 */
data class ExternalIdentity(
    val provider: String,
    val subject: String,
    val email: String,
    val emailVerified: Boolean,
    val firstName: String,
    val lastName: String,
)

/**
 * Урок 15: привязка внешней учётки к пользователю сервиса и выдача своего JWT (того же, что урок 12).
 *
 * Правило привязки:
 * 1. есть пользователь с парой «провайдер + `sub`» — это он;
 * 2. иначе есть пользователь с той же почтой и почта подтверждена провайдером (`emailVerified`) —
 *    привязать учётку к нему (записать `auth_provider` и `external_subject`) и сбросить `password_hash` в `NULL`:
 *    иначе тот, кто заранее зарегистрировал эту почту паролем, входил бы им и после привязки;
 *    почта не подтверждена — отказ исключением, к чужому аккаунту не привязывать;
 * 3. иначе создать пользователя с ролью `student` и `password_hash = NULL`.
 *
 * Вызывает обработчик успешного входа цепочки `oauth2Login`; тесты `OAuthLoginTest` вызывают сервис
 * напрямую — без браузера и без запущенного Keycloak. Заглушка шаблона: класс и сигнатуру не менять,
 * зависимости в конструктор добавить можно.
 */
@Service
class ExternalLoginService {

    fun login(identity: ExternalIdentity): AuthResponse =
        TODO("Глава 3, урок 15: найти или привязать пользователя по правилу выше и выдать AuthResponse урока 12")
}

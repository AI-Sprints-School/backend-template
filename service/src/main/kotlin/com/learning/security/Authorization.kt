package com.learning.security

import com.learning.models.ErrorResponse
import com.learning.utils.ErrorTypes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Проверка роли — второй шаг после аутентификации.
 *
 * Разделение ответственности:
 * - `authenticate("auth-jwt")` отвечает на вопрос «кто это?» — нет токена или он
 *   недействителен → **401**;
 * - [withRole] отвечает на вопрос «можно ли ему?» — токен верный, но роли нет → **403**.
 *
 * Роль берётся из claim `role` access-токена: база на каждый запрос не опрашивается.
 * Цена решения — роль, снятая в базе, действует до истечения токена (срок access-токена
 * короткий, см. `jwt.accessTokenExpiration`).
 */
class RoleAuthorizationConfig {
    var roles: Set<String> = emptySet()
}

val RoleAuthorization = createRouteScopedPlugin("RoleAuthorization", ::RoleAuthorizationConfig) {
    val allowed = pluginConfig.roles
    on(AuthenticationChecked) { call ->
        if (call.isHandled) return@on
        TODO("Глава 5, урок 23: роль из claim role; нет нужной роли — 403 AUTHORIZATION_ERROR")
    }
}

/**
 * Маршруты внутри доступны только пользователям с одной из [roles].
 * Ставится внутри `authenticate { }`: без аутентификации роли не бывает.
 */
fun Route.withRole(vararg roles: String, build: Route.() -> Unit): Route {
    val route = createChild(RoleRouteSelector(roles.toSet()))
    route.install(RoleAuthorization) { this.roles = roles.toSet() }
    route.build()
    return route
}

private class RoleRouteSelector(private val roles: Set<String>) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Transparent

    override fun toString(): String = "(role ${roles.joinToString()})"
}

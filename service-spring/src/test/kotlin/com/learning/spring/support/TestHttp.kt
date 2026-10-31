package com.learning.spring.support

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Ответ сервиса: код, тело и заголовки (имена — в нижнем регистре); `json` разбирает тело как JSON. */
class Resp(val status: Int, val body: String, val headers: Map<String, List<String>> = emptyMap()) {
    /** Все значения заголовка `name` (регистр имени не важен). */
    fun header(name: String): List<String> = headers[name.lowercase()].orEmpty()

    val json: JsonNode by lazy { TestHttp.mapper.readTree(body) }

    /** Значения поля `field` у элементов массива `array`. */
    fun strings(array: String, field: String): List<String> =
        (0 until json[array].size()).map { json[array][it][field].asString() }
}

/** Клиент HTTP для тестов: сервис поднят на случайном порту. Готово в шаблоне. */
class TestHttp(private val baseUrl: String) {

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    // Редиректы не выполняются: тест видит сам ответ 302 и его Location (урок 15)
    fun get(path: String, bearer: String? = null) = send("GET", path, null, bearer)

    fun post(path: String, json: String? = null, bearer: String? = null) = send("POST", path, json ?: "", bearer)

    fun put(path: String, json: String, bearer: String? = null) = send("PUT", path, json, bearer)

    private fun send(method: String, path: String, body: String?, bearer: String?): Resp {
        val builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30))
        if (body != null) builder.header("Content-Type", "application/json")
        bearer?.let { builder.header("Authorization", "Bearer $it") }
        builder.method(method, if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body))
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        val headers = response.headers().map().mapKeys { it.key.lowercase() }
        return Resp(response.statusCode(), response.body(), headers)
    }

    companion object {
        val mapper: JsonMapper = JsonMapper.builder().build()
    }
}

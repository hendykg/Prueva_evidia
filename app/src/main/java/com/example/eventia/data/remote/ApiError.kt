package com.example.eventia.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.Response

/** Error HTTP con código, para que los ViewModels distingan 400/401/404/409/422/5xx. */
class ApiException(val code: Int, override val message: String) : Exception(message)

/**
 * Cuerpo de error de la API (colección Postman):
 * `{ "code": "EVENT_FULL" | "UNAUTHENTICATED" | "VALIDATION_ERROR" | "NOT_FOUND" | ...,
 *    "message": "...", "fields": { "email": "..." } }`
 */
data class ServerErrorDto(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("fields") val fields: Map<String, JsonElement>?
)

/**
 * Extrae el mensaje legible del error del servidor (regla 45/64):
 * usa `message` y, si existen, los mensajes por campo de `fields`.
 */
fun <T> Response<T>.serverErrorMessage(fallback: String): String {
    val raw = try {
        errorBody()?.string()
    } catch (e: Exception) {
        null
    }
    if (raw.isNullOrBlank()) return fallback
    return try {
        val dto = Gson().fromJson(raw, ServerErrorDto::class.java)
        val parts = buildList {
            dto?.message?.takeIf { it.isNotBlank() }?.let { add(it) }
            dto?.fields?.values?.forEach { value -> addAll(fieldMessages(value)) }
            // Sin mensaje legible, se usa el código de error (p. ej. EVENT_FULL)
            if (isEmpty()) dto?.code?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        parts.joinToString("\n").ifBlank { fallback }
    } catch (e: Exception) {
        fallback
    }
}

/** Un campo de validación puede traer un mensaje (string) o una lista de mensajes. */
private fun fieldMessages(value: JsonElement): List<String> = when {
    value.isJsonArray -> value.asJsonArray
        .mapNotNull { if (it.isJsonPrimitive) it.asString else null }

    value.isJsonPrimitive -> listOf(value.asString)
    else -> emptyList()
}

package com.example.eventia.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

/** Error HTTP con código, para que los ViewModels distingan 400/401/404/409/422/5xx. */
class ApiException(val code: Int, override val message: String) : Exception(message)

/** Formato esperado del cuerpo de error del servidor (provisional). */
data class ServerErrorDto(
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: Map<String, List<String>>?
)

/**
 * Extrae el mensaje legible del error del servidor (regla 45/64):
 * usa `message` y/o los campos de validación `errors`.
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
            dto?.errors?.values?.flatten()?.filter { it.isNotBlank() }?.let { addAll(it) }
        }
        parts.joinToString("\n").ifBlank { fallback }
    } catch (e: Exception) {
        fallback
    }
}

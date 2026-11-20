package com.example.eventia.data.remote

import retrofit2.Response

/**
 * Utilidades para convertir llamadas HTTP en [Result].
 * Centralizan el try/catch y la construcción de [ApiException] para que
 * cada repositorio describa solo su regla de negocio, no el mecanismo
 * de error.
 */

/** Ejecuta [block]; cualquier excepción de red o de parseo queda como fallo. */
suspend fun <T> apiCall(block: suspend () -> Result<T>): Result<T> = try {
    block()
} catch (e: Exception) {
    Result.failure(e)
}

/** ApiException con el mensaje legible del servidor (o [fallback] si no hay). */
fun <T> Response<T>.toApiException(fallback: String): ApiException =
    ApiException(code(), serverErrorMessage(fallback))

/**
 * Intenta [remote] y, si falla (sin conexión o error HTTP), recurre a la
 * caché de Room de [cache]. Excepción: un 404 significa que el recurso ya no
 * existe en el servidor, por lo que no se muestra una copia obsoleta
 * (API 8.3: un evento borrado responde "Este evento ya no está disponible").
 */
suspend fun <T> remoteOrCache(
    remote: suspend () -> Result<T>,
    cache: suspend () -> T?
): Result<T> {
    val result = apiCall(remote)
    if (result.isSuccess) return result
    if ((result.exceptionOrNull() as? ApiException)?.code == 404) return result
    return try {
        cache()?.let { Result.success(it) } ?: result
    } catch (e: Exception) {
        result
    }
}

package com.example.eventia.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Proveedor único de la API (mismo patrón que RetrofitRepository de PracticaWebAPI).
 * Diferencia: la instancia de Retrofit y del servicio se crean una sola vez
 * (by lazy) y se reutilizan en toda la app. No existe otro Retrofit/OkHttp
 * en el proyecto: esta es la única fuente.
 */
object RetrofitRepository {
    /**
     * URL base de la API (única fuente de verdad). Debe terminar en "/",
     * si no Retrofit lanza excepción al construirse.
     */
    const val BASE_URL = "https://eventia.jmacboy.dev/api/"

    @Volatile
    private var okHttpClient: OkHttpClient? = null

    private val retrofit: Retrofit by lazy {
        val builder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
        // Si NetworkModule ya instaló el cliente, se usa (Bearer + logging + timeouts)
        okHttpClient?.let { builder.client(it) }
        builder.build()
    }

    /** Servicio listo para usar (patrón RetrofitClient), creado una sola vez. */
    val api: EventiaApi by lazy {
        retrofit.create(EventiaApi::class.java)
    }

    /**
     * Instala el OkHttpClient con el AuthInterceptor (Hilt) ANTES del primer
     * acceso a [api]. Lo hace NetworkModule.provideEventiaApi al inyectar la API.
     */
    fun installClient(client: OkHttpClient) {
        if (okHttpClient == null) {
            okHttpClient = client
        }
    }
}

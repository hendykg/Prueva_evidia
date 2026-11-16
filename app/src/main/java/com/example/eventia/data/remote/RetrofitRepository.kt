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
     * URL base de la API (única fuente de verdad):
     * - Emulador Android Studio (ACTUAL): "http://10.0.2.2:8000/api/"
     *   (en el emulador "localhost" es el propio emulador; 10.0.2.2 apunta a la PC)
     * - Celular Físico conectado por USB: "http://localhost:8000/api/"
     *   (usando adb reverse tcp:8000 tcp:8000)
     * - Servidor en la nube: la URL pública terminada en "/api/"
     */
    const val BASE_URL = "http://10.0.2.2:8000/api/"

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
     * acceso a [api] o [getInstance]. Lo hace NetworkModule.provideEventiaApi
     * al inyectar la API.
     */
    fun installClient(client: OkHttpClient) {
        if (okHttpClient == null) {
            okHttpClient = client
        }
    }

    fun getInstance(): Retrofit = retrofit
}

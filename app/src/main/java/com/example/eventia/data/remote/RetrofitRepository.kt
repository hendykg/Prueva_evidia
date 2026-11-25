package com.example.eventia.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitRepository {
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

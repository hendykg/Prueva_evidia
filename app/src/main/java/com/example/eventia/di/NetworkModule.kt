package com.example.eventia.di

import com.example.eventia.data.remote.AuthInterceptor
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.RetrofitRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Cliente con Authorization: Bearer (token de DataStore) + logging + timeouts. */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * La instancia de Retrofit la provee RetrofitRepository (patrón del curso);
     * aquí solo se instala el cliente autenticado y se toma el servicio ya creado.
     */
    @Provides
    @Singleton
    fun provideEventiaApi(okHttpClient: OkHttpClient): EventiaApi {
        RetrofitRepository.installClient(okHttpClient)
        return RetrofitRepository.api
    }
}

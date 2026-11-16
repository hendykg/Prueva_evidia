package com.example.eventia.data.remote

import com.example.eventia.data.remote.dto.AuthRequestDto
import com.example.eventia.data.remote.dto.AuthResponseDto
import com.example.eventia.data.remote.dto.CategoryDto
import com.example.eventia.data.remote.dto.EventDto
import com.example.eventia.data.remote.dto.UserDto
import com.example.eventia.data.remote.dto.VenueDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato HTTP de la API Eventia (colección Postman "Eventia API").
 * Cada método coincide 1:1 con un endpoint documentado; no hay endpoints inventados.
 */
interface EventiaApi {
    /** Crea la cuenta (201) y devuelve {token, user}. */
    @POST("auth/register")
    suspend fun register(@Body body: AuthRequestDto): Response<AuthResponseDto>

    /** Abre sesión (200) y devuelve {token}. */
    @POST("auth/login")
    suspend fun login(@Body body: AuthRequestDto): Response<AuthResponseDto>

    /** Usuario autenticado por el token (401 si el token no es válido). */
    @GET("me")
    suspend fun me(): Response<UserDto>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    /**
     * Catálogo de eventos próximos.
     * @param query texto a buscar en el nombre → parámetro `q` de la API.
     * @param categoryId id de la categoría → parámetro `category` de la API.
     */
    @GET("events")
    suspend fun getEvents(
        @Query("q") query: String? = null,
        @Query("category") categoryId: String? = null
    ): Response<List<EventDto>>

    /** Detalle de un evento; con token la API incluye isAttending. */
    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: String): Response<EventDto>

    @GET("venues")
    suspend fun getVenues(): Response<List<VenueDto>>

    @GET("venues/{id}")
    suspend fun getVenue(@Path("id") id: String): Response<VenueDto>

    /** Próximos eventos del lugar. */
    @GET("venues/{id}/events")
    suspend fun getVenueEvents(@Path("id") id: String): Response<List<EventDto>>

    /** Inscribe (201) y devuelve el evento actualizado; 409 EVENT_FULL sin cupos. */
    @POST("events/{id}/attendance")
    suspend fun attend(@Path("id") id: String): Response<EventDto>

    /** Cancela (200) y devuelve el evento actualizado. */
    @DELETE("events/{id}/attendance")
    suspend fun cancelAttendance(@Path("id") id: String): Response<EventDto>

    /** Eventos (pasados y próximos) en los que el usuario está inscrito. */
    @GET("me/attendances")
    suspend fun myAttendances(): Response<List<EventDto>>
}

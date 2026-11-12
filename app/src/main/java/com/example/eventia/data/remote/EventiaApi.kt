package com.example.eventia.data.remote

import com.example.eventia.data.remote.dto.AttendanceDto
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

interface EventiaApi {
    @POST("auth/register")
    suspend fun register(@Body body: AuthRequestDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequestDto): Response<AuthResponseDto>

    @GET("me")
    suspend fun me(): Response<UserDto>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @GET("events")
    suspend fun getEvents(
        @Query("search") search: String? = null,
        @Query("category") categoryId: String? = null,
        @Query("upcoming") upcoming: Boolean? = null
    ): Response<List<EventDto>>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: String): Response<EventDto>

    @GET("venues")
    suspend fun getVenues(): Response<List<VenueDto>>

    @GET("venues/{id}")
    suspend fun getVenue(@Path("id") id: String): Response<VenueDto>

    @GET("venues/{id}/events")
    suspend fun getVenueEvents(@Path("id") id: String): Response<List<EventDto>>

    @POST("events/{id}/attendance")
    suspend fun attend(@Path("id") id: String): Response<Unit>

    @DELETE("events/{id}/attendance")
    suspend fun cancelAttendance(@Path("id") id: String): Response<Unit>

    @GET("me/attendances")
    suspend fun myAttendances(): Response<List<AttendanceDto>>
}

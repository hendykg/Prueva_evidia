package com.example.eventia.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Evento tal como lo devuelve la API (campos documentados en la colección
 * Postman): la fecha va en `startsAt`, categoría y lugar llegan como objetos
 * anidados y `isAttending` solo se incluye cuando la petición lleva token.
 */
data class EventDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("startsAt") val startsAt: String?,
    @SerializedName("category") val category: CategoryDto?,
    @SerializedName("venue") val venue: VenueDto?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("capacity") val capacity: Int?,
    @SerializedName("availableSpots") val availableSpots: Int?,
    // Coordenadas: el enunciado las envía como lat/lng (a veces solo en el venue anidado)
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("isAttending") val isAttending: Boolean? = null
)

data class CategoryDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class VenueDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("description") val description: String?,
    // El enunciado no fija el nombre del campo de horario: se aceptan ambos nombres
    @SerializedName("openingHours") val openingHours: String?,
    @SerializedName("schedule") val schedule: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("upcomingEventsCount") val upcomingEventsCount: Int?
)

data class AuthRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String? = null
)

data class AuthResponseDto(
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: UserDto?
)

data class UserDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?
)

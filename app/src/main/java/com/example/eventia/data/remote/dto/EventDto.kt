package com.example.eventia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EventDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("category") val category: CategoryDto?,
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("venue") val venue: VenueDto?,
    @SerializedName("venueId") val venueId: String?,
    @SerializedName("place") val place: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("capacity") val capacity: Int?,
    @SerializedName("availableSpots") val availableSpots: Int?,
    @SerializedName("cups") val cups: Int?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
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
    @SerializedName("schedule") val schedule: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
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

data class AttendanceDto(
    @SerializedName("id") val id: String?,
    @SerializedName("eventId") val eventId: String?,
    @SerializedName("event") val event: EventDto?,
    @SerializedName("status") val status: String?
)

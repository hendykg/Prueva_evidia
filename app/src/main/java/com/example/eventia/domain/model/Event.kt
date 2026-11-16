package com.example.eventia.domain.model

data class Event(
    val id: String,
    val title: String,
    val summary: String?,
    val description: String?,
    val date: String?,
    val categoryId: String?,
    val categoryName: String?,
    val venueId: String?,
    val venueName: String?,
    val place: String?,
    val imageUrl: String?,
    val capacity: Int?,
    val availableSpots: Int?,
    val latitude: Double?,
    val longitude: Double?
)

data class Category(
    val id: String,
    val name: String
)

data class Venue(
    val id: String,
    val name: String,
    val address: String?,
    val description: String?,
    val schedule: String?,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val upcomingEventsCount: Int?
)

data class User(
    val id: String,
    val name: String?,
    val email: String?
)

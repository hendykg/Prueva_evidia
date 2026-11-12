package com.example.eventia.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
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

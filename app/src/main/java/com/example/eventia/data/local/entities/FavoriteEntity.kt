package com.example.eventia.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favorito con copia completa del evento (RF-04).
 * Se guarda el snapshot para que el favorito sea permanente y siga visible
 * aunque el catálogo se actualice o el dispositivo esté sin Internet.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val eventId: String,
    val addedAt: Long,
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

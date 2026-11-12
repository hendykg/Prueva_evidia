package com.example.eventia.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String?,
    val description: String?,
    val schedule: String?,
    val latitude: Double?,
    val longitude: Double?
)

package com.example.eventia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.eventia.data.local.dao.EventDao
import com.example.eventia.data.local.dao.FavoriteDao
import com.example.eventia.data.local.dao.VenueDao
import com.example.eventia.data.local.entities.CategoryEntity
import com.example.eventia.data.local.entities.EventEntity
import com.example.eventia.data.local.entities.FavoriteEntity
import com.example.eventia.data.local.entities.VenueEntity

@Database(
    entities = [
        EventEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        VenueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EventiaDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun venueDao(): VenueDao
}

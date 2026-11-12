package com.example.eventia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.eventia.data.local.entities.VenueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {
    @Query("SELECT * FROM venues ORDER BY name ASC")
    fun observeVenues(): Flow<List<VenueEntity>>

    @Query("SELECT * FROM venues WHERE id = :id LIMIT 1")
    suspend fun getVenueById(id: String): VenueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVenues(venues: List<VenueEntity>)

    @Query("DELETE FROM venues")
    suspend fun clearVenues()
}

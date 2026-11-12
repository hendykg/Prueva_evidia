package com.example.eventia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.eventia.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    /** Lista completa de favoritos con su snapshot (funciona sin Internet). */
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    /** Ids de favoritos, para mostrar el estado del corazón en detalle/catálogo. */
    @Query("SELECT eventId FROM favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE eventId = :eventId")
    suspend fun removeFavorite(eventId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE eventId = :eventId)")
    suspend fun isFavorite(eventId: String): Boolean
}

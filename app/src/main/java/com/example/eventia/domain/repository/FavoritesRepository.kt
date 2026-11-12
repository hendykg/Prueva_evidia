package com.example.eventia.domain.repository

import com.example.eventia.domain.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * RF-04 / RF-05: los favoritos se guardan en Room de forma permanente
 * y se pueden agregar o quitar sin Internet.
 */
interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Event>>
    fun observeFavoriteIds(): Flow<List<String>>
    suspend fun addFavorite(event: Event)
    suspend fun removeFavorite(eventId: String)
    suspend fun isFavorite(eventId: String): Boolean
}

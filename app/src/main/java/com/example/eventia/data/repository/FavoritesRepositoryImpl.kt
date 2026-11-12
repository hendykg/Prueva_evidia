package com.example.eventia.data.repository

import com.example.eventia.data.local.dao.FavoriteDao
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.mapper.toFavoriteEntity
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RF-04 / RF-05: opera exclusivamente sobre Room, por lo que agregar y
 * quitar favoritos funciona sin Internet. El snapshot del evento se guarda
 * en la tabla favorites, así una sincronización del catálogo no lo pierde.
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun observeFavorites(): Flow<List<Event>> =
        favoriteDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<List<String>> =
        favoriteDao.observeFavoriteIds()

    override suspend fun addFavorite(event: Event) {
        favoriteDao.addFavorite(event.toFavoriteEntity())
    }

    override suspend fun removeFavorite(eventId: String) {
        favoriteDao.removeFavorite(eventId)
    }

    override suspend fun isFavorite(eventId: String): Boolean =
        favoriteDao.isFavorite(eventId)
}

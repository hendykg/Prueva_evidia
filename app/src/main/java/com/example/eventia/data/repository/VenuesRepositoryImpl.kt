package com.example.eventia.data.repository

import androidx.room.withTransaction
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.dao.EventDao
import com.example.eventia.data.local.dao.VenueDao
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.mapper.toEventEntities
import com.example.eventia.data.mapper.toEntity
import com.example.eventia.data.mapper.toVenueEntities
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.apiCall
import com.example.eventia.data.remote.remoteOrCache
import com.example.eventia.data.remote.toApiException
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.domain.repository.VenuesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lugares (RF-13): lista y detalle con caché en Room para funcionar
 * sin conexión, igual que el catálogo de eventos.
 */
@Singleton
class VenuesRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val database: EventiaDatabase,
    private val venueDao: VenueDao,
    private val eventDao: EventDao
) : VenuesRepository {

    override fun observeVenues(): Flow<List<Venue>> =
        venueDao.observeVenues().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshVenues(): Result<Unit> = apiCall {
        val response = api.getVenues()
        if (response.isSuccessful) {
            val entities = response.body().orEmpty().toVenueEntities()
            database.withTransaction {
                venueDao.clearVenues()
                venueDao.upsertVenues(entities)
            }
            Result.success(Unit)
        } else {
            Result.failure(response.toApiException("No se pudieron cargar los lugares"))
        }
    }

    override suspend fun getVenue(id: String): Result<Venue> = remoteOrCache(
        remote = {
            val response = api.getVenue(id)
            val dto = response.body()
            if (response.isSuccessful && dto != null) {
                val entity = dto.copy(id = dto.id ?: id).toEntity()
                venueDao.upsertVenues(listOf(entity))
                Result.success(entity.toDomain())
            } else {
                Result.failure(response.toApiException("No se pudo cargar el lugar"))
            }
        },
        // Sin conexión: detalle del lugar desde Room
        cache = { venueDao.getVenueById(id)?.toDomain() }
    )

    override suspend fun getVenueEvents(venueId: String): Result<List<Event>> = remoteOrCache(
        remote = {
            val response = api.getVenueEvents(venueId)
            if (response.isSuccessful) {
                val entities = response.body().orEmpty().toEventEntities()
                // Actualiza la caché de eventos en transacción; favoritos no se tocan
                database.withTransaction { eventDao.upsertEvents(entities) }
                Result.success(entities.map { it.toDomain() })
            } else {
                Result.failure(response.toApiException("No se pudieron cargar los eventos del lugar"))
            }
        },
        // Sin Internet: eventos del lugar desde Room
        cache = { eventDao.getEventsByVenue(venueId).takeIf { it.isNotEmpty() }?.map { it.toDomain() } }
    )
}

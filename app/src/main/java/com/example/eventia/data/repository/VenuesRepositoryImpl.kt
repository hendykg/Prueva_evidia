package com.example.eventia.data.repository

import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.dao.EventDao
import com.example.eventia.data.local.dao.VenueDao
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.mapper.toEntity
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.domain.repository.VenuesRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenuesRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val database: EventiaDatabase,
    private val venueDao: VenueDao,
    private val eventDao: EventDao
) : VenuesRepository {

    override fun observeVenues(): Flow<List<Venue>> =
        venueDao.observeVenues().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshVenues(): Result<Unit> {
        return try {
            val response = api.getVenues()
            if (response.isSuccessful) {
                val entities = response.body().orEmpty().mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    dto.copy(id = id).toEntity()
                }
                database.withTransaction {
                    venueDao.clearVenues()
                    venueDao.upsertVenues(entities)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVenue(id: String): Result<Venue> {
        return try {
            val response = api.getVenue(id)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val entity = dto.copy(id = dto.id ?: id).toEntity()
                    venueDao.upsertVenues(listOf(entity))
                    Result.success(entity.toDomain())
                } else {
                    val local = venueDao.getVenueById(id)
                    if (local != null) Result.success(local.toDomain())
                    else Result.failure(Exception("Lugar no encontrado"))
                }
            } else {
                val local = venueDao.getVenueById(id)
                if (local != null) Result.success(local.toDomain())
                else Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            val local = venueDao.getVenueById(id)
            if (local != null) Result.success(local.toDomain())
            else Result.failure(e)
        }
    }

    override suspend fun getVenueEvents(venueId: String): Result<List<Event>> {
        return try {
            val response = api.getVenueEvents(venueId)
            if (response.isSuccessful) {
                val entities = response.body().orEmpty().mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    dto.copy(id = id).toEntity()
                }
                // Actualiza la caché de eventos en transacción; favoritos no se tocan
                database.withTransaction {
                    eventDao.upsertEvents(entities)
                }
                Result.success(entities.map { it.toDomain() })
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            // Sin Internet: eventos del lugar desde Room
            val local = eventDao.getEventsByVenue(venueId)
            if (local.isNotEmpty()) Result.success(local.map { it.toDomain() })
            else Result.failure(e)
        }
    }
}

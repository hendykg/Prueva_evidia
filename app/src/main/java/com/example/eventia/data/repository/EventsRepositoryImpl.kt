package com.example.eventia.data.repository

import com.example.eventia.data.datastore.SessionDataStore
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.dao.EventDao
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.mapper.toEntity
import com.example.eventia.data.remote.ApiException
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.serverErrorMessage
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.EventsRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val database: EventiaDatabase,
    private val eventDao: EventDao,
    private val sessionDataStore: SessionDataStore
) : EventsRepository {

    override fun observeEvents(): Flow<List<Event>> =
        eventDao.observeEvents().map { list -> list.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        eventDao.observeCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshEvents(query: String?, categoryId: String?): Result<Unit> {
        return try {
            val response = api.getEvents(query = query, categoryId = categoryId)
            if (response.isSuccessful) {
                val body = response.body().orEmpty()
                val entities = body.mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    dto.copy(id = id).toEntity()
                }
                // Transacción: reemplaza la caché del catálogo sin tocar la tabla de favoritos
                database.withTransaction {
                    eventDao.clearEvents()
                    eventDao.upsertEvents(entities)
                }
                sessionDataStore.saveLastSync(System.currentTimeMillis())
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshCategories(): Result<Unit> {
        return try {
            val response = api.getCategories()
            if (response.isSuccessful) {
                val entities = response.body().orEmpty().mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    dto.copy(id = id).toEntity()
                }
                database.withTransaction {
                    eventDao.clearCategories()
                    eventDao.upsertCategories(entities)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEvent(id: String): Result<Event> {
        return try {
            val response = api.getEvent(id)
            if (response.isSuccessful) {
                val dto = response.body() ?: return Result.failure(Exception("Evento no encontrado"))
                Result.success(dto.toDomain())
            } else if (response.code() == 404) {
                // API 8.3: 404 → "Este evento ya no está disponible"
                Result.failure(ApiException(404, "Este evento ya no está disponible"))
            } else {
                // Sin conexión u otro error: se usa la caché de Room si existe
                val local = eventDao.getEventById(id)
                if (local != null) Result.success(local.toDomain())
                else Result.failure(
                    ApiException(response.code(), response.serverErrorMessage("No se pudo cargar el evento"))
                )
            }
        } catch (e: Exception) {
            val local = eventDao.getEventById(id)
            if (local != null) Result.success(local.toDomain())
            else Result.failure(e)
        }
    }

    override suspend fun getLastSyncMillis(): Long? = sessionDataStore.getLastSync()
}

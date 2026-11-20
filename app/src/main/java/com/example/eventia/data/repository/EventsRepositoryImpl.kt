package com.example.eventia.data.repository

import androidx.room.withTransaction
import com.example.eventia.data.datastore.SessionDataStore
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.dao.EventDao
import com.example.eventia.data.mapper.toCategoryEntities
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.mapper.toEventEntities
import com.example.eventia.data.mapper.toEntity
import com.example.eventia.data.remote.ApiException
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.apiCall
import com.example.eventia.data.remote.remoteOrCache
import com.example.eventia.data.remote.toApiException
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catálogo: la API es la fuente de verdad y Room es la caché que permite
 * trabajar sin conexión (RF-05, RF-14).
 */
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

    override suspend fun refreshEvents(query: String?, categoryId: String?): Result<Unit> = apiCall {
        val response = api.getEvents(query = query, categoryId = categoryId)
        if (response.isSuccessful) {
            val entities = response.body().orEmpty().toEventEntities()
            // Transacción: reemplaza la caché del catálogo sin tocar favoritos
            database.withTransaction {
                eventDao.clearEvents()
                eventDao.upsertEvents(entities)
            }
            sessionDataStore.saveLastSync(System.currentTimeMillis())
            Result.success(Unit)
        } else {
            Result.failure(response.toApiException("No se pudo actualizar el catálogo"))
        }
    }

    override suspend fun refreshCategories(): Result<Unit> = apiCall {
        val response = api.getCategories()
        if (response.isSuccessful) {
            val entities = response.body().orEmpty().toCategoryEntities()
            database.withTransaction {
                eventDao.clearCategories()
                eventDao.upsertCategories(entities)
            }
            Result.success(Unit)
        } else {
            Result.failure(response.toApiException("No se pudieron cargar las categorías"))
        }
    }

    override suspend fun getEvent(id: String): Result<Event> = remoteOrCache(
        remote = {
            val response = api.getEvent(id)
            when {
                response.isSuccessful ->
                    response.body()?.toDomain()?.let { Result.success(it) }
                        ?: Result.failure(ApiException(response.code(), "Respuesta inválida del servidor"))

                // API 8.3: 404 → mensaje del enunciado (remoteOrCache no usa caché)
                response.code() == 404 ->
                    Result.failure(ApiException(404, EVENT_UNAVAILABLE))

                else -> Result.failure(response.toApiException("No se pudo cargar el evento"))
            }
        },
        // Sin conexión u otro error: detalle desde la caché de Room
        cache = { eventDao.getEventById(id)?.toDomain() }
    )

    override suspend fun getLastSyncMillis(): Long? = sessionDataStore.getLastSync()

    private companion object {
        /** API 8.3: mensaje obligatorio cuando el evento ya no existe. */
        const val EVENT_UNAVAILABLE = "Este evento ya no está disponible"
    }
}

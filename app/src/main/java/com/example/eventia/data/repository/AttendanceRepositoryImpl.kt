package com.example.eventia.data.repository

import com.example.eventia.data.datastore.SessionDataStore
import com.example.eventia.data.mapper.toAttendance
import com.example.eventia.data.remote.ApiException
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.serverErrorMessage
import com.example.eventia.domain.model.Attendance
import com.example.eventia.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RF-12: inscribir/cancelar confirmando con el servidor (regla 41:
 * no se considera exitosa sin respuesta HTTP 2xx).
 * - 401 → limpia la sesión para que la UI redirija al login.
 * - 409 → se propaga el conflicto (ya inscrito / sin cupos).
 */
@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val sessionDataStore: SessionDataStore
) : AttendanceRepository {

    private val myEventIds = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun attend(eventId: String): Result<Unit> {
        return try {
            val response = api.attend(eventId)
            when {
                response.isSuccessful -> {
                    myEventIds.value = myEventIds.value + eventId
                    Result.success(Unit)
                }

                response.code() == 401 -> {
                    sessionDataStore.clearToken()
                    Result.failure(ApiException(401, "Sesión expirada. Inicia sesión nuevamente."))
                }

                else -> Result.failure(
                    ApiException(
                        response.code(),
                        response.serverErrorMessage("No se pudo completar la inscripción")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelAttendance(eventId: String): Result<Unit> {
        return try {
            val response = api.cancelAttendance(eventId)
            when {
                response.isSuccessful -> {
                    myEventIds.value = myEventIds.value - eventId
                    Result.success(Unit)
                }

                response.code() == 401 -> {
                    sessionDataStore.clearToken()
                    Result.failure(ApiException(401, "Sesión expirada. Inicia sesión nuevamente."))
                }

                else -> Result.failure(
                    ApiException(response.code(), response.serverErrorMessage("No se pudo cancelar la inscripción"))
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyAttendances(): Result<List<Attendance>> {
        return try {
            val response = api.myAttendances()
            if (response.isSuccessful) {
                val attendances = response.body().orEmpty().map { it.toAttendance() }
                myEventIds.value = attendances.map { it.eventId }.toSet()
                Result.success(attendances)
            } else {
                if (response.code() == 401) {
                    sessionDataStore.clearToken()
                }
                Result.failure(
                    ApiException(response.code(), response.serverErrorMessage("No se pudieron cargar tus inscripciones"))
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeMyEventIds(): Flow<Set<String>> = myEventIds.asStateFlow()
}

package com.example.eventia.data.repository

import com.example.eventia.data.datastore.SessionDataStore
import com.example.eventia.data.mapper.toAttendance
import com.example.eventia.data.remote.ApiException
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.apiCall
import com.example.eventia.data.remote.dto.EventDto
import com.example.eventia.data.remote.toApiException
import com.example.eventia.domain.model.Attendance
import com.example.eventia.domain.repository.AttendanceRepository
import retrofit2.Response
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

    override suspend fun attend(eventId: String): Result<Unit> =
        changeAttendance(
            call = { api.attend(eventId) },
            onSuccess = { myEventIds.value = myEventIds.value + eventId },
            fallback = "No se pudo completar la inscripción"
        )

    override suspend fun cancelAttendance(eventId: String): Result<Unit> =
        changeAttendance(
            call = { api.cancelAttendance(eventId) },
            onSuccess = { myEventIds.value = myEventIds.value - eventId },
            fallback = "No se pudo cancelar la inscripción"
        )

    /**
     * 2xx → éxito y actualiza el conjunto local; 401 → limpia la sesión;
     * cualquier otro error → ApiException con el mensaje del servidor.
     */
    private suspend fun changeAttendance(
        call: suspend () -> Response<EventDto>,
        onSuccess: () -> Unit,
        fallback: String
    ): Result<Unit> = apiCall {
        val response = call()
        when {
            response.isSuccessful -> {
                onSuccess()
                Result.success(Unit)
            }

            response.code() == 401 -> {
                sessionDataStore.clearToken()
                Result.failure(ApiException(401, SESSION_EXPIRED))
            }

            else -> Result.failure(response.toApiException(fallback))
        }
    }

    override suspend fun getMyAttendances(): Result<List<Attendance>> = apiCall {
        val response = api.myAttendances()
        if (response.isSuccessful) {
            val attendances = response.body().orEmpty().map { it.toAttendance() }
            myEventIds.value = attendances.map { it.eventId }.toSet()
            Result.success(attendances)
        } else {
            if (response.code() == 401) {
                sessionDataStore.clearToken()
            }
            Result.failure(response.toApiException("No se pudieron cargar tus inscripciones"))
        }
    }

    override fun observeMyEventIds(): Flow<Set<String>> = myEventIds.asStateFlow()

    private companion object {
        const val SESSION_EXPIRED = "Sesión expirada. Inicia sesión nuevamente."
    }
}

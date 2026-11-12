package com.example.eventia.domain.repository

import com.example.eventia.domain.model.Attendance
import kotlinx.coroutines.flow.Flow

/**
 * RF-12: inscripciones a eventos.
 * Toda operación protegida confirma con el servidor antes de considerarse exitosa;
 * ante un 401 se limpia la sesión para que la UI redirija al login.
 */
interface AttendanceRepository {
    suspend fun attend(eventId: String): Result<Unit>
    suspend fun cancelAttendance(eventId: String): Result<Unit>
    suspend fun getMyAttendances(): Result<List<Attendance>>
    fun observeMyEventIds(): Flow<Set<String>>
}

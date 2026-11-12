package com.example.eventia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Attendance
import com.example.eventia.domain.model.User
import com.example.eventia.domain.repository.AttendanceRepository
import com.example.eventia.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val upcoming: List<Attendance> = emptyList(),
    val past: List<Attendance> = emptyList(),
    val errorMessage: String? = null
)

/**
 * RF-11 / RF-12: perfil autenticado con inscripciones próximas y pasadas,
 * cierre de sesión y restauración de sesión al abrir la app (DataStore).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        // Restaura la sesión: si existe token en DataStore, trae perfil e inscripciones
        viewModelScope.launch {
            authRepository.observeIsLoggedIn().collect { loggedIn ->
                if (loggedIn) {
                    loadUserData()
                } else {
                    _uiState.value = ProfileUiState(isLoading = false, isLoggedIn = false)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { loadUserData() }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private suspend fun loadUserData() {
        _uiState.value = _uiState.value.copy(isLoading = true, isLoggedIn = true, errorMessage = null)

        val meResult = authRepository.me()
        val attendancesResult = attendanceRepository.getMyAttendances()

        val user = meResult.getOrNull() ?: _uiState.value.user
        val attendances = attendancesResult.getOrDefault(emptyList())
        val (past, upcoming) = splitByDate(attendances)

        val error = listOfNotNull(
            meResult.exceptionOrNull()?.message,
            attendancesResult.exceptionOrNull()?.message
        ).joinToString("\n").ifBlank { null }

        // Si durante la carga el token fue invalidado (401), no restaurar la sesión
        if (!authRepository.isLoggedIn()) {
            _uiState.value = ProfileUiState(isLoading = false, isLoggedIn = false)
            return
        }

        _uiState.value = ProfileUiState(
            isLoading = false,
            isLoggedIn = true,
            user = user,
            upcoming = upcoming,
            past = past,
            errorMessage = error
        )
    }

    /** Clasifica por fecha local; si la fecha no se interpreta, queda en "próximas". */
    private fun splitByDate(list: List<Attendance>): Pair<List<Attendance>, List<Attendance>> {
        val now = System.currentTimeMillis()
        val past = mutableListOf<Attendance>()
        val upcoming = mutableListOf<Attendance>()
        list.forEach { attendance ->
            if (isPast(attendance, now)) past.add(attendance) else upcoming.add(attendance)
        }
        return past to upcoming
    }

    private fun isPast(attendance: Attendance, now: Long): Boolean {
        if (attendance.status.equals("past", ignoreCase = true)) return true
        val raw = attendance.event?.date ?: return false
        val millis = parseDateMillis(raw) ?: return false
        return millis < now
    }

    private fun parseDateMillis(raw: String): Long? {
        val cleaned = normalizeDate(raw)
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy"
        )
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.getDefault())
                format.isLenient = false
                format.parse(cleaned)?.let { return it.time }
            } catch (_: Exception) {
                // probar el siguiente patrón
            }
        }
        return null
    }

    /** Quita la zona horaria ("Z" / "+05:00") para poder usar SimpleDateFormat. */
    private fun normalizeDate(raw: String): String {
        val value = raw.trim()
        val tIndex = value.indexOf('T')
        if (tIndex <= 0) return value
        val rest = value.substring(tIndex + 1)
        val cut = listOf(rest.indexOf('Z'), rest.indexOf('+'))
            .filter { it >= 0 }
            .minOrNull()
            ?: return value
        return value.substring(0, tIndex + 1 + cut)
    }
}

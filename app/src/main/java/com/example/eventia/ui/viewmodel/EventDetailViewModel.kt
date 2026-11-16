package com.example.eventia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.data.remote.ApiException
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.AttendanceRepository
import com.example.eventia.domain.repository.AuthRepository
import com.example.eventia.domain.repository.EventsRepository
import com.example.eventia.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isAttending: Boolean = false
)

data class DetailMessage(val id: Long, val text: String)

/**
 * Detalle de evento (RF-01/02): carga desde API con respaldo en Room,
 * favoritos solo con Room (RF-04) e inscripción confirmada por el
 * servidor (RF-11/RF-12) con manejo de 401 → pedir login sin cerrar la app.
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private val _message = MutableStateFlow<DetailMessage?>(null)
    val message: StateFlow<DetailMessage?> = _message

    private val _requiresLogin = MutableStateFlow(false)
    val requiresLogin: StateFlow<Boolean> = _requiresLogin

    private var favoriteJob: Job? = null
    private var sessionJob: Job? = null
    private var currentEventId: String? = null

    fun load(eventId: String) {
        currentEventId = eventId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repository.getEvent(eventId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                event = result.getOrNull() ?: _uiState.value.event,
                error = result.exceptionOrNull()?.message ?: "Error al cargar"
            )
        }

        // RF-04: estado del corazón (solo Room)
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            favoritesRepository.observeFavoriteIds().collect { ids ->
                _isFavorite.value = eventId in ids
            }
        }

        // RF-11/RF-12: sesión + estado de inscripción; reacciona a login/logout/401
        if (sessionJob?.isActive != true) {
            sessionJob = viewModelScope.launch {
                authRepository.observeIsLoggedIn().collect { loggedIn ->
                    _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                    if (loggedIn) {
                        refreshAttendances()
                    } else {
                        _uiState.value = _uiState.value.copy(isAttending = false)
                    }
                }
            }
        }
    }

    private suspend fun refreshAttendances() {
        val eventId = currentEventId ?: return
        val result = attendanceRepository.getMyAttendances()
        val attending = result.getOrNull()?.any { it.eventId == eventId } == true
        _uiState.value = _uiState.value.copy(isAttending = attending)
    }

    /** Refresca el detalle sin bloquear la pantalla (p. ej. tras un conflicto 409). */
    private fun refreshEvent() {
        val eventId = currentEventId ?: return
        viewModelScope.launch {
            repository.getEvent(eventId).getOrNull()?.let { fresh ->
                _uiState.value = _uiState.value.copy(event = fresh, error = null)
            }
        }
    }

    /** RF-12: inscribirse; solo se confirma con respuesta exitosa del servidor. */
    fun attend(event: Event) {
        viewModelScope.launch {
            val result = attendanceRepository.attend(event.id)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isAttending = true,
                        event = event.copy(
                            availableSpots = event.availableSpots?.let { (it - 1).coerceAtLeast(0) }
                        )
                    )
                    _message.value = DetailMessage(System.nanoTime(), "Inscripción confirmada")
                },
                onFailure = { handleAttendanceError(it, fallback = "No se pudo completar la inscripción") }
            )
        }
    }

    /** RF-12: cancelar inscripción. */
    fun cancelAttendance(event: Event) {
        viewModelScope.launch {
            val result = attendanceRepository.cancelAttendance(event.id)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isAttending = false,
                        event = event.copy(
                            availableSpots = event.availableSpots?.let { it + 1 }
                        )
                    )
                    _message.value = DetailMessage(System.nanoTime(), "Inscripción cancelada")
                },
                onFailure = { handleAttendanceError(it, fallback = "No se pudo cancelar la inscripción") }
            )
        }
    }

    private fun handleAttendanceError(error: Throwable, fallback: String) {
        when {
            error is ApiException && error.code == 401 -> {
                // Sesión inválida/expirada: el repositorio ya limpió el token;
                // se pide login sin cerrar la app (regla 50)
                _requiresLogin.value = true
                _message.value = DetailMessage(
                    System.nanoTime(),
                    error.message.ifBlank { "Sesión expirada. Inicia sesión nuevamente." }
                )
            }

            error is ApiException && error.code == 409 -> {
                _message.value = DetailMessage(
                    System.nanoTime(),
                    error.message.ifBlank { "Conflicto con tu inscripción (409)." }
                )
                // API 8.3: informar al usuario y refrescar el detalle
                refreshEvent()
            }

            error is ApiException -> {
                _message.value = DetailMessage(
                    System.nanoTime(),
                    if (error.code in 400..499) error.message
                    else "$fallback. Verifica tu conexión e inténtalo de nuevo."
                )
            }

            else -> {
                _message.value = DetailMessage(System.nanoTime(), "$fallback. Sin conexión con el servidor.")
            }
        }
    }

    fun consumeRequiresLogin() {
        _requiresLogin.value = false
    }

    /** RF-04: agregar/quitar favorito solo con Room, sin consultar la API. */
    fun toggleFavorite(event: Event) {
        viewModelScope.launch {
            if (_isFavorite.value) {
                favoritesRepository.removeFavorite(event.id)
                _message.value = DetailMessage(System.nanoTime(), "Quitado de favoritos")
            } else {
                favoritesRepository.addFavorite(event)
                _message.value = DetailMessage(System.nanoTime(), "Agregado a favoritos")
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

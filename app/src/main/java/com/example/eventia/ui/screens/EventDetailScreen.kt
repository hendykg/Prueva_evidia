package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
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

        // RF-11: estado del corazón (solo Room)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onVenueClick: ((String) -> Unit)? = null,
    onRequireLogin: (() -> Unit)? = null,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val message by viewModel.message.collectAsState()
    val requiresLogin by viewModel.requiresLogin.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.text, withDismissAction = true)
        viewModel.consumeMessage()
    }

    // RF-50/55: redirigir al login sin cerrar la aplicación
    LaunchedEffect(requiresLogin) {
        if (requiresLogin) {
            viewModel.consumeRequiresLogin()
            onRequireLogin?.invoke()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.title ?: "Detalle del evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.event != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(state.event!!) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }

            state.error != null && state.event == null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }

            state.event != null -> {
                val event = state.event!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = event.imageUrl ?: "https://via.placeholder.com/800x400.png?text=Eventia",
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(event.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    event.categoryName?.let { Text("Categoría: $it", style = MaterialTheme.typography.bodyMedium) }
                    event.date?.let { Text("Fecha: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("Lugar: ${event.place ?: event.venueName ?: "No disponible"}", style = MaterialTheme.typography.bodyMedium)
                    val venueId = event.venueId
                    if (venueId != null && onVenueClick != null) {
                        TextButton(onClick = { onVenueClick(venueId) }) {
                            Text("Ver lugar →")
                        }
                    }
                    val cupos = event.availableSpots ?: event.capacity
                    if (cupos != null) {
                        Text("Cupos: $cupos", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // RF-11/RF-12: inscripción con sesión; el botón y los cupos
                    // se actualizan tras confirmar el servidor
                    if (state.isAttending) {
                        OutlinedButton(
                            onClick = { viewModel.cancelAttendance(event) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar inscripción", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (state.isLoggedIn) viewModel.attend(event)
                                else onRequireLogin?.invoke()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isLoggedIn) "Inscribirme" else "Inscribirme (inicia sesión)")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Descripción", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.description ?: event.summary ?: "Sin descripción disponible.")
                }
            }
        }
    }
}

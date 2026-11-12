package com.example.eventia.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MapUiState(
    /** Solo eventos con coordenadas válidas (RF-07). */
    val events: List<Event> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = true,
    /** Total de eventos cacheados, para distinguir "sin datos" de "sin coordenadas". */
    val totalEvents: Int = 0
)

/**
 * RF-07 / RF-08: marcadores del mapa solo con coordenadas válidas
 * y filtro por categoría combinable con la misma lógica del catálogo.
 * Los datos vienen de Room (funciona sin Internet).
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryFlow = savedStateHandle.getStateFlow<String?>(CATEGORY_KEY, null)

    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    val uiState: StateFlow<MapUiState> = combine(
        repository.observeEvents(),
        repository.observeCategories(),
        categoryFlow
    ) { events, categories, categoryId ->
        val withValidCoordinates = events.filter { it.hasValidCoordinates() }
        val filtered = if (categoryId.isNullOrBlank()) {
            withValidCoordinates
        } else {
            withValidCoordinates.filter { it.categoryId == categoryId }
        }
        MapUiState(
            events = filtered,
            categories = categories,
            selectedCategoryId = categoryId,
            isLoading = false,
            totalEvents = events.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun onCategorySelected(categoryId: String?) {
        savedStateHandle[CATEGORY_KEY] = categoryId
        // El evento seleccionado podría quedar fuera del filtro
        _selectedEvent.value = null
    }

    fun selectEvent(event: Event) {
        _selectedEvent.value = event
    }

    fun clearSelection() {
        _selectedEvent.value = null
    }

    companion object {
        private const val CATEGORY_KEY = "map_category"
    }
}

/**
 * RF-07: coordenada válida = dentro de rangos geográficos.
 * (0.0, 0.0) se trata como inválida porque suele ser un dato ausente ("null island").
 */
fun Event.hasValidCoordinates(): Boolean {
    val lat = latitude ?: return false
    val lng = longitude ?: return false
    if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
    if (lat == 0.0 && lng == 0.0) return false
    return true
}

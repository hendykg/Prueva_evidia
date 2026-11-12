package com.example.eventia.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.domain.repository.VenuesRepository
import com.example.eventia.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VenueDetailUiState(
    val isLoading: Boolean = true,
    val venue: Venue? = null,
    val events: List<Event> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class VenueDetailViewModel @Inject constructor(
    private val repository: VenuesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val venueId: String = savedStateHandle[Routes.VENUE_ID_ARG] ?: ""

    private val _uiState = MutableStateFlow(VenueDetailUiState())
    val uiState: StateFlow<VenueDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = VenueDetailUiState(isLoading = true)

            val venueResult = repository.getVenue(venueId)
            venueResult.onFailure { e ->
                _uiState.value = VenueDetailUiState(
                    isLoading = false,
                    error = e.message ?: "No se pudo cargar el lugar"
                )
                return@launch
            }
            val venue = venueResult.getOrNull()

            val eventsResult = repository.getVenueEvents(venueId)
            _uiState.value = VenueDetailUiState(
                isLoading = false,
                venue = venue,
                events = eventsResult.getOrDefault(emptyList()),
                error = eventsResult.exceptionOrNull()?.message
                    ?.let { "Eventos no disponibles: $it" }
            )
        }
    }
}

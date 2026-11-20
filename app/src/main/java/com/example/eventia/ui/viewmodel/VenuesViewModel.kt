package com.example.eventia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Venue
import com.example.eventia.domain.repository.VenuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VenuesUiState(
    val venues: List<Venue> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Lugares (RF-13): lista observable desde Room con actualización manual
 * (RF-06) y errores compartidos con el catálogo vía [ListStatus].
 */
@HiltViewModel
class VenuesViewModel @Inject constructor(
    private val repository: VenuesRepository
) : ViewModel() {

    private val status = ListStatus()

    val uiState: StateFlow<VenuesUiState> = combine(
        repository.observeVenues(),
        status.isLoading,
        status.isRefreshing,
        status.error
    ) { venues, isLoading, isRefreshing, errorMessage ->
        VenuesUiState(
            venues = venues,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VenuesUiState())

    init {
        loadInitial()
    }

    fun consumeError() {
        status.consumeError()
    }

    fun loadInitial() {
        viewModelScope.launch { status.loadInitial { repository.refreshVenues() } }
    }

    fun refresh() {
        viewModelScope.launch { status.refresh { repository.refreshVenues() } }
    }
}

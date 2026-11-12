package com.example.eventia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Venue
import com.example.eventia.domain.repository.VenuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

@HiltViewModel
class VenuesViewModel @Inject constructor(
    private val repository: VenuesRepository
) : ViewModel() {

    private val loadingFlow = MutableStateFlow(false)
    private val refreshingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VenuesUiState> = combine(
        repository.observeVenues(),
        loadingFlow,
        refreshingFlow,
        errorFlow
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
        errorFlow.value = null
    }

    fun loadInitial() {
        viewModelScope.launch {
            loadingFlow.value = true
            val result = repository.refreshVenues()
            loadingFlow.value = false
            result.onFailure {
                errorFlow.value = "No se pudo actualizar. Mostrando lugares disponibles."
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshingFlow.value = true
            val result = repository.refreshVenues()
            refreshingFlow.value = false
            result.onFailure {
                errorFlow.value = "No se pudo actualizar. Se conservan los datos actuales."
            }
        }
    }
}

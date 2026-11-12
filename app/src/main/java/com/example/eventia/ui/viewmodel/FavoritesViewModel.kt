package com.example.eventia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SnackbarMessage(val id: Long, val text: String)

data class FavoritesUiState(
    val favorites: List<Event> = emptyList(),
    val message: SnackbarMessage? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FavoritesRepository
) : ViewModel() {

    private val messageFlow = MutableStateFlow<SnackbarMessage?>(null)
    private var lastRemoved: Event? = null

    val uiState: StateFlow<FavoritesUiState> = kotlinx.coroutines.flow.combine(
        repository.observeFavorites(),
        messageFlow
    ) { favorites, message ->
        FavoritesUiState(favorites = favorites, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    /** RF-04: quitar favorito (solo Room, funciona sin Internet). */
    fun remove(event: Event) {
        viewModelScope.launch {
            repository.removeFavorite(event.id)
            lastRemoved = event
            messageFlow.value = SnackbarMessage(System.nanoTime(), "Favorito eliminado")
        }
    }

    /** RF-04: opción Deshacer tras eliminar. */
    fun undoRemove() {
        viewModelScope.launch {
            lastRemoved?.let { repository.addFavorite(it) }
            lastRemoved = null
            messageFlow.value = null
        }
    }

    fun consumeMessage() {
        messageFlow.value = null
    }
}

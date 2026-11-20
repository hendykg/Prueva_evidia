package com.example.eventia.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.data.NetworkMonitor
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncMillis: Long? = null,
    val isConnected: Boolean = true
)

private data class CatalogData(
    val events: List<Event>,
    val categories: List<Category>
)

private data class StatusData(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val errorMessage: String?,
    val lastSyncMillis: Long?,
    val isConnected: Boolean
)

@OptIn(FlowPreview::class)
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val networkMonitor: NetworkMonitor,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val queryFlow = savedStateHandle.getStateFlow(QUERY_KEY, "")
    private val categoryFlow = savedStateHandle.getStateFlow<String?>(CATEGORY_KEY, null)
    private val lastSyncFlow = MutableStateFlow<Long?>(null)
    // Estado de carga/error compartido con VenuesViewModel (RF-06)
    private val status = ListStatus()

    // RF-03: debounce de 300 ms sobre la búsqueda por nombre
    private val debouncedQuery = queryFlow.debounce(300).distinctUntilChanged()
    private val filterFlow = combine(debouncedQuery, categoryFlow) { q, c -> q to c }

    // RF-01/RF-03: eventos cacheados en Room + filtro combinado (búsqueda + categoría)
    private val filteredEvents: StateFlow<List<Event>> = combine(
        repository.observeEvents(),
        filterFlow
    ) { events, (q, categoryId) ->
        events.filter { event ->
            val matchesQuery = q.isBlank() || event.title.contains(q, ignoreCase = true)
            val matchesCategory = categoryId.isNullOrBlank() || event.categoryId == categoryId
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val catalogData: StateFlow<CatalogData> = combine(
        filteredEvents,
        repository.observeCategories()
    ) { events, categories -> CatalogData(events, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogData(emptyList(), emptyList()))

    private val statusData: StateFlow<StatusData> = combine(
        status.isLoading,
        status.isRefreshing,
        status.error,
        lastSyncFlow,
        networkMonitor.isConnected
    ) { isLoading, isRefreshing, errorMessage, lastSync, isConnected ->
        StatusData(isLoading, isRefreshing, errorMessage, lastSync, isConnected)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatusData(false, false, null, null, true)
    )

    val uiState: StateFlow<EventsUiState> = combine(
        catalogData,
        queryFlow,
        categoryFlow,
        statusData
    ) { catalog, query, categoryId, status ->
        EventsUiState(
            events = catalog.events,
            categories = catalog.categories,
            query = query,
            selectedCategoryId = categoryId,
            isLoading = status.isLoading,
            isRefreshing = status.isRefreshing,
            errorMessage = status.errorMessage,
            lastSyncMillis = status.lastSyncMillis,
            isConnected = status.isConnected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    init {
        viewModelScope.launch {
            repository.refreshCategories()
        }
        loadInitial()
    }

    fun onQueryChange(value: String) {
        savedStateHandle[QUERY_KEY] = value
    }

    fun onCategorySelected(categoryId: String?) {
        savedStateHandle[CATEGORY_KEY] = categoryId
    }

    fun clearFilters() {
        savedStateHandle[QUERY_KEY] = ""
        savedStateHandle[CATEGORY_KEY] = null
    }

    fun consumeError() {
        status.consumeError()
    }

    fun loadInitial() {
        viewModelScope.launch {
            status.loadInitial { repository.refreshEvents() }
            lastSyncFlow.value = repository.getLastSyncMillis()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            status.refresh { repository.refreshEvents() }
            lastSyncFlow.value = repository.getLastSyncMillis()
        }
    }

    companion object {
        private const val QUERY_KEY = "events_query"
        private const val CATEGORY_KEY = "events_category"
    }
}

package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventia.ui.components.EventCard
import com.example.eventia.ui.viewmodel.EventsViewModel
import java.util.concurrent.TimeUnit

/**
 * Segmento "Eventos" de [ExploreScreen]: catálogo con búsqueda (debounce de
 * 300 ms, RF-03), filtro por categoría combinable, actualización manual
 * (RF-06), estados de carga/vacío/error y aviso de caché (RF-14).
 * El estado de lista lo aporta ExploreScreen para conservar la posición (RF-02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsContent(
    onEventClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar por nombre") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categorías", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = viewModel::clearFilters) { Text("Limpiar filtros") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text("Todas") }
                )
            }
            items(state.categories, key = { it.id }) { category ->
                FilterChip(
                    selected = state.selectedCategoryId == category.id,
                    onClick = { viewModel.onCategorySelected(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RF-14: aviso de datos locales + antigüedad real desde la última sincronización
            val lastSync = state.lastSyncMillis
            when {
                !state.isConnected && lastSync != null -> Text(
                    text = "Sin conexión · datos de hace ${formatAge(System.currentTimeMillis() - lastSync)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                !state.isConnected -> Text(
                    text = "Sin conexión · sin datos locales",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                lastSync != null -> Text(
                    text = "Última actualización hace ${formatAge(System.currentTimeMillis() - lastSync)}",
                    style = MaterialTheme.typography.bodySmall
                )

                else -> Spacer(modifier = Modifier.height(0.dp))
            }
            Button(onClick = viewModel::refresh, enabled = !state.isRefreshing) {
                Text(if (state.isRefreshing) "Actualizando..." else "Actualizar")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.events.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.events.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No hay eventos disponibles.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = viewModel::loadInitial) { Text("Reintentar") }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventCard(event = event, onClick = { onEventClick(event.id) })
                    }
                }
            }
        }
    }
}

private fun formatAge(deltaMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
    val hours = TimeUnit.MILLISECONDS.toHours(deltaMs)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMs)
    return when {
        minutes < 1 -> "hace menos de 1 min"
        minutes < 60 -> "hace $minutes min"
        hours < 24 -> "hace $hours h"
        else -> "hace $days d"
    }
}

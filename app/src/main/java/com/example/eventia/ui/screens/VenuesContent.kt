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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventia.ui.components.VenueCard
import com.example.eventia.ui.viewmodel.VenuesViewModel

/**
 * Segmento "Lugares" de [ExploreScreen] (RF-13): lista con nombre, dirección,
 * imagen y eventos próximos; estados de carga/vacío/error y actualización manual.
 */
@Composable
fun VenuesContent(
    onVenueClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    viewModel: VenuesViewModel = hiltViewModel()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Todos los lugares", style = MaterialTheme.typography.titleSmall)
            Button(onClick = viewModel::refresh, enabled = !state.isRefreshing) {
                Text(if (state.isRefreshing) "Actualizando..." else "Actualizar")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.venues.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.venues.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No hay lugares disponibles.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = viewModel::loadInitial) { Text("Reintentar") }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.venues, key = { it.id }) { venue ->
                        VenueCard(
                            venue = venue,
                            onClick = { onVenueClick(venue.id) }
                        )
                    }
                }
            }
        }
    }
}

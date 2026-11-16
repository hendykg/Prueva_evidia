package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventia.ui.components.EventCard
import com.example.eventia.ui.util.NO_MAPS_APP_MESSAGE
import com.example.eventia.ui.util.openDirections
import com.example.eventia.ui.viewmodel.VenueDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: VenueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.venue?.name ?: "Detalle del lugar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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

            state.error != null && state.venue == null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }

            state.venue != null -> {
                val venue = state.venue!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // Datos del lugar
                        venue.address?.let {
                            Text("Dirección: $it", style = MaterialTheme.typography.bodyMedium)
                        }
                        venue.schedule?.let {
                            Text("Horario: $it", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (venue.description != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(venue.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        // RF-09 / RF-13: indicaciones hacia el lugar con app externa de mapas
                        val venueLat = venue.latitude
                        val venueLng = venue.longitude
                        if (venueLat != null && venueLng != null) {
                            TextButton(
                                onClick = {
                                    openDirections(
                                        context = context,
                                        latitude = venueLat,
                                        longitude = venueLng,
                                        label = venue.name,
                                        onNoApp = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(NO_MAPS_APP_MESSAGE)
                                            }
                                        }
                                    )
                                }
                            ) { Text("Indicaciones →") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Próximos eventos", style = MaterialTheme.typography.titleMedium)
                        state.error?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (state.events.isEmpty()) {
                        item {
                            Text("Este lugar aún no tiene eventos próximos.")
                        }
                    } else {
                        // Reutilización de EventCard (RF-13)
                        items(state.events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                onClick = { onEventClick(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

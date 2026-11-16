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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.util.NO_MAPS_APP_MESSAGE
import com.example.eventia.ui.util.openDirections
import com.example.eventia.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onEventClick: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Cámara: primer marcador disponible o posición por defecto (Buenos Aires)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 3f)
    }
    val firstMarker = state.events
        .firstOrNull()
        ?.takeIf { it.latitude != null && it.longitude != null }
    LaunchedEffect(firstMarker?.id) {
        firstMarker?.let { event ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(event.latitude!!, event.longitude!!),
                if (state.events.size > 1) 6f else 12f
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Eventia · Mapa") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // RF-08: filtro por categoría, igual que el catálogo
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // RF-07: solo se agregan marcadores con coordenadas válidas
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    state.events.forEach { event ->
                        val lat = event.latitude ?: return@forEach
                        val lng = event.longitude ?: return@forEach
                        Marker(
                            state = rememberMarkerState(
                                key = event.id,
                                position = LatLng(lat, lng)
                            ),
                            title = event.title,
                            onClick = {
                                viewModel.selectEvent(event)
                                true
                            }
                        )
                    }
                }

                if (!state.isLoading && state.events.isEmpty()) {
                    Text(
                        text = when {
                            state.totalEvents == 0 -> "No hay eventos cargados. Actualiza el catálogo."
                            else -> "Ningún evento tiene coordenadas válidas para mostrar en el mapa."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                // RF-09: resumen del evento al seleccionar un marcador
                selectedEvent?.let { event ->
                    EventSummaryCard(
                        event = event,
                        onOpenDetail = { onEventClick(event.id) },
                        onDirections = {
                            val lat = event.latitude
                            val lng = event.longitude
                            if (lat != null && lng != null) {
                                openDirections(
                                    context = context,
                                    latitude = lat,
                                    longitude = lng,
                                    label = event.title,
                                    onNoApp = {
                                        // RF-09: ausencia de app de mapas compatible
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(NO_MAPS_APP_MESSAGE)
                                        }
                                    }
                                )
                            }
                        },
                        onDismiss = viewModel::clearSelection,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSummaryCard(
    event: Event,
    onOpenDetail: () -> Unit,
    onDirections: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar resumen")
                }
            }
            event.date?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = listOfNotNull(event.place ?: event.venueName, event.categoryName)
                    .joinToString(" • "),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenDetail) { Text("Ver detalle") }
                TextButton(onClick = onDirections) { Text("Indicaciones") }
            }
        }
    }
}

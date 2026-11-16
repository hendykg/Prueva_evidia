package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Destino "Explorar" de la barra inferior (Figura 1 del enunciado):
 * un solo destino con dos segmentos, Eventos (RF-01/03/06) y Lugares (RF-13).
 * La barra inferior tiene exactamente cuatro destinos: Explorar, Mapa,
 * Favoritos y Perfil (sección 5 del enunciado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onEventClick: (String) -> Unit,
    onVenueClick: (String) -> Unit
) {
    var showVenues by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    // RF-02: cada lista conserva su posición al rotar, cambiar de segmento o volver del detalle
    val eventsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val venuesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showVenues) "Eventia · Lugares" else "Eventia · Eventos") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = !showVenues,
                    onClick = { showVenues = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Eventos") }
                SegmentedButton(
                    selected = showVenues,
                    onClick = { showVenues = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Lugares") }
            }

            if (showVenues) {
                VenuesContent(
                    onVenueClick = onVenueClick,
                    snackbarHostState = snackbarHostState,
                    listState = venuesListState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                EventsContent(
                    onEventClick = onEventClick,
                    snackbarHostState = snackbarHostState,
                    listState = eventsListState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.eventia.ui.util.NO_MAPS_APP_MESSAGE
import com.example.eventia.ui.util.openDirections
import com.example.eventia.ui.viewmodel.EventDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onVenueClick: ((String) -> Unit)? = null,
    onRequireLogin: (() -> Unit)? = null,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val message by viewModel.message.collectAsState()
    val requiresLogin by viewModel.requiresLogin.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.text, withDismissAction = true)
        viewModel.consumeMessage()
    }

    // RF-11: redirigir al login sin cerrar la aplicación
    LaunchedEffect(requiresLogin) {
        if (requiresLogin) {
            viewModel.consumeRequiresLogin()
            onRequireLogin?.invoke()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.title ?: "Detalle del evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.event != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(state.event!!) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
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

            state.error != null && state.event == null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }

            state.event != null -> {
                val event = state.event!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = event.imageUrl ?: "https://via.placeholder.com/800x400.png?text=Eventia",
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(event.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    event.categoryName?.let { Text("Categoría: $it", style = MaterialTheme.typography.bodyMedium) }
                    event.date?.let { Text("Fecha: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("Lugar: ${event.place ?: event.venueName ?: "No disponible"}", style = MaterialTheme.typography.bodyMedium)
                    // RF-09: indicaciones hacia la ubicación del evento con app externa
                    val eventLat = event.latitude
                    val eventLng = event.longitude
                    if (eventLat != null && eventLng != null) {
                        TextButton(
                            onClick = {
                                openDirections(
                                    context = context,
                                    latitude = eventLat,
                                    longitude = eventLng,
                                    label = event.title,
                                    onNoApp = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(NO_MAPS_APP_MESSAGE)
                                        }
                                    }
                                )
                            }
                        ) { Text("Indicaciones →") }
                    }
                    val venueId = event.venueId
                    if (venueId != null && onVenueClick != null) {
                        TextButton(onClick = { onVenueClick(venueId) }) {
                            Text("Ver lugar →")
                        }
                    }
                    val cupos = event.availableSpots ?: event.capacity
                    if (cupos != null) {
                        Text("Cupos: $cupos", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // RF-11/RF-12: inscripción con sesión; el botón y los cupos
                    // se actualizan tras confirmar el servidor
                    if (state.isAttending) {
                        OutlinedButton(
                            onClick = { viewModel.cancelAttendance(event) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar inscripción", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (state.isLoggedIn) viewModel.attend(event)
                                else onRequireLogin?.invoke()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isLoggedIn) "Inscribirme" else "Inscribirme (inicia sesión)")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Descripción", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.description ?: event.summary ?: "Sin descripción disponible.")
                }
            }
        }
    }
}

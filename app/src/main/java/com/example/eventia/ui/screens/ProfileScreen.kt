package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventia.ui.components.EventCard
import com.example.eventia.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Eventia · Perfil") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                !state.isLoggedIn -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Explora sin cuenta y cuando quieras inscribirte, inicia sesión.")
                        Text(
                            text = "Tus inscripciones y perfil se guardan en tu cuenta.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = onLogin) { Text("Iniciar sesión") }
                    }
                }

                else -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "header") {
                            Column {
                                Text(
                                    text = state.user?.name?.takeIf { it.isNotBlank() } ?: "Tu cuenta",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                state.user?.email?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                                state.errorMessage?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = viewModel::logout,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cerrar sesión")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Próximas inscripciones", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        if (state.upcoming.isEmpty()) {
                            item(key = "upcoming-empty") {
                                Text(
                                    "No tienes inscripciones próximas.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            items(state.upcoming, key = { "up-${it.id}" }) { attendance ->
                                attendance.event?.let { event ->
                                    EventCard(event = event, onClick = { onEventClick(event.id) })
                                } ?: Text(attendance.eventId, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        item(key = "past-title") {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Inscripciones pasadas", style = MaterialTheme.typography.titleMedium)
                        }

                        if (state.past.isEmpty()) {
                            item(key = "past-empty") {
                                Text(
                                    "Aún no tienes inscripciones pasadas.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            items(state.past, key = { "past-${it.id}" }) { attendance ->
                                attendance.event?.let { event ->
                                    EventCard(event = event, onClick = { onEventClick(event.id) })
                                } ?: Text(attendance.eventId, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

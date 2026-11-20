package com.example.eventia.ui.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Muestra [message] una sola vez en la snack bar y avisa a [onConsumed]
 * para que el ViewModel la limpie. Compartido por Eventos y Lugares en
 * lugar de repetir el mismo LaunchedEffect en cada pantalla.
 */
@Composable
fun SnackbarEffect(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onConsumed: () -> Unit
) {
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onConsumed()
        }
    }
}

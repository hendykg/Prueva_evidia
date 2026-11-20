package com.example.eventia.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Estados compartidos de toda lista: cargando (spinner), vacía (mensaje +
 * botón "Reintentar") o con contenido. Lo usan Eventos y Lugares para no
 * repetir el mismo bloque en cada pantalla.
 */
@Composable
fun ListStateBox(
    isLoading: Boolean,
    isEmpty: Boolean,
    emptyMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isEmpty && isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            isEmpty -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(emptyMessage)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Reintentar") }
            }

            else -> content()
        }
    }
}

/** Botón "Actualizar" con su estado de carga, igual en todas las listas (RF-06). */
@Composable
fun RefreshButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, enabled = !isRefreshing, modifier = modifier) {
        Text(if (isRefreshing) "Actualizando..." else "Actualizar")
    }
}

package com.example.eventia.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * RF-09: abre indicaciones hacia la ubicación del evento/lugar en una
 * aplicación externa de mapas (intent `geo:lat,lng`).
 * Si ninguna app puede manejarlo, se invoca [onNoApp] para informar al
 * usuario sin cerrar la aplicación.
 */
fun openDirections(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
    onNoApp: () -> Unit
) {
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude,${Uri.encode(label)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        onNoApp()
    }
}

/** Mensaje compartido cuando no hay app de mapas disponible. */
const val NO_MAPS_APP_MESSAGE =
    "No se encontró una aplicación de mapas para mostrar indicaciones."

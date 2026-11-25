package com.example.eventia

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class EventiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // osmdroid: caché de teselas en almacenamiento privado de la app y
        // agente de usuario identificable (lo exigen los servidores de tiles
        // de OpenStreetMap; además bloquean los UA que contienen "example").
        // Sin permisos de almacenamiento ni API key.
        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "Eventia/1.0 (proyecto estudiante PM1)"
        }
    }
}

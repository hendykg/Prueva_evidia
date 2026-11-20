package com.example.eventia.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Estado de carga, actualización y error compartido por los ViewModels de
 * listado (Eventos y Lugares): una sola implementación de "Actualizar"
 * (RF-06) en lugar de duplicar loadInitial/refresh en cada ViewModel.
 */
class ListStatus {
    val isLoading = MutableStateFlow(false)
    val isRefreshing = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    /** Primera carga o reintento desde la lista vacía. */
    suspend fun loadInitial(block: suspend () -> Result<*>) =
        run(isLoading, "No se pudo actualizar. Mostrando datos disponibles.", block)

    /** Actualización manual con la lista ya visible (RF-06). */
    suspend fun refresh(block: suspend () -> Result<*>) =
        run(isRefreshing, "No se pudo actualizar. Se conservan los datos actuales.", block)

    fun consumeError() {
        error.value = null
    }

    private suspend fun run(
        loading: MutableStateFlow<Boolean>,
        failureMessage: String,
        block: suspend () -> Result<*>
    ) {
        loading.value = true
        val result = block()
        loading.value = false
        // RF-06: si falla, se informa pero no se borra la lista visible
        result.onFailure { error.value = failureMessage }
    }
}

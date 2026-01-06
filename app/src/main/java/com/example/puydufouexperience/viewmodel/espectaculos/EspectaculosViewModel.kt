package com.example.puydufouexperience.viewmodel.espectaculos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.EspectaculosRepository
import com.example.puydufouexperience.model.entity.Espectaculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la lista de espectáculos (MVVM light).
 *
 * Responsabilidades:
 * - Cargar la lista desde el Repository (IO).
 * - Exponer estado observable para que el Fragment solo pinte UI.
 *
 * Nota:
 * - No usamos ProgressBar (como pediste).
 * - Pero sí dejamos un error por si quieres mostrar un Toast (opcional).
 */
class EspectaculosViewModel(
    private val repo: EspectaculosRepository
) : ViewModel() {

    // Lista observable de espectáculos
    private val _items = MutableStateFlow<List<Espectaculo>>(emptyList())
    val items: StateFlow<List<Espectaculo>> = _items

    // Error opcional (por si quieres Toast en el Fragment)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Carga la lista completa.
     * Si ya está cargada y no quieres recargar, puedes controlar eso aquí.
     */
    fun cargar() {
        viewModelScope.launch {
            try {
                _items.value = repo.getAll()
                _error.value = null
            } catch (e: Exception) {
                // Nunca crashear por un fallo de BD
                _error.value = e.message ?: "Error cargando espectáculos"
            }
        }
    }

    /**
     * Limpia el error tras consumirlo (opcional).
     */
    fun clearError() {
        _error.value = null
    }
}

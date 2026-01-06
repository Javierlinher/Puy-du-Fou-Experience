package com.example.puydufouexperience.viewmodel.restaurantes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.RestaurantesRepository
import com.example.puydufouexperience.model.entity.Restaurante
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la lista de restaurantes (MVVM light).
 *
 * Responsabilidades:
 * - Cargar la lista desde el Repository.
 * - Exponer estado observable para que el Fragment solo pinte UI.
 *
 * Nota:
 * - No usamos ProgressBar (como pediste).
 * - Dejamos un canal de error opcional por si quieres Toast.
 */
class RestaurantesViewModel(
    private val repo: RestaurantesRepository
) : ViewModel() {

    // Lista observable de restaurantes
    private val _items = MutableStateFlow<List<Restaurante>>(emptyList())
    val items: StateFlow<List<Restaurante>> = _items

    // Error opcional
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Carga la lista completa de restaurantes.
     */
    fun cargar() {
        viewModelScope.launch {
            try {
                _items.value = repo.getAll()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando restaurantes"
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

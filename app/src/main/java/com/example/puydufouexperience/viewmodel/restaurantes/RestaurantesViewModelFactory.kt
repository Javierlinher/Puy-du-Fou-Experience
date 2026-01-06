package com.example.puydufouexperience.viewmodel.restaurantes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.puydufouexperience.data.repository.RestaurantesRepository

/**
 * Factory para crear RestaurantesViewModel sin DI.
 * Mantiene el patrón que ya usas en Ajustes y en Espectáculos.
 */
class RestaurantesViewModelFactory(
    private val repo: RestaurantesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RestaurantesViewModel(repo) as T
    }
}

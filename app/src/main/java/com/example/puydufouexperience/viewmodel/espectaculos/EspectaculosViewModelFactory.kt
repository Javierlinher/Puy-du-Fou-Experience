package com.example.puydufouexperience.viewmodel.espectaculos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.puydufouexperience.data.repository.EspectaculosRepository

/**
 * Factory para crear EspectaculosViewModel sin DI.
 * Mantiene el patrón que ya usas en Ajustes.
 */
class EspectaculosViewModelFactory(
    private val repo: EspectaculosRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EspectaculosViewModel(repo) as T
    }
}

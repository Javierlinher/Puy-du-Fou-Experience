package com.example.puydufouexperience.viewmodel.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.UserRepository
import com.example.puydufouexperience.model.entity.AjustesUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de Ajustes (MVVM):
 * - Mantiene en memoria los AjustesUsuario del usuario actual
 * - Permite cargar/crear ajustes y guardarlos
 *
 * OJO:
 * - No aplica idioma/tema (eso lo haces desde Fragment con AppCompatDelegate, si quieres)
 * - Aquí solo se persiste en BD (Room) vía UserRepository
 */
class AjustesViewModel(
    private val userRepository: UserRepository,
    private val idUsuarioActual: Int
) : ViewModel() {

    private val _ajustes = MutableStateFlow<AjustesUsuario?>(null)
    val ajustes: StateFlow<AjustesUsuario?> = _ajustes

    /**
     * Carga ajustes del usuario actual.
     * Si no existen, los crea con valores por defecto.
     */
    fun cargar() {
        viewModelScope.launch {
            _ajustes.value = userRepository.getOrCreateAjustes(idUsuarioActual)
        }
    }

    /**
     * Guarda ajustes:
     * - Si aún no están cargados, primero los obtiene/crea
     * - Luego hace upsert en BD
     */
    fun guardar(idioma: String, tema: String, notificaciones: Boolean) {
        viewModelScope.launch {
            val actual = _ajustes.value ?: userRepository.getOrCreateAjustes(idUsuarioActual)

            val actualizado = actual.copy(
                idioma = idioma,
                tema = tema,
                notificaciones = notificaciones
            )

            userRepository.updateAjustes(actualizado)
            _ajustes.value = actualizado
        }
    }
}

/**
 * Factory simple para construir AjustesViewModel con parámetros.
 */
class AjustesViewModelFactory(
    private val userRepository: UserRepository,
    private val idUsuarioActual: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AjustesViewModel(userRepository, idUsuarioActual) as T
    }
}

package com.example.puydufouexperience.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.UserRepository
import com.example.puydufouexperience.model.entity.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para administración de usuarios.
 *
 * - Carga lista de usuarios
 * - Borra usuario por id
 * - Refresca lista tras borrar
 *
 * Importante:
 * - El Fragment NO debe hacer coroutines para borrar.
 * - Aquí las hacemos con viewModelScope (MVVM correcto).
 */
class AdminUsuariosViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    val usuarios: StateFlow<List<Usuario>> = _usuarios

    fun cargarUsuarios() {
        viewModelScope.launch {
            _usuarios.value = userRepository.getAllUsers()
        }
    }

    /**
     * Borrar usuario y refrescar lista.
     * Devuelve el resultado mediante callback para que el Fragment muestre Toast.
     */
    fun borrarUsuario(id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = userRepository.deleteUserById(id)
            if (ok) {
                _usuarios.value = userRepository.getAllUsers()
            }
            onResult(ok)
        }
    }
}

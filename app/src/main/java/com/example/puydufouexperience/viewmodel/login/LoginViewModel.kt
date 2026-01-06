package com.example.puydufouexperience.viewmodel.login

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puydufouexperience.data.repository.UserRepository
import com.example.puydufouexperience.model.entity.Usuario
import com.example.puydufouexperience.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoginViewModel:
 * - Espera a que el seed esté listo (usuarios > 0)
 * - Autologin si hay sesión guardada
 * - Login y registro usando UserRepository
 * - Aplica ajustes de idioma/tema del usuario
 * - Marca admin si nombre == "admin"
 */
class LoginViewModel(
    private val repo: UserRepository,
    private val appContext: Context
) : ViewModel() {

    companion object {
        private const val ADMIN_USER = "admin"
    }

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data object Ready : State()
        data class Error(val message: String) : State()
        data class Logged(val usuario: Usuario) : State()
        data class Registered(val usuario: Usuario) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    /**
     * init():
     * - Evita el caso "instalación limpia" donde aún no se han insertado usuarios.
     * - Si hay sesión (id) y existe en BD, entra directo y aplica ajustes.
     */
    fun init() {
        _state.value = State.Loading

        viewModelScope.launch(Dispatchers.IO) {
            // 1) Esperar seed (usuarios > 0)
            while (repo.countUsuarios() <= 0) {
                delay(200)
            }

            // 2) Autologin si hay id guardado
            val id = SessionManager.getIdUsuarioActual(appContext)
            if (id != -1) {
                val usuario = repo.getUsuarioById(id)
                if (usuario != null) {
                    SessionManager.setAdmin(appContext, usuario.nombre == ADMIN_USER)
                    aplicarAjustes(usuario.id)

                    withContext(Dispatchers.Main) {
                        _state.value = State.Logged(usuario)
                    }
                    return@launch
                } else {
                    SessionManager.clearSession(appContext)
                }
            }

            withContext(Dispatchers.Main) {
                _state.value = State.Ready
            }
        }
    }

    fun login(nombre: String, passwordPlain: String) {
        val n = nombre.trim()

        if (n.isBlank() || passwordPlain.isBlank()) {
            _state.value = State.Error("Rellena usuario y contraseña")
            return
        }

        _state.value = State.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val usuario = repo.loginPlain(n, passwordPlain)

            withContext(Dispatchers.Main) {
                if (usuario == null) {
                    _state.value = State.Error("Credenciales incorrectas")
                } else {
                    SessionManager.setIdUsuarioActual(appContext, usuario.id)
                    SessionManager.setAdmin(appContext, usuario.nombre == ADMIN_USER)
                    aplicarAjustes(usuario.id)

                    _state.value = State.Logged(usuario)
                }
            }
        }
    }

    fun register(nombre: String, passwordPlain: String) {
        val n = nombre.trim()

        if (n.isBlank() || passwordPlain.isBlank()) {
            _state.value = State.Error("Rellena usuario y contraseña")
            return
        }

        // Reservado: admin lo mete el seed
        if (n == ADMIN_USER) {
            _state.value = State.Error("Ese nombre está reservado")
            return
        }

        _state.value = State.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val creado = repo.registerUser(n, passwordPlain)

            withContext(Dispatchers.Main) {
                if (creado == null) {
                    _state.value = State.Error("No se pudo crear: nombre en uso o datos inválidos")
                } else {
                    SessionManager.setIdUsuarioActual(appContext, creado.id)
                    SessionManager.setAdmin(appContext, false)
                    aplicarAjustes(creado.id)

                    _state.value = State.Registered(creado)
                }
            }
        }
    }

    private suspend fun aplicarAjustes(idUsuario: Int) {
        val ajustes = repo.getOrCreateAjustes(idUsuario)

        // Idioma
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(ajustes.idioma))

        // Tema
        when (ajustes.tema) {
            "LIGHT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "DARK" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    class Factory(
        private val repo: UserRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(repo, appContext) as T
        }
    }
}

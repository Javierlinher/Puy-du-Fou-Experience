package com.example.puydufouexperience.data.repository

import com.example.puydufouexperience.data.dao.AjustesUsuarioDao
import com.example.puydufouexperience.data.dao.UsuarioDao
import com.example.puydufouexperience.model.entity.AjustesUsuario
import com.example.puydufouexperience.model.entity.Usuario
import com.example.puydufouexperience.utils.HashUtils

/**
 * Repository de usuarios:
 * - Encapsula el acceso a UsuarioDao y AjustesUsuarioDao
 * - Centraliza hashing y helpers para login/registro
 */
class UserRepository(
    private val usuarioDao: UsuarioDao,
    private val ajustesUsuarioDao: AjustesUsuarioDao
) {

    /**
     * Para esperar el seed al arrancar (LoginViewModel.init()).
     */
    suspend fun countUsuarios(): Int = usuarioDao.countUsuarios()

    /**
     * Login normal (API por si ya traes el hash calculado).
     */
    suspend fun login(nombre: String, passwordHash: String): Usuario? {
        return usuarioDao.login(nombre, passwordHash)
    }

    /**
     * Login recomendado desde UI:
     * - recibe password en claro
     * - hashea dentro (así la UI no toca HashUtils)
     */
    suspend fun loginPlain(nombre: String, passwordPlain: String): Usuario? {
        val hash = HashUtils.sha256(passwordPlain)
        return usuarioDao.login(nombre, hash)
    }

    suspend fun getUsuarioById(id: Int): Usuario? {
        return usuarioDao.getById(id)
    }

    /**
     * Obtiene ajustes si existen; si no, los crea por defecto.
     * Esto evita nulls y te garantiza el 1:1 lógico con Usuario.
     */
    suspend fun getOrCreateAjustes(idUsuario: Int): AjustesUsuario {
        val actual = ajustesUsuarioDao.getByUsuario(idUsuario)
        if (actual != null) return actual

        val nuevo = AjustesUsuario(
            idUsuario = idUsuario,
            idioma = "es",
            tema = "SYSTEM",
            notificaciones = true
        )

        ajustesUsuarioDao.upsert(nuevo)
        return nuevo
    }

    suspend fun updateAjustes(ajustes: AjustesUsuario) {
        ajustesUsuarioDao.upsert(ajustes)
    }

    // -------------------------
    // Admin panel helpers (si los usas)
    // -------------------------

    suspend fun getAllUsers(): List<Usuario> = usuarioDao.getAll()

    suspend fun deleteUserById(idUsuario: Int): Boolean {
        return usuarioDao.deleteById(idUsuario) > 0
    }

    /**
     * Registro:
     * - nombre único
     * - contraseña guardada como hash
     * - crea ajustes por defecto
     */
    suspend fun registerUser(nombre: String, passwordPlain: String): Usuario? {
        val n = nombre.trim()
        if (n.isBlank() || passwordPlain.isBlank()) return null
        if (usuarioDao.getByNombre(n) != null) return null

        val hash = HashUtils.sha256(passwordPlain)
        val id = usuarioDao.insert(Usuario(nombre = n, contrasena = hash)).toInt()

        getOrCreateAjustes(id)
        return usuarioDao.getById(id)
    }
}

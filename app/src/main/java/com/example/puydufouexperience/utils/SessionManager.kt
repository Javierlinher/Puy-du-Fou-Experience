package com.example.puydufouexperience.utils

import android.content.Context

/**
 * Gestión mínima de sesión:
 * - idUsuarioActual: SIEMPRE existe si estás logueado (incluido admin, porque admin está en BD por seed).
 * - isAdmin: flag lógico (no BD) que se activa SOLO si has entrado con admin/admin.
 */
object SessionManager {

    private const val PREFS_NAME = "session"
    private const val KEY_ID_USUARIO = "idUsuarioActual"
    private const val KEY_IS_ADMIN = "isAdmin"

    fun getIdUsuarioActual(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_ID_USUARIO, -1)
    }

    fun setIdUsuarioActual(context: Context, idUsuario: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ID_USUARIO, idUsuario)
            .apply()
    }

    fun isAdmin(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ADMIN, false)
    }

    fun setAdmin(context: Context, isAdmin: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ADMIN, isAdmin)
            .apply()
    }

    /**
     * Cierra sesión completa.
     */
    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ID_USUARIO)
            .remove(KEY_IS_ADMIN)
            .apply()
    }
}

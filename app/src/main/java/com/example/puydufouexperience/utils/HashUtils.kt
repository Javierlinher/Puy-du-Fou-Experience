package com.example.puydufouexperience.utils

import java.security.MessageDigest

// Helper para hashear contraseñas con SHA-256.
// - Es simple y suficiente para un proyecto académico.
// - Guardamos el hash en BD, nunca la contraseña en claro.
object HashUtils {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

package com.example.puydufouexperience.ui.espectaculos

/**
 * Modelo UI de una "sesión" de espectáculo (hora concreta).
 */
data class HorarioUi(
    val espectaculoId: Int,
    val hora: String,          // "11:00"
    val titulo: String,        // "El Sueño de Toledo"
    val extra: String          // "45 min · Accesible: Sí" (texto corto)
)

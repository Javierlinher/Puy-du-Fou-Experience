package com.example.puydufouexperience.ui.favoritos

// Modelo de UI para pintar un favorito ya resuelto (con título y tipo).
data class FavoritoUi(
    val tipoElemento: String,  // "ESPECTACULO" / "RESTAURANTE" / "RUTA"
    val idElemento: Int,       // id del elemento en su tabla
    val titulo: String,        // nombre del espectáculo / restaurante / ruta
    val subtitulo: String      // texto auxiliar ("Espectáculo", "Restaurante", etc.)
)

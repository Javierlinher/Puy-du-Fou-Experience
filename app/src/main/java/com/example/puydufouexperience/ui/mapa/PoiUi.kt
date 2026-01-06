package com.example.puydufouexperience.ui.mapa

// POI que pintamos en el mapa (marker). Sirve para saber qué abrir al pulsar.
data class PoiUi(
    val tipo: String,     // PoiUi.TIPO_ESPECTACULO / PoiUi.TIPO_RESTAURANTE
    val id: Int,
    val nombre: String,
    val lat: Double,
    val lng: Double
) {
    companion object {
        const val TIPO_ESPECTACULO = "ESPECTACULO"
        const val TIPO_RESTAURANTE = "RESTAURANTE"
    }
}

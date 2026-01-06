package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla ParadaRuta (polimórfica)
// - tipoElemento + idElemento (o null si PUNTO)
// - orden único por ruta (evita duplicados en la misma ruta)
@Entity(
    tableName = "ParadaRuta",
    indices = [
        Index(value = ["idRuta"]),
        Index(value = ["idRuta", "orden"], unique = true)
    ]
)
data class ParadaRuta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idRuta: Int,
    val orden: Int,
    val tipoElemento: String, // "ESPECTACULO" / "RESTAURANTE" / "PUNTO"
    val idElemento: Int?,     // null si tipoElemento = "PUNTO"
    val latitud: Double,
    val longitud: Double,
    val etiqueta: String
)

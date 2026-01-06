package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla Favorito (polimórfica)
// - Un usuario no debería poder repetir el mismo favorito
@Entity(
    tableName = "Favorito",
    indices = [
        Index(value = ["idUsuario"]),
        Index(value = ["idUsuario", "tipoElemento", "idElemento"], unique = true)
    ]
)
data class Favorito(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUsuario: Int,
    val tipoElemento: String, // "ESPECTACULO" / "RESTAURANTE" / "RUTA"
    val idElemento: Int
)

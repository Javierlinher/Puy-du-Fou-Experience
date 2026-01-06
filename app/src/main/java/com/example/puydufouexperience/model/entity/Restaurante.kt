package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tabla Restaurante
@Entity(tableName = "Restaurante")
data class Restaurante(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val rangoPrecio: String, // "€" / "€€" / "€€€"
    val latitud: Double,
    val longitud: Double,
    val imagen: String
)

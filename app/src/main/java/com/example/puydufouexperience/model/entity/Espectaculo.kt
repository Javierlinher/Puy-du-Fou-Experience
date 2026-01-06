package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tabla Espectaculo
// - horarios: "11:00|13:30|17:00"
@Entity(tableName = "Espectaculo")
data class Espectaculo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val duracionMinutos: Int,
    val horarios: String,
    val latitud: Double,
    val longitud: Double,
    val imagen: String,
    val accesible: Boolean
)

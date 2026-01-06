package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla Ruta
@Entity(
    tableName = "Ruta",
    indices = [Index(value = ["idUsuario"])]
)
data class Ruta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUsuario: Int,
    val nombre: String,
    val fechaCreacion: Long
)

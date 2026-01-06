package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla ReservaRestaurante
@Entity(
    tableName = "ReservaRestaurante",
    indices = [
        Index(value = ["idUsuario"]),
        Index(value = ["idRestaurante"])
    ]
)
data class ReservaRestaurante(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUsuario: Int,
    val idRestaurante: Int,
    val fechaHora: Long,
    val numPersonas: Int
)

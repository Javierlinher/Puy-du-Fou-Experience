package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla AjustesUsuario (1:1 lógico con Usuario)
// - idUsuario debe ser único (un ajuste por usuario)
// - no hay FOREIGN KEY física, solo integridad lógica
@Entity(
    tableName = "AjustesUsuario",
    indices = [Index(value = ["idUsuario"], unique = true)]
)
data class AjustesUsuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUsuario: Int,
    val idioma: String,          // "es" / "en"
    val tema: String,            // "LIGHT" / "DARK" / "SYSTEM"
    val notificaciones: Boolean  // true/false
)

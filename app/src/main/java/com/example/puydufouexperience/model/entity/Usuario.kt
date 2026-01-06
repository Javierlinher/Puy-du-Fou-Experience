package com.example.puydufouexperience.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla Usuario
 * - nombre único para login
 * - contrasena es un hash (no texto plano)
 */
@Entity(
    tableName = "Usuario",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val contrasena: String
)

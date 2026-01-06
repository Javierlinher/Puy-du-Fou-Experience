package com.example.puydufouexperience.model.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

// Modelo auxiliar para pintar "Mis reservas" con el nombre del restaurante.
// No es una tabla: es el resultado de una query con JOIN.
data class ReservaConRestauranteNombre(
    @Embedded val reserva: ReservaRestaurante,

    @ColumnInfo(name = "restauranteNombre")
    val restauranteNombre: String
)

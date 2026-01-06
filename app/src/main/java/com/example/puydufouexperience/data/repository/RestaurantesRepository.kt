package com.example.puydufouexperience.data.repository

import com.example.puydufouexperience.data.dao.ReservaRestauranteDao
import com.example.puydufouexperience.data.dao.RestauranteDao
import com.example.puydufouexperience.model.entity.ReservaRestaurante
import com.example.puydufouexperience.model.entity.Restaurante

// Repositorio de restaurantes: lectura + reservas.
class RestaurantesRepository(
    private val restauranteDao: RestauranteDao,
    private val reservaDao: ReservaRestauranteDao
) {
    suspend fun getAll(): List<Restaurante> = restauranteDao.getAll()

    suspend fun getById(id: Int): Restaurante? = restauranteDao.getById(id)

    // Crear una reserva (se guarda en Room)
    suspend fun crearReserva(reserva: ReservaRestaurante): Long = reservaDao.insert(reserva)

    // Listar reservas del usuario (opcional para “Mis reservas”)
    suspend fun getReservasByUsuario(idUsuario: Int): List<ReservaRestaurante> = reservaDao.getByUsuario(idUsuario)
}

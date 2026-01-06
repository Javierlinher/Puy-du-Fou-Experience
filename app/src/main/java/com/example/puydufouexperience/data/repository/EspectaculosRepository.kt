package com.example.puydufouexperience.data.repository

import com.example.puydufouexperience.data.dao.EspectaculoDao
import com.example.puydufouexperience.model.entity.Espectaculo

// Repositorio de espectáculos: lectura básica.
class EspectaculosRepository(
    private val espectaculoDao: EspectaculoDao
) {
    suspend fun getAll(): List<Espectaculo> = espectaculoDao.getAll()

    suspend fun getById(id: Int): Espectaculo? = espectaculoDao.getById(id)
}

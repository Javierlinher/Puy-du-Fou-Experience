package com.example.puydufouexperience.data.repository

import com.example.puydufouexperience.data.dao.EspectaculoDao
import com.example.puydufouexperience.data.dao.RestauranteDao
import com.example.puydufouexperience.ui.mapa.PoiUi

// Repositorio de mapa: trae puntos (espectáculos + restaurantes) desde Room.
class MapaRepository(
    private val espectaculoDao: EspectaculoDao,
    private val restauranteDao: RestauranteDao
) {
    suspend fun getPois(): List<PoiUi> {
        val espectaculos = espectaculoDao.getAll().map {
            PoiUi(
                tipo = "ESPECTACULO",
                id = it.id,
                nombre = it.nombre,
                lat = it.latitud,
                lng = it.longitud
            )
        }

        val restaurantes = restauranteDao.getAll().map {
            PoiUi(
                tipo = "RESTAURANTE",
                id = it.id,
                nombre = it.nombre,
                lat = it.latitud,
                lng = it.longitud
            )
        }

        // Mezclamos; si quieres, luego los ordenamos o filtramos.
        return espectaculos + restaurantes
    }
}

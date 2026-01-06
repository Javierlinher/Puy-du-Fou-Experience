package com.example.puydufouexperience.data.repository

import com.example.puydufouexperience.data.dao.FavoritoDao
import com.example.puydufouexperience.model.entity.Favorito

// Repositorio de favoritos: tabla polimórfica (tipoElemento + idElemento).
class FavoritosRepository(
    private val favoritoDao: FavoritoDao
) {
    companion object {
        // Tipos oficiales de tu polimorfismo
        const val TIPO_ESPECTACULO = "ESPECTACULO"
        const val TIPO_RESTAURANTE = "RESTAURANTE"
        const val TIPO_RUTA = "RUTA"
    }

    suspend fun getByUsuario(idUsuario: Int): List<Favorito> =
        favoritoDao.getByUsuario(idUsuario)

    // Añadir favorito (si ya existe, el índice unique lo bloqueará)
    suspend fun add(idUsuario: Int, tipo: String, idElemento: Int): Long {
        return favoritoDao.insert(
            Favorito(
                idUsuario = idUsuario,
                tipoElemento = tipo,
                idElemento = idElemento
            )
        )
    }

    suspend fun remove(idUsuario: Int, tipo: String, idElemento: Int) {
        favoritoDao.deleteOne(idUsuario, tipo, idElemento)
    }

    suspend fun isFavorito(idUsuario: Int, tipo: String, idElemento: Int): Boolean {
        return favoritoDao.getOne(idUsuario, tipo, idElemento) != null
    }

    // =========================
    // Helpers específicos: RUTAS
    // =========================

    suspend fun addFavoritoRuta(idUsuario: Int, idRuta: Int): Long {
        return add(idUsuario, TIPO_RUTA, idRuta)
    }

    suspend fun removeFavoritoRuta(idUsuario: Int, idRuta: Int) {
        remove(idUsuario, TIPO_RUTA, idRuta)
    }

    suspend fun isFavoritoRuta(idUsuario: Int, idRuta: Int): Boolean {
        return isFavorito(idUsuario, TIPO_RUTA, idRuta)
    }
}

package com.example.puydufouexperience.data.repository

import com.example.puydufouexperience.data.dao.ParadaRutaDao
import com.example.puydufouexperience.data.dao.RutaDao
import com.example.puydufouexperience.model.entity.ParadaRuta
import com.example.puydufouexperience.model.entity.Ruta

// Repositorio de rutas: crea rutas y gestiona paradas.
class RutasRepository(
    private val rutaDao: RutaDao,
    private val paradaRutaDao: ParadaRutaDao
) {
    suspend fun getRutasByUsuario(idUsuario: Int): List<Ruta> = rutaDao.getByUsuario(idUsuario)

    suspend fun getRutaById(idRuta: Int): Ruta? = rutaDao.getById(idRuta)

    suspend fun getParadasOrdenadas(idRuta: Int): List<ParadaRuta> = paradaRutaDao.getByRutaOrdenadas(idRuta)

    // Guardar ruta + paradas en una sola operación lógica:
    // - Inserta la ruta
    // - Inserta sus paradas apuntando al idRuta generado
    suspend fun crearRutaConParadas(ruta: Ruta, paradas: List<ParadaRuta>): Int {
        val idRuta = rutaDao.insert(ruta).toInt()

        val paradasConRuta = paradas.map { it.copy(idRuta = idRuta) }
        paradaRutaDao.insertAll(paradasConRuta)

        return idRuta
    }

    // Borrado lógico (si lo necesitas en el futuro)
    suspend fun borrarRuta(idRuta: Int) {
        // Primero paradas, luego ruta (integridad lógica, ya que no hay FK)
        paradaRutaDao.deleteByRuta(idRuta)
        rutaDao.deleteById(idRuta)
    }
}

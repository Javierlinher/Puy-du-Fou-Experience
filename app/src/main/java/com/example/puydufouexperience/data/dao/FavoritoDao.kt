package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.Favorito

@Dao
interface FavoritoDao {

    @Query("SELECT * FROM Favorito WHERE idUsuario = :idUsuario")
    suspend fun getByUsuario(idUsuario: Int): List<Favorito>

    @Query("""
        SELECT * FROM Favorito
        WHERE idUsuario = :idUsuario AND tipoElemento = :tipo AND idElemento = :idElemento
        LIMIT 1
    """)
    suspend fun getOne(idUsuario: Int, tipo: String, idElemento: Int): Favorito?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(favorito: Favorito): Long

    @Query("""
        DELETE FROM Favorito
        WHERE idUsuario = :idUsuario AND tipoElemento = :tipo AND idElemento = :idElemento
    """)
    suspend fun deleteOne(idUsuario: Int, tipo: String, idElemento: Int)
}

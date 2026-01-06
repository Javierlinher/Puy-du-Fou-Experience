package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.Ruta

@Dao
interface RutaDao {

    @Query("SELECT * FROM Ruta WHERE idUsuario = :idUsuario ORDER BY fechaCreacion DESC")
    suspend fun getByUsuario(idUsuario: Int): List<Ruta>

    @Query("SELECT * FROM Ruta WHERE id = :idRuta LIMIT 1")
    suspend fun getById(idRuta: Int): Ruta?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ruta: Ruta): Long

    @Query("DELETE FROM Ruta WHERE id = :idRuta")
    suspend fun deleteById(idRuta: Int)
}

package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.ParadaRuta

@Dao
interface ParadaRutaDao {

    @Query("SELECT * FROM ParadaRuta WHERE idRuta = :idRuta ORDER BY orden ASC")
    suspend fun getByRutaOrdenadas(idRuta: Int): List<ParadaRuta>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ParadaRuta>)

    @Query("DELETE FROM ParadaRuta WHERE idRuta = :idRuta")
    suspend fun deleteByRuta(idRuta: Int)
}

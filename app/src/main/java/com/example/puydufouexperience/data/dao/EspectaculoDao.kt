package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.Espectaculo

@Dao
interface EspectaculoDao {

    @Query("SELECT * FROM Espectaculo ORDER BY nombre ASC")
    suspend fun getAll(): List<Espectaculo>

    @Query("SELECT * FROM Espectaculo WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Espectaculo?

    // ✅ Para favoritos: traer varios espectáculos de golpe (evita consultas en bucle)
    @Query("SELECT * FROM Espectaculo WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Espectaculo>

    // Seed: metemos un lote inicial. REPLACE para facilitar re-seed en dev si hiciera falta.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Espectaculo>)
}

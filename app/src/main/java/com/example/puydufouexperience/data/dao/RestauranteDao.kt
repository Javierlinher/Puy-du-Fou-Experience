package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.Restaurante

@Dao
interface RestauranteDao {

    @Query("SELECT * FROM Restaurante ORDER BY nombre ASC")
    suspend fun getAll(): List<Restaurante>

    @Query("SELECT * FROM Restaurante WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Restaurante?

    // ✅ Para favoritos: traer varios restaurantes de golpe (evita consultas en bucle)
    @Query("SELECT * FROM Restaurante WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Restaurante>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Restaurante>)
}

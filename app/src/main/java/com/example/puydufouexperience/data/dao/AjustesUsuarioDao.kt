package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.AjustesUsuario

@Dao
interface AjustesUsuarioDao {

    @Query("SELECT * FROM AjustesUsuario WHERE idUsuario = :idUsuario LIMIT 1")
    suspend fun getByUsuario(idUsuario: Int): AjustesUsuario?

    /**
     * Upsert lógico:
     * - Si existe un registro con el mismo idUsuario (índice unique), lo reemplaza.
     * - Si no existe, lo inserta.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ajustes: AjustesUsuario): Long
}

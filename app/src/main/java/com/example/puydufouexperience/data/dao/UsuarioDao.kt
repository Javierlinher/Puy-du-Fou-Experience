package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puydufouexperience.model.entity.Usuario

@Dao
interface UsuarioDao {

    // Login: nombre + hash
    @Query("SELECT * FROM Usuario WHERE nombre = :nombre AND contrasena = :hash LIMIT 1")
    suspend fun login(nombre: String, hash: String): Usuario?

    @Query("SELECT * FROM Usuario WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Usuario?

    @Query("SELECT * FROM Usuario WHERE nombre = :nombre LIMIT 1")
    suspend fun getByNombre(nombre: String): Usuario?

    @Query("SELECT COUNT(*) FROM Usuario")
    suspend fun countUsuarios(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(usuario: Usuario): Long

    // ✅ Para panel admin: listar
    @Query("SELECT * FROM Usuario ORDER BY nombre ASC")
    suspend fun getAll(): List<Usuario>

    // ✅ Para panel admin: borrar
    @Query("DELETE FROM Usuario WHERE id = :id")
    suspend fun deleteById(id: Int): Int
}

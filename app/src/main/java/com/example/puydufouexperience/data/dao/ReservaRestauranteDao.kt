package com.example.puydufouexperience.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.puydufouexperience.model.entity.ReservaConRestauranteNombre
import com.example.puydufouexperience.model.entity.ReservaRestaurante

@Dao
interface ReservaRestauranteDao {

    // Lista de reservas del usuario actual (más recientes primero)
    @Query("""
        SELECT * FROM ReservaRestaurante
        WHERE idUsuario = :idUsuario
        ORDER BY fechaHora DESC
    """)
    suspend fun getByUsuario(idUsuario: Int): List<ReservaRestaurante>

    // ✅ Lista de reservas + nombre del restaurante (JOIN lógico por idRestaurante)
    @Query("""
        SELECT rr.*, r.nombre AS restauranteNombre
        FROM ReservaRestaurante rr
        JOIN Restaurante r ON r.id = rr.idRestaurante
        WHERE rr.idUsuario = :idUsuario
        ORDER BY rr.fechaHora DESC
    """)
    suspend fun getByUsuarioConNombre(idUsuario: Int): List<ReservaConRestauranteNombre>

    // Insertar una nueva reserva
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reserva: ReservaRestaurante): Long

    // Actualizar una reserva existente
    @Update
    suspend fun update(reserva: ReservaRestaurante)

    // Eliminar una reserva existente
    @Delete
    suspend fun delete(reserva: ReservaRestaurante)
}

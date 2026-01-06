package com.example.puydufouexperience.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.puydufouexperience.data.dao.*
import com.example.puydufouexperience.model.entity.*

@Database(
    // Entities exactas del modelo oficial (BD cerrada)
    entities = [
        Usuario::class,
        AjustesUsuario::class,
        Espectaculo::class,
        Restaurante::class,
        Ruta::class,
        ParadaRuta::class,
        Favorito::class,
        ReservaRestaurante::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun ajustesUsuarioDao(): AjustesUsuarioDao
    abstract fun espectaculoDao(): EspectaculoDao
    abstract fun restauranteDao(): RestauranteDao
    abstract fun rutaDao(): RutaDao
    abstract fun paradaRutaDao(): ParadaRutaDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun reservaRestauranteDao(): ReservaRestauranteDao
}

package com.example.puydufouexperience.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.puydufouexperience.data.seed.SeedData
import com.example.puydufouexperience.utils.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Singleton simple para obtener AppDatabase sin meter DI todavía.
// - Añade un callback onCreate para precargar datos SOLO en el primer arranque.
object DatabaseProvider {

    @Volatile private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
        }
    }

    private fun build(appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "puydufou.db")
            .addCallback(object : RoomDatabase.Callback() {

                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    // Seed en background: inserta usuario demo, ajustes por defecto, espectáculos y restaurantes
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = get(appContext)

                        val usuarioId = database.usuarioDao().insert(
                            com.example.puydufouexperience.model.entity.Usuario(
                                nombre = SeedData.DEMO_USERNAME,
                                contrasena = HashUtils.sha256(SeedData.DEMO_PASSWORD_PLAIN)
                            )
                        ).toInt()

                        // ✅ Antes: insert(...)
                        // ✅ Ahora: upsert(...) para AjustesUsuario
                        database.ajustesUsuarioDao().upsert(SeedData.ajustesPorDefecto(usuarioId))

                        database.espectaculoDao().insertAll(SeedData.espectaculos())
                        database.restauranteDao().insertAll(SeedData.restaurantes())
                    }
                }
            })
            .build()
    }
}

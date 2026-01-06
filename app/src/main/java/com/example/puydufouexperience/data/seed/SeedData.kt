package com.example.puydufouexperience.data.seed

import com.example.puydufouexperience.model.entity.AjustesUsuario
import com.example.puydufouexperience.model.entity.Espectaculo
import com.example.puydufouexperience.model.entity.Restaurante

// Datos iniciales para que la app tenga contenido desde el primer arranque.
// - 1 usuario demo
// - 1 ajustesUsuario por defecto (1:1 lógico)
// - 8+ espectáculos
// - algunos restaurantes
object SeedData {

    // Usuario admin:
    // nombre: admin
    // password: admin  (guardado como hash SHA-256; lo calculamos con HashUtils al insertar)
    const val DEMO_USERNAME = "admin"
    const val DEMO_PASSWORD_PLAIN = "admin"

    // Centro de referencia (Parquesol, Valladolid) - tu punto
    const val PARQUESOL_LAT = 41.636539
    const val PARQUESOL_LNG = -4.758565

    fun espectaculos(): List<Espectaculo> = listOf(
        Espectaculo(
            nombre = "El Último Cantar", duracionMinutos = 35, horarios = "11:00|16:00",
            latitud = 41.638020, longitud = -4.761100, imagen = "espectaculo_1", accesible = true
        ),
        Espectaculo(
            nombre = "La Forja del Reino", duracionMinutos = 25, horarios = "12:30|18:00",
            latitud = 41.637420, longitud = -4.754980, imagen = "espectaculo_2", accesible = true
        ),
        Espectaculo(
            nombre = "Amanecer en la Villa", duracionMinutos = 20, horarios = "10:30|14:00|17:30",
            latitud = 41.635200, longitud = -4.756420, imagen = "espectaculo_3", accesible = false
        ),
        Espectaculo(
            nombre = "Guardia del Alcázar", duracionMinutos = 30, horarios = "13:00|19:00",
            latitud = 41.634980, longitud = -4.760520, imagen = "espectaculo_4", accesible = true
        ),
        Espectaculo(
            nombre = "Los Navegantes", duracionMinutos = 40, horarios = "11:30|15:30",
            latitud = 41.637950, longitud = -4.758180, imagen = "espectaculo_5", accesible = true
        ),
        Espectaculo(
            nombre = "El Juicio del Conde", duracionMinutos = 28, horarios = "12:00|17:00",
            latitud = 41.636150, longitud = -4.762050, imagen = "espectaculo_6", accesible = false
        ),
        Espectaculo(
            nombre = "Noches de Antorchas", duracionMinutos = 22, horarios = "20:30",
            latitud = 41.633980, longitud = -4.757380, imagen = "espectaculo_7", accesible = true
        ),
        Espectaculo(
            nombre = "El Secreto del Monasterio", duracionMinutos = 33, horarios = "10:00|13:30|18:30",
            latitud = 41.636980, longitud = -4.753820, imagen = "espectaculo_8", accesible = true
        )
    )

    fun restaurantes(): List<Restaurante> = listOf(
        Restaurante(
            nombre = "La Taberna del Camino", tipo = "Tapas", rangoPrecio = "€€",
            latitud = 41.635780, longitud = -4.754300, imagen = "restaurante_1"
        ),
        Restaurante(
            nombre = "El Mesón del Hidalgo", tipo = "Menú", rangoPrecio = "€€",
            latitud = 41.637680, longitud = -4.760860, imagen = "restaurante_2"
        ),
        Restaurante(
            nombre = "Parrilla de la Villa", tipo = "Parrilla", rangoPrecio = "€€€",
            latitud = 41.634620, longitud = -4.759120, imagen = "restaurante_3"
        ),
        Restaurante(
            nombre = "Dulces del Mercado", tipo = "Postres", rangoPrecio = "€",
            latitud = 41.636520, longitud = -4.756050, imagen = "restaurante_4"
        )
    )

    fun ajustesPorDefecto(idUsuario: Int): AjustesUsuario =
        AjustesUsuario(
            idUsuario = idUsuario,
            idioma = "es",
            tema = "SYSTEM",
            notificaciones = true
        )
}

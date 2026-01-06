package com.example.puydufouexperience.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Utilidades para notificaciones:
 * - Crear canal (Android 8+)
 * - Mostrar notificación
 *
 * NOTA:
 * - No creamos ningún drawable propio.
 * - Usamos un icono del sistema (android.R.drawable.ic_dialog_info).
 */
object NotificationUtils {

    private const val CHANNEL_ID = "recordatorios_espectaculos"
    private const val CHANNEL_NAME = "Recordatorios de espectáculos"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de inicio y 15 minutos antes de los espectáculos"
        }

        manager.createNotificationChannel(channel)
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String
    ) {
        ensureChannel(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // ✅ Icono del sistema -> NO hace falta crear drawable
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }
}

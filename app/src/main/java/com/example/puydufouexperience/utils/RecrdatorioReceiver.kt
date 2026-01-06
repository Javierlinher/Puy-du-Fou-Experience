package com.example.puydufouexperience.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver que dispara el AlarmManager.
 * Recibe datos del espectáculo y muestra la notificación.
 */
class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Recordatorio"
        val texto = intent.getStringExtra(EXTRA_TEXTO) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)

        NotificationUtils.showNotification(
            context = context,
            notificationId = notifId,
            title = titulo,
            text = texto
        )
    }

    companion object {
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_TEXTO = "extra_texto"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}

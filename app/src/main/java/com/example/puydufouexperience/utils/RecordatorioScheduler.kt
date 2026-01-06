package com.example.puydufouexperience.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Scheduler para recordatorios con AlarmManager.
 *
 * Android 12+ (API 31+) restringe las alarmas exactas:
 * - setExactAndAllowWhileIdle() puede lanzar SecurityException si no tienes capacidad/permiso.
 *
 * Diseño recomendado:
 * - Si el sistema permite exactas => exacta
 * - Si NO => fallback a inexacta (sin crashear)
 */
object RecordatorioScheduler {

    const val TIPO_INICIO = 1
    const val TIPO_15_MIN = 2

    private const val TAG = "RecordatorioScheduler"

    fun programar(
        context: Context,
        espectaculoId: Int,
        tipo: Int,
        triggerAtMillis: Long,
        titulo: String,
        texto: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notifId = buildNotificationId(espectaculoId, tipo)

        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(RecordatorioReceiver.EXTRA_TITULO, titulo)
            putExtra(RecordatorioReceiver.EXTRA_TEXTO, texto)
            putExtra(RecordatorioReceiver.EXTRA_NOTIF_ID, notifId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(espectaculoId, tipo),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // API 31+ (Android 12+) => comprobamos si se pueden alarmas exactas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val exactAllowed = alarmManager.canScheduleExactAlarms()

                if (exactAllowed) {
                    // ✅ Exacta permitida
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // ✅ Fallback sin crash: inexacta (pero muy válida para recordatorios)
                    Log.w(TAG, "Exact alarms not allowed. Scheduling inexact alarm instead.")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                return
            }

            // API 23..30: exacta sin esa restricción especial
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // API < 23: exacta normal
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

        } catch (se: SecurityException) {
            // Cinturón de seguridad: si el OEM/OS lanza SecurityException, nunca crashear.
            Log.e(TAG, "SecurityException scheduling alarm. Falling back to inexact.", se)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelar(context: Context, espectaculoId: Int, tipo: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buildRequestCode(espectaculoId, tipo),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildRequestCode(espectaculoId: Int, tipo: Int): Int {
        return espectaculoId * 10 + tipo
    }

    private fun buildNotificationId(espectaculoId: Int, tipo: Int): Int {
        return buildRequestCode(espectaculoId, tipo)
    }
}

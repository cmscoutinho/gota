package com.gota.agua.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gota.agua.R
import com.gota.agua.data.WaterState
import com.gota.agua.ui.MainActivity
import com.gota.agua.util.formatMl

object NotificationHelper {
    const val CHANNEL_ID = "lembretes_agua"
    const val NOTIFICATION_ID = 1001

    private val titles = listOf(
        "Hora de beber água 💧",
        "Que tal um copo d'água?",
        "Pausa para se hidratar 🌊",
        "Seu corpo pede água 💙",
        "Um gole agora faz diferença",
    )

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes de água",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Avisos ao longo do dia para você beber água" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, state: WaterState) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val s = state.settings
        val remaining = (s.goalMl - state.todayMl).coerceAtLeast(0)
        val text = if (remaining > 0) {
            "Você bebeu ${formatMl(state.todayMl)} de ${formatMl(s.goalMl)}. Faltam ${formatMl(remaining)}."
        } else {
            "Meta de hoje concluída! Continue se hidratando."
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )
        val drink = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ActionReceiver::class.java)
                .setAction(ActionReceiver.ACTION_DRINK)
                .putExtra(ActionReceiver.EXTRA_ML, s.cupMl),
            flags,
        )
        val snooze = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_SNOOZE),
            flags,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(0xFF0A7BD6.toInt())
            .setContentTitle(titles.random())
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_stat_drop, "Bebi ${s.cupMl} ml", drink)
            .addAction(R.drawable.ic_stat_drop, "Lembrar em 15 min", snooze)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio.
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

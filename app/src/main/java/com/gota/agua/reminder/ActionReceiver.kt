package com.gota.agua.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gota.agua.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Trata os botões "Bebi" e "Lembrar em 15 min" da notificação. */
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = WaterRepository(app)
                when (intent.action) {
                    ACTION_DRINK -> {
                        repo.addWater(intent.getIntExtra(EXTRA_ML, 250))
                        ReminderScheduler.reschedule(app, repo)
                    }
                    ACTION_SNOOZE -> ReminderScheduler.snooze(app, repo, 15)
                }
                NotificationHelper.cancel(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DRINK = "com.gota.agua.action.DRINK"
        const val ACTION_SNOOZE = "com.gota.agua.action.SNOOZE"
        const val EXTRA_ML = "ml"
    }
}

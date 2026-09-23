package com.gota.agua.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gota.agua.data.WaterRepository
import java.time.LocalTime

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = WaterRepository(applicationContext)
        val state = repo.current()
        val settings = state.settings
        if (!settings.remindersEnabled) return Result.success()

        val now = LocalTime.now()
        val inWindow = ReminderScheduler.isInWindow(now.hour * 60 + now.minute, settings)
        val pausedByGoal = settings.stopWhenGoalReached && state.goalReached
        if (inWindow && !pausedByGoal) {
            NotificationHelper.showReminder(applicationContext, state)
        }

        ReminderScheduler.reschedule(applicationContext, repo, fromWorker = true)
        return Result.success()
    }
}

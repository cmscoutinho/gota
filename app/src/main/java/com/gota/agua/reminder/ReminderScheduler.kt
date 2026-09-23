package com.gota.agua.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gota.agua.data.Settings
import com.gota.agua.data.WaterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Agenda um lembrete por vez com o WorkManager (que sobrevive a reinicializações).
 * Cada lembrete, ao disparar, agenda o próximo.
 */
object ReminderScheduler {
    private const val WORK_NAME = "gota_lembrete"

    suspend fun reschedule(context: Context, repo: WaterRepository, fromWorker: Boolean = false) {
        val settings = repo.current().settings
        val workManager = WorkManager.getInstance(context)
        if (!settings.remindersEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            repo.setNextReminder(0L)
            return
        }
        val now = LocalDateTime.now()
        val next = nextReminderTime(settings, now)
        // Dentro do próprio worker, "APPEND" evita cancelar o trabalho em execução.
        val policy = if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        enqueue(context, Duration.between(now, next), policy)
        repo.setNextReminder(next.toEpochMillis())
    }

    suspend fun snooze(context: Context, repo: WaterRepository, minutes: Long = 15) {
        val next = LocalDateTime.now().plusMinutes(minutes)
        enqueue(context, Duration.ofMinutes(minutes), ExistingWorkPolicy.REPLACE)
        repo.setNextReminder(next.toEpochMillis())
    }

    /** Garante que exista um lembrete agendado (ex.: após o app ser forçado a parar). */
    suspend fun ensureScheduled(context: Context, repo: WaterRepository) {
        if (!repo.current().settings.remindersEnabled) return
        val infos = withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()
        }
        if (infos.none { !it.state.isFinished }) reschedule(context, repo)
    }

    fun isInWindow(minuteOfDay: Int, s: Settings): Boolean =
        if (s.startMinutes <= s.endMinutes) {
            minuteOfDay in s.startMinutes..s.endMinutes
        } else {
            // Janela que atravessa a meia-noite (ex.: 20:00 até 02:00)
            minuteOfDay >= s.startMinutes || minuteOfDay <= s.endMinutes
        }

    fun nextReminderTime(s: Settings, now: LocalDateTime): LocalDateTime {
        val candidate = now.plusMinutes(s.intervalMinutes.toLong())
        if (isInWindow(candidate.hour * 60 + candidate.minute, s)) return candidate
        var start = candidate.toLocalDate().atTime(s.startMinutes / 60, s.startMinutes % 60)
        if (!start.isAfter(candidate)) start = start.plusDays(1)
        return start
    }

    private fun enqueue(context: Context, delay: Duration, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

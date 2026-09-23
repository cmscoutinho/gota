package com.gota.agua.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gota_prefs")

/** Preferências configuráveis pelo usuário. Horários em minutos desde a meia-noite. */
data class Settings(
    val remindersEnabled: Boolean = true,
    val intervalMinutes: Int = 60,
    val startMinutes: Int = 8 * 60,
    val endMinutes: Int = 22 * 60,
    val goalMl: Int = 2000,
    val cupMl: Int = 250,
    val stopWhenGoalReached: Boolean = false,
    val weightKg: Int = 0,
)

data class WaterState(
    val settings: Settings,
    val history: Map<LocalDate, Int>,
    val todayEntries: List<Int>,
    val nextReminderAt: Long,
) {
    val todayMl: Int get() = history[LocalDate.now()] ?: 0
    val progress: Float get() = if (settings.goalMl > 0) todayMl.toFloat() / settings.goalMl else 0f
    val goalReached: Boolean get() = todayMl >= settings.goalMl
}

private object Keys {
    val ENABLED = booleanPreferencesKey("enabled")
    val INTERVAL = intPreferencesKey("interval")
    val START = intPreferencesKey("start")
    val END = intPreferencesKey("end")
    val GOAL = intPreferencesKey("goal")
    val CUP = intPreferencesKey("cup")
    val STOP_AT_GOAL = booleanPreferencesKey("stop_at_goal")
    val WEIGHT = intPreferencesKey("weight")
    val HISTORY = stringPreferencesKey("history")
    val ENTRIES = stringPreferencesKey("entries")
    val ENTRIES_DATE = stringPreferencesKey("entries_date")
    val NEXT_AT = longPreferencesKey("next_at")
}

private const val HISTORY_DAYS = 120L

class WaterRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val state: Flow<WaterState> = store.data.map { it.toState() }

    suspend fun current(): WaterState = state.first()

    suspend fun addWater(ml: Int) {
        if (ml <= 0) return
        store.edit { p ->
            val today = LocalDate.now()
            val history = decodeHistory(p[Keys.HISTORY]).toMutableMap()
            history[today] = (history[today] ?: 0) + ml
            val entries = if (p[Keys.ENTRIES_DATE] == today.toString()) decodeEntries(p[Keys.ENTRIES]) else emptyList()
            p[Keys.ENTRIES] = (entries + ml).joinToString(",")
            p[Keys.ENTRIES_DATE] = today.toString()
            p[Keys.HISTORY] = encodeHistory(history, today)
        }
    }

    suspend fun undoLast() {
        store.edit { p ->
            val today = LocalDate.now()
            if (p[Keys.ENTRIES_DATE] != today.toString()) return@edit
            val entries = decodeEntries(p[Keys.ENTRIES])
            if (entries.isEmpty()) return@edit
            val history = decodeHistory(p[Keys.HISTORY]).toMutableMap()
            history[today] = ((history[today] ?: 0) - entries.last()).coerceAtLeast(0)
            p[Keys.ENTRIES] = entries.dropLast(1).joinToString(",")
            p[Keys.HISTORY] = encodeHistory(history, today)
        }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        store.edit { p -> p.writeSettings(transform(p.toSettings())) }
    }

    suspend fun setNextReminder(epochMillis: Long) {
        store.edit { it[Keys.NEXT_AT] = epochMillis }
    }
}

private fun Preferences.toSettings() = Settings(
    remindersEnabled = this[Keys.ENABLED] ?: true,
    intervalMinutes = this[Keys.INTERVAL] ?: 60,
    startMinutes = this[Keys.START] ?: (8 * 60),
    endMinutes = this[Keys.END] ?: (22 * 60),
    goalMl = this[Keys.GOAL] ?: 2000,
    cupMl = this[Keys.CUP] ?: 250,
    stopWhenGoalReached = this[Keys.STOP_AT_GOAL] ?: false,
    weightKg = this[Keys.WEIGHT] ?: 0,
)

private fun MutablePreferences.writeSettings(s: Settings) {
    this[Keys.ENABLED] = s.remindersEnabled
    this[Keys.INTERVAL] = s.intervalMinutes.coerceIn(15, 24 * 60)
    this[Keys.START] = s.startMinutes.coerceIn(0, 24 * 60 - 1)
    this[Keys.END] = s.endMinutes.coerceIn(0, 24 * 60 - 1)
    this[Keys.GOAL] = s.goalMl.coerceIn(500, 10_000)
    this[Keys.CUP] = s.cupMl.coerceIn(50, 2_000)
    this[Keys.STOP_AT_GOAL] = s.stopWhenGoalReached
    this[Keys.WEIGHT] = s.weightKg.coerceIn(0, 400)
}

private fun Preferences.toState(): WaterState {
    val today = LocalDate.now().toString()
    return WaterState(
        settings = toSettings(),
        history = decodeHistory(this[Keys.HISTORY]),
        todayEntries = if (this[Keys.ENTRIES_DATE] == today) decodeEntries(this[Keys.ENTRIES]) else emptyList(),
        nextReminderAt = this[Keys.NEXT_AT] ?: 0L,
    )
}

// Formato: "2026-09-22=1800;2026-09-23=750"
private fun decodeHistory(raw: String?): Map<LocalDate, Int> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split(";").mapNotNull { item ->
        val parts = item.split("=")
        if (parts.size != 2) return@mapNotNull null
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val ml = parts[1].toIntOrNull() ?: return@mapNotNull null
        date to ml
    }.toMap()
}

private fun encodeHistory(history: Map<LocalDate, Int>, today: LocalDate): String {
    val oldest = today.minusDays(HISTORY_DAYS)
    return history.filterKeys { it.isAfter(oldest) }
        .toSortedMap()
        .entries.joinToString(";") { "${it.key}=${it.value}" }
}

private fun decodeEntries(raw: String?): List<Int> =
    raw?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

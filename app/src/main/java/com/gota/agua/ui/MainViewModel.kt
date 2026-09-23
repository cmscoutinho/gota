package com.gota.agua.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gota.agua.data.Settings
import com.gota.agua.data.WaterRepository
import com.gota.agua.data.WaterState
import com.gota.agua.reminder.NotificationHelper
import com.gota.agua.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val repo = WaterRepository(application)

    val state: StateFlow<WaterState?> =
        repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { ReminderScheduler.ensureScheduled(app, repo) }
    }

    /** Registra água e reinicia a contagem até o próximo lembrete. */
    fun drink(ml: Int) {
        viewModelScope.launch {
            repo.addWater(ml)
            NotificationHelper.cancel(app)
            ReminderScheduler.reschedule(app, repo)
        }
    }

    fun undo() {
        viewModelScope.launch { repo.undoLast() }
    }

    fun updateSettings(reschedule: Boolean, transform: (Settings) -> Settings) {
        viewModelScope.launch {
            repo.updateSettings(transform)
            if (reschedule) ReminderScheduler.reschedule(app, repo)
        }
    }

    fun testNotification() {
        viewModelScope.launch { NotificationHelper.showReminder(app, repo.current()) }
    }
}

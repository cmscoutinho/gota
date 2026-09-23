package com.gota.agua

import android.app.Application
import com.gota.agua.reminder.NotificationHelper

class GotaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}

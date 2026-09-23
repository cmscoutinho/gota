package com.gota.agua.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gota.agua.ui.screens.HistoryScreen
import com.gota.agua.ui.screens.SettingsScreen
import com.gota.agua.ui.screens.TodayScreen
import com.gota.agua.util.openNotificationSettings

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Hoje", Icons.Rounded.WaterDrop),
    Tab("Histórico", Icons.Rounded.BarChart),
    Tab("Ajustes", Icons.Rounded.Tune),
)

@Composable
fun GotaRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }

    fun notificationsOn() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var notificationsAllowed by remember { mutableStateOf(notificationsOn()) }
    var openSettingsIfDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted
        if (!granted && openSettingsIfDenied) openNotificationSettings(context)
        openSettingsIfDenied = false
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Atualiza o estado da permissão ao voltar das configurações do sistema.
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = notificationsOn()
        onPauseOrDispose { }
    }

    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= 33) {
            openSettingsIfDenied = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Crossfade(targetState = selected, label = "tabs", modifier = Modifier.padding(padding)) { tab ->
                when (tab) {
                    0 -> TodayScreen(
                        state = current,
                        onDrink = viewModel::drink,
                        onUndo = viewModel::undo,
                    )
                    1 -> HistoryScreen(state = current)
                    else -> SettingsScreen(
                        state = current,
                        notificationsAllowed = notificationsAllowed,
                        onRequestNotifications = requestNotifications,
                        onUpdate = { reschedule, transform -> viewModel.updateSettings(reschedule, transform) },
                        onTestNotification = viewModel::testNotification,
                    )
                }
            }
        }
    }
}

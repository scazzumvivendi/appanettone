package com.scazzumvivendi.appanettone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.scazzumvivendi.appanettone.timer.ReminderScheduler
import com.scazzumvivendi.appanettone.ui.AppanettoneScreen
import com.scazzumvivendi.appanettone.ui.AppanettoneTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var supporterBilling: SupporterBilling
    private val app get() = application as AppanettoneApplication
    private var notificationDestination by mutableStateOf<Long?>(null)
    private var notificationsAllowed by mutableStateOf(false)
    private var exactAlarmsAllowed by mutableStateOf(false)
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { updatePermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supporterBilling = SupporterBilling(this)
        enableEdgeToEdge()
        if (savedInstanceState == null) readDestination(intent)
        setContent {
            AppanettoneTheme {
                AppanettoneScreen(
                    repository = app.repository,
                    supporter = supporterBilling.state,
                    onBecomeSupporter = supporterBilling::purchase,
                    onRefreshSupporter = supporterBilling::refresh,
                    notificationDestination = notificationDestination,
                    consumeNotificationDestination = { notificationDestination = null },
                    notificationsAllowed = notificationsAllowed,
                    exactAlarmsAllowed = exactAlarmsAllowed,
                    enableNotifications = ::enableNotifications,
                    enableExactAlarms = ::enableExactAlarms,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissions()
        supporterBilling.refresh()
        lifecycleScope.launch { app.repository.reconcileTimers() }
    }

    override fun onDestroy() {
        supporterBilling.close()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDestination(intent)
    }

    private fun readDestination(intent: Intent) {
        if (intent.data?.scheme != "appanettone" || intent.data?.host != "fase") return
        val batchId = intent.getLongExtra(ReminderScheduler.EXTRA_BATCH_ID, 0)
        if (batchId > 0) notificationDestination = batchId
    }

    private fun updatePermissions() {
        notificationsAllowed = app.scheduler.notificationsEnabled()
        exactAlarmsAllowed = app.scheduler.canScheduleExact()
    }

    private fun enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !getPreferences(MODE_PRIVATE).getBoolean("notificationPermissionRequested", false)
        ) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("notificationPermissionRequested", true).apply()
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }
    }

    private fun enableExactAlarms() {
        if (Build.VERSION.SDK_INT >= 31) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
    }
}

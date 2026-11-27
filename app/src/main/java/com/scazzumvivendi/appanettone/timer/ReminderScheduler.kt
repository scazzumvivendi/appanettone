package com.scazzumvivendi.appanettone.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.scazzumvivendi.appanettone.MainActivity
import com.scazzumvivendi.appanettone.R
import com.scazzumvivendi.appanettone.data.Phase
import com.scazzumvivendi.appanettone.data.Reminder

class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    fun canScheduleExact() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    fun notificationsEnabled(): Boolean {
        val channel = notifications.getNotificationChannel(CHANNEL_ID)
        return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun schedule(phase: Phase, notifyIfDue: Boolean = false) {
        val deadline = phase.deadline ?: return
        val now = System.currentTimeMillis()
        val trigger = deadline - LEAD_TIME_MS
        if (deadline <= now || (trigger <= now && !notifyIfDue)) return

        scheduleAlarm(maxOf(now, trigger), alarmIntent(phase.id, ACTION_REMINDER))
    }

    fun scheduleTransition(phase: Phase) {
        val deadline = phase.deadline ?: return
        scheduleAlarm(deadline, alarmIntent(phase.id, ACTION_TRANSITION))
    }

    private fun scheduleAlarm(at: Long, operation: PendingIntent) {
        if (canScheduleExact()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                return
            } catch (_: SecurityException) {
                // Android can revoke exact-alarm access between the check and scheduling.
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
    }

    fun cancel(phaseId: Long) {
        cancelReminder(phaseId)
        alarms.cancel(alarmIntent(phaseId, ACTION_TRANSITION))
    }

    fun cancelReminder(phaseId: Long) {
        alarms.cancel(alarmIntent(phaseId, ACTION_REMINDER))
        notifications.cancel("phase:$phaseId", NOTIFICATION_ID)
    }

    fun show(reminder: Reminder) {
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Lievitazione", NotificationManager.IMPORTANCE_HIGH),
        )
        if (!notificationsEnabled()) return
        val phase = reminder.phase
        val deadline = phase.deadline ?: return
        val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0)
        val minutes = ((remaining + 59_999) / 60_000).coerceAtLeast(1)
        val intent = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("appanettone://fase/${phase.id}"))
            .putExtra(EXTRA_BATCH_ID, phase.panettoneId)
        val openBatch = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_panettone_mono)
            .setContentTitle(reminder.batchName)
            .setContentText(context.resources.getQuantityString(R.plurals.reminder_minutes, minutes.toInt(), minutes, phase.name))
            .setContentIntent(openBatch)
            .setTimeoutAfter(remaining.coerceAtLeast(1))
            .setAutoCancel(true)
            .build()
        notifications.notify("phase:${phase.id}", NOTIFICATION_ID, notification)
    }

    private fun alarmIntent(phaseId: Long, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("appanettone://promemoria/$phaseId"))
            .putExtra(EXTRA_PHASE_ID, phaseId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val LEAD_TIME_MS = 5 * 60_000L
        const val ACTION_TRANSITION = "com.scazzumvivendi.appanettone.TRANSITION"
        const val ACTION_REMINDER = "com.scazzumvivendi.appanettone.REMINDER"
        const val EXTRA_PHASE_ID = "phaseId"
        const val EXTRA_BATCH_ID = "batchId"
        private const val CHANNEL_ID = "lievitazione"
        private const val NOTIFICATION_ID = 1
    }
}

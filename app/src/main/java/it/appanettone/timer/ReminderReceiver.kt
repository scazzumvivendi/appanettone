package it.appanettone.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.appanettone.AppanettoneApplication
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AppanettoneApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_REMINDER -> app.repository.deliverReminder(
                        intent.getLongExtra(ReminderScheduler.EXTRA_PHASE_ID, 0),
                    )
                    ReminderScheduler.ACTION_TRANSITION,
                    Intent.ACTION_BOOT_COMPLETED,
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
                    -> app.repository.reconcileTimers()
                }
            } catch (exception: Exception) {
                Log.e("Appanettone", "Errore nel promemoria", exception)
            } finally {
                pending.finish()
            }
        }
    }
}

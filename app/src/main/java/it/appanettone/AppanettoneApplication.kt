package it.appanettone

import android.app.Application
import androidx.room.Room
import it.appanettone.data.AppDatabase
import it.appanettone.data.AppRepository
import it.appanettone.data.BatchArchive
import it.appanettone.timer.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppanettoneApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val scheduler by lazy { ReminderScheduler(this) }
    val repository by lazy {
        val database = Room.databaseBuilder(this, AppDatabase::class.java, "appanettone.db")
            .build()
        AppRepository(database, scheduler, BatchArchive(getSharedPreferences("batch_archive", MODE_PRIVATE)))
    }
}

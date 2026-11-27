package com.scazzumvivendi.appanettone

import android.app.Application
import androidx.room.Room
import com.scazzumvivendi.appanettone.data.AppDatabase
import com.scazzumvivendi.appanettone.data.AppRepository
import com.scazzumvivendi.appanettone.data.BatchArchive
import com.scazzumvivendi.appanettone.timer.ReminderScheduler
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

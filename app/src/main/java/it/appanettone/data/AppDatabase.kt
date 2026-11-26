package it.appanettone.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [Panettone::class, PhaseTemplate::class, Phase::class, Preferences::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract val dao: AppDao
}

@Dao
interface AppDao {
    @Transaction
    @Query("SELECT * FROM Panettone ORDER BY id DESC")
    fun observeBatches(): Flow<List<Batch>>

    @Transaction
    @Query("SELECT * FROM Panettone")
    suspend fun batches(): List<Batch>

    @Transaction
    @Query("SELECT * FROM Panettone WHERE id = :id")
    suspend fun batch(id: Long): Batch?

    @Query("SELECT * FROM PhaseTemplate ORDER BY id")
    fun observeTemplates(): Flow<List<PhaseTemplate>>

    @Query("SELECT * FROM PhaseTemplate ORDER BY id")
    suspend fun templates(): List<PhaseTemplate>

    @Query("SELECT * FROM Preferences WHERE id = 0")
    fun observePreferences(): Flow<Preferences?>

    @Query("SELECT * FROM Preferences WHERE id = 0")
    suspend fun preferences(): Preferences?

    @Insert suspend fun insert(panettone: Panettone): Long
    @Insert suspend fun insert(template: PhaseTemplate)
    @Insert suspend fun insert(phases: List<Phase>)
    @Query("DELETE FROM Phase WHERE panettoneId = :panettoneId")
    suspend fun deletePhases(panettoneId: Long)

    @Insert suspend fun insert(preferences: Preferences)
    @Update suspend fun update(preferences: Preferences)
    @Update suspend fun update(template: PhaseTemplate)
    @Update suspend fun updatePhases(phases: List<Phase>)
    @Delete suspend fun delete(template: PhaseTemplate)

    @Query("""
        UPDATE Phase SET startedAt = :start
        WHERE id = :id AND completedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM Phase AS earlier
            WHERE earlier.panettoneId = Phase.panettoneId
            AND earlier.position < Phase.position AND earlier.completedAt IS NULL
        )
    """)
    suspend fun setPhaseStart(id: Long, start: Long): Int

    @Query("""
        SELECT Phase.*, Panettone.name AS batchName
        FROM Phase JOIN Panettone ON Panettone.id = Phase.panettoneId
        WHERE Phase.startedAt IS NOT NULL AND Phase.completedAt IS NULL
    """)
    suspend fun activeReminders(): List<Reminder>
}

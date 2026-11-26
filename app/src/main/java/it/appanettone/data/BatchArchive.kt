package it.appanettone.data

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class BatchArchive(private val preferences: SharedPreferences) {
    private val ids = MutableStateFlow(
        preferences.getStringSet(KEY, emptySet()).orEmpty().mapNotNull(String::toLongOrNull).toSet(),
    )
    val archivedIds = ids.asStateFlow()

    suspend fun archive(id: Long) = withContext(Dispatchers.IO) {
        val updated = ids.value + id
        check(preferences.edit().putStringSet(KEY, updated.map(Long::toString).toSet()).commit())
        ids.value = updated
    }

    private companion object {
        const val KEY = "archived_batch_ids"
    }
}

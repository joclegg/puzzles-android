package com.joclegg.puzzles.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StampedPreferences(
    private val delegate: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis,
) : DataStore<Preferences> {
    override val data: Flow<Preferences> = delegate.data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        delegate.updateData { current ->
            val next = transform(current)
            if (next == current) {
                current
            } else {
                next.toMutablePreferences().apply { this[UPDATED_AT] = now() }
            }
        }

    suspend fun updatedAt(): Long = delegate.data.first()[UPDATED_AT] ?: 0

    suspend fun stamp(at: Long) {
        delegate.updateData { it.toMutablePreferences().apply { this[UPDATED_AT] = at } }
    }

    private companion object {
        val UPDATED_AT = longPreferencesKey("settings_updated_at")
    }
}

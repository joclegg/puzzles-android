package com.joclegg.puzzles.sync

import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.core.sync.SyncRow
import com.joclegg.puzzles.core.sync.SyncSettings
import com.joclegg.puzzles.core.sync.SyncSource
import org.json.JSONObject

class EntitySyncSource<E>(
    override val game: String,
    private val entities: EntityAccess<E>,
    private val mapper: EntityMapper<E>,
    private val settingsStore: StampedPreferences,
    private val readSettings: suspend () -> JSONObject,
    private val writeSettings: suspend (JSONObject) -> Unit,
) : SyncSource {
    override suspend fun changedSince(since: Long): List<SyncRow> =
        entities.changedSince(since).map { SyncRow(mapper.uuidOf(it), mapper.toJson(it), mapper.updatedAtOf(it)) }

    override suspend fun apply(row: SyncRow) {
        val local = entities.byUuid(row.id)
        if (local != null && mapper.updatedAtOf(local) >= row.updatedAt) {
            return
        }
        if (row.deleted) {
            entities.delete(row.id)
        } else {
            runCatching { mapper.fromJson(row.data, row.id, row.updatedAt) }.getOrNull()?.let { entities.import(it) }
        }
    }

    override suspend fun settings(): SyncSettings = SyncSettings(readSettings(), settingsStore.updatedAt())

    override suspend fun applySettings(settings: SyncSettings) {
        runCatching { writeSettings(settings.data) }
        settingsStore.stamp(settings.updatedAt)
    }
}

interface EntityAccess<E> {
    suspend fun changedSince(since: Long): List<E>

    suspend fun byUuid(uuid: String): E?

    suspend fun import(entity: E)

    suspend fun delete(uuid: String)
}

interface EntityMapper<E> {
    fun uuidOf(entity: E): String

    fun updatedAtOf(entity: E): Long

    fun toJson(entity: E): JSONObject

    fun fromJson(
        json: JSONObject,
        uuid: String,
        updatedAt: Long,
    ): E
}

package com.joclegg.puzzles.wordwheel

import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.core.sync.SyncSource
import com.joclegg.puzzles.core.sync.toJsonName
import com.joclegg.puzzles.sync.EntityAccess
import com.joclegg.puzzles.sync.EntityMapper
import com.joclegg.puzzles.sync.EntitySyncSource
import com.joclegg.puzzles.sync.enumName
import com.joclegg.puzzles.sync.optLongOrNull
import com.joclegg.puzzles.sync.putNullable
import com.joclegg.puzzles.sync.toJsonEnum
import com.joclegg.puzzles.wordwheel.core.data.GameDao
import com.joclegg.puzzles.wordwheel.core.data.GameEntity
import com.joclegg.puzzles.wordwheel.core.data.SettingsRepository
import com.joclegg.puzzles.wordwheel.core.data.Toggle
import com.joclegg.puzzles.wordwheel.engine.Level
import kotlinx.coroutines.flow.first
import org.json.JSONObject

fun wordWheelSyncSource(
    dao: GameDao,
    settings: SettingsRepository,
    store: StampedPreferences,
): SyncSource =
    EntitySyncSource(
        game = "wordWheel",
        entities = WordWheelAccess(dao),
        mapper = WordWheelMapper,
        settingsStore = store,
        readSettings = { settingsJson(settings) },
        writeSettings = { applySettingsJson(settings, it) },
    )

private class WordWheelAccess(
    private val dao: GameDao,
) : EntityAccess<GameEntity> {
    override suspend fun changedSince(since: Long) = dao.changedSince(since)

    override suspend fun byUuid(uuid: String) = dao.byUuid(uuid)

    override suspend fun import(entity: GameEntity) = dao.importGame(entity)

    override suspend fun delete(uuid: String) = dao.deleteByUuid(uuid)
}

private object WordWheelMapper : EntityMapper<GameEntity> {
    override fun uuidOf(entity: GameEntity) = entity.uuid

    override fun updatedAtOf(entity: GameEntity) = entity.updatedAt

    override fun toJson(entity: GameEntity): JSONObject =
        JSONObject()
            .put("id", entity.uuid)
            .put("puzzleId", entity.puzzleId)
            .put("size", Level.valueOf(entity.level).ordinal)
            .put("puzzle", entity.puzzle)
            .put("changes", progressOf(entity))
            .put("elapsedMs", entity.elapsedMs)
            .put("hints", entity.revealed.commaList().size)
            .put("mistakes", 0)
            .put("status", entity.status.toJsonEnum())
            .put("startedAt", entity.startedAt)
            .putNullable("finishedAt", entity.finishedAt)

    override fun fromJson(
        json: JSONObject,
        uuid: String,
        updatedAt: Long,
    ) = GameEntity(
        uuid = uuid,
        updatedAt = updatedAt,
        puzzleId = json.getString("puzzleId"),
        level = Level.entries[json.getInt("size")].name,
        puzzle = json.getString("puzzle"),
        found = progressPart(json.getString("changes"), 0).joinToString(","),
        bonus = progressPart(json.getString("changes"), 1).joinToString(","),
        revealed = progressPart(json.getString("changes"), 2).joinToString(",") { it.replace(',', ':') },
        elapsedMs = json.getLong("elapsedMs"),
        status = json.enumName("status"),
        startedAt = json.getLong("startedAt"),
        finishedAt = json.optLongOrNull("finishedAt"),
    )
}

private fun toggleKey(toggle: Toggle): String = toggle.name.toJsonName()

private suspend fun settingsJson(settings: SettingsRepository): JSONObject {
    val current = settings.settings.first()
    val json = JSONObject()
    Toggle.entries.forEach { json.put(toggleKey(it), it.read(current)) }

    return json
}

private suspend fun applySettingsJson(
    settings: SettingsRepository,
    json: JSONObject,
) {
    Toggle.entries.filter { json.has(toggleKey(it)) }.forEach { settings.set(it, json.getBoolean(toggleKey(it))) }
}

private fun String.commaList(): List<String> = split(',').filter { it.isNotEmpty() }

private fun progressOf(entity: GameEntity): String =
    listOf(
        entity.found.commaList().joinToString(" "),
        entity.bonus.commaList().joinToString(" "),
        entity.revealed.commaList().joinToString(" ") { it.replace(':', ',') },
    ).joinToString(";")

private fun progressPart(
    changes: String,
    index: Int,
): List<String> =
    changes
        .split(';')
        .getOrElse(index) { "" }
        .split(' ')
        .filter { it.isNotEmpty() }

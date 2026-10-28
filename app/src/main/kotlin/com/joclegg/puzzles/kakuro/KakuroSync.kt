package com.joclegg.puzzles.kakuro

import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.core.sync.SyncSource
import com.joclegg.puzzles.core.sync.toJsonName
import com.joclegg.puzzles.kakuro.core.data.GameDao
import com.joclegg.puzzles.kakuro.core.data.GameEntity
import com.joclegg.puzzles.kakuro.core.data.HintStyle
import com.joclegg.puzzles.kakuro.core.data.SettingsRepository
import com.joclegg.puzzles.kakuro.core.data.Toggle
import com.joclegg.puzzles.kakuro.engine.InputMode
import com.joclegg.puzzles.kakuro.engine.MistakeCheck
import com.joclegg.puzzles.sync.EntityAccess
import com.joclegg.puzzles.sync.EntityMapper
import com.joclegg.puzzles.sync.EntitySyncSource
import com.joclegg.puzzles.sync.enumName
import com.joclegg.puzzles.sync.optLongOrNull
import com.joclegg.puzzles.sync.putNullable
import com.joclegg.puzzles.sync.toJsonEnum
import kotlinx.coroutines.flow.first
import org.json.JSONObject

fun kakuroSyncSource(
    dao: GameDao,
    settings: SettingsRepository,
    store: StampedPreferences,
): SyncSource =
    EntitySyncSource(
        game = "kakuro",
        entities = KakuroAccess(dao),
        mapper = KakuroMapper,
        settingsStore = store,
        readSettings = { settingsJson(settings) },
        writeSettings = { applySettingsJson(settings, it) },
    )

private class KakuroAccess(
    private val dao: GameDao,
) : EntityAccess<GameEntity> {
    override suspend fun changedSince(since: Long) = dao.changedSince(since)

    override suspend fun byUuid(uuid: String) = dao.byUuid(uuid)

    override suspend fun import(entity: GameEntity) = dao.importGame(entity)

    override suspend fun delete(uuid: String) = dao.deleteByUuid(uuid)
}

private object KakuroMapper : EntityMapper<GameEntity> {
    override fun uuidOf(entity: GameEntity) = entity.uuid

    override fun updatedAtOf(entity: GameEntity) = entity.updatedAt

    override fun toJson(entity: GameEntity): JSONObject =
        JSONObject()
            .put("id", entity.uuid)
            .put("puzzleId", entity.puzzleId)
            .put("size", entity.size)
            .put("puzzle", entity.puzzle)
            .put("changes", entity.changes)
            .put("elapsedMs", entity.elapsedMs)
            .put("hints", entity.hints)
            .put("mistakes", entity.mistakes)
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
        size = json.getInt("size"),
        puzzle = json.getString("puzzle"),
        changes = json.getString("changes"),
        elapsedMs = json.getLong("elapsedMs"),
        hints = json.optInt("hints"),
        mistakes = json.optInt("mistakes"),
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
    json.put("inputMode", current.inputMode.toJsonName())
    json.put("mistakeCheck", current.mistakeCheck.toJsonName())
    json.put("hintStyle", current.hintStyle.toJsonName())
    return json
}

private suspend fun applySettingsJson(
    settings: SettingsRepository,
    json: JSONObject,
) {
    Toggle.entries.filter { json.has(toggleKey(it)) }.forEach { settings.set(it, json.getBoolean(toggleKey(it))) }
    if (json.has("inputMode")) {
        InputMode.entries.find { it.name == json.enumName("inputMode") }?.let { settings.setInputMode(it) }
    }
    if (json.has("mistakeCheck")) {
        MistakeCheck.entries.find { it.name == json.enumName("mistakeCheck") }?.let { settings.setMistakeCheck(it) }
    }
    if (json.has("hintStyle")) {
        HintStyle.entries.find { it.name == json.enumName("hintStyle") }?.let { settings.setHintStyle(it) }
    }
}

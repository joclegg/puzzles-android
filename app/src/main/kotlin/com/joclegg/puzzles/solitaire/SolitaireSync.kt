package com.joclegg.puzzles.solitaire

import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.core.sync.SyncSource
import com.joclegg.puzzles.core.sync.toJsonName
import com.joclegg.puzzles.solitaire.core.data.GameDao
import com.joclegg.puzzles.solitaire.core.data.GameEntity
import com.joclegg.puzzles.solitaire.core.data.HintStyle
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.core.data.Toggle
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.sync.EntityAccess
import com.joclegg.puzzles.sync.EntityMapper
import com.joclegg.puzzles.sync.EntitySyncSource
import com.joclegg.puzzles.sync.enumName
import com.joclegg.puzzles.sync.optLongOrNull
import com.joclegg.puzzles.sync.putNullable
import com.joclegg.puzzles.sync.toJsonEnum
import kotlinx.coroutines.flow.first
import org.json.JSONObject

fun solitaireSyncSource(
    dao: GameDao,
    settings: SettingsRepository,
    store: StampedPreferences,
): SyncSource =
    EntitySyncSource(
        game = "solitaire",
        entities = SolitaireAccess(dao),
        mapper = SolitaireMapper,
        settingsStore = store,
        readSettings = { settingsJson(settings) },
        writeSettings = { applySettingsJson(settings, it) },
    )

private class SolitaireAccess(
    private val dao: GameDao,
) : EntityAccess<GameEntity> {
    override suspend fun changedSince(since: Long) = dao.changedSince(since)

    override suspend fun byUuid(uuid: String) = dao.byUuid(uuid)

    override suspend fun import(entity: GameEntity) = dao.importGame(entity)

    override suspend fun delete(uuid: String) = dao.deleteByUuid(uuid)
}

private object SolitaireMapper : EntityMapper<GameEntity> {
    override fun uuidOf(entity: GameEntity) = entity.uuid

    override fun updatedAtOf(entity: GameEntity) = entity.updatedAt

    override fun toJson(entity: GameEntity): JSONObject =
        JSONObject()
            .put("id", entity.uuid)
            .put("dealId", entity.dealId)
            .put("drawMode", entity.drawMode.toJsonEnum())
            .put("order", entity.buildOrder.toJsonEnum())
            .put("board", entity.deal)
            .put("moves", entity.moves)
            .put("moveCount", entity.moveCount)
            .put("elapsedMs", entity.elapsedMs)
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
        dealId = json.getString("dealId"),
        drawMode = json.enumName("drawMode"),
        buildOrder = json.enumName("order"),
        deal = json.getString("board"),
        moves = json.getString("moves"),
        moveCount = json.optInt("moveCount"),
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
    json.put("drawMode", current.drawMode.toJsonName())
    json.put("hintStyle", current.hintStyle.toJsonName())
    return json
}

private suspend fun applySettingsJson(
    settings: SettingsRepository,
    json: JSONObject,
) {
    Toggle.entries.filter { json.has(toggleKey(it)) }.forEach { settings.set(it, json.getBoolean(toggleKey(it))) }
    if (json.has("drawMode")) {
        DrawMode.entries.find { it.name == json.enumName("drawMode") }?.let { settings.setDrawMode(it) }
    }
    if (json.has("hintStyle")) {
        HintStyle.entries.find { it.name == json.enumName("hintStyle") }?.let { settings.setHintStyle(it) }
    }
}

package com.joclegg.puzzles.sudoku

import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.core.sync.SyncSource
import com.joclegg.puzzles.core.sync.toJsonName
import com.joclegg.puzzles.sudoku.core.data.DigitCounts
import com.joclegg.puzzles.sudoku.core.data.GameDao
import com.joclegg.puzzles.sudoku.core.data.GameEntity
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.HintStyle
import com.joclegg.puzzles.sudoku.core.data.ModeSettings
import com.joclegg.puzzles.sudoku.core.data.PastBestEntity
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.Toggle
import com.joclegg.puzzles.sudoku.engine.HighlightStyle
import com.joclegg.puzzles.sudoku.engine.InputMode
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sync.EntityAccess
import com.joclegg.puzzles.sync.EntityMapper
import com.joclegg.puzzles.sync.EntitySyncSource
import com.joclegg.puzzles.sync.enumName
import com.joclegg.puzzles.sync.optEnumName
import com.joclegg.puzzles.sync.optLongOrNull
import com.joclegg.puzzles.sync.putNullable
import com.joclegg.puzzles.sync.toJsonEnum
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val ASSISTS = setOf(Toggle.SHOW_HINTS, Toggle.SHOW_TUTORIAL, Toggle.ALLOW_REVEAL)

fun sudokuSyncSource(
    dao: GameDao,
    settings: SettingsRepository,
    store: StampedPreferences,
): SyncSource =
    EntitySyncSource(
        game = "sudoku",
        entities = SudokuAccess(dao),
        mapper = SudokuMapper,
        settingsStore = store,
        readSettings = { settingsJson(settings, dao) },
        writeSettings = { applySettingsJson(settings, dao, it) },
    )

private class SudokuAccess(
    private val dao: GameDao,
) : EntityAccess<GameEntity> {
    override suspend fun changedSince(since: Long) = dao.changedSince(since)

    override suspend fun byUuid(uuid: String) = dao.byUuid(uuid)

    override suspend fun import(entity: GameEntity) = dao.importGame(entity)

    override suspend fun delete(uuid: String) = dao.deleteByUuid(uuid)
}

private object SudokuMapper : EntityMapper<GameEntity> {
    override fun uuidOf(entity: GameEntity) = entity.uuid

    override fun updatedAtOf(entity: GameEntity) = entity.updatedAt

    override fun toJson(entity: GameEntity): JSONObject =
        JSONObject()
            .put("id", entity.uuid)
            .put("puzzleId", entity.puzzleId)
            .put("mode", entity.mode.toJsonEnum())
            .putNullable("difficulty", entity.difficulty?.toJsonEnum())
            .put("custom", entity.custom)
            .put("puzzle", entity.puzzle)
            .put("values", entity.cellValues)
            .put(
                "notes",
                JSONArray(
                    entity.notes
                        .split(',')
                        .filter { it.isNotEmpty() }
                        .map(String::toInt),
                ),
            ).put("moves", entity.undoHistory)
            .put("elapsedMs", entity.elapsedMs)
            .put("mistakes", entity.mistakes)
            .put("mistakeCheck", entity.mistakeLevel.toJsonEnum())
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
        mode = json.enumName("mode"),
        difficulty = json.optEnumName("difficulty"),
        custom = json.optBoolean("custom"),
        puzzle = json.getString("puzzle"),
        cellValues = json.getString("values"),
        notes = json.getJSONArray("notes").let { array -> (0 until array.length()).joinToString(",") { array.getInt(it).toString() } },
        undoHistory = json.optString("moves"),
        elapsedMs = json.getLong("elapsedMs"),
        mistakes = json.optInt("mistakes"),
        mistakeLevel = json.enumName("mistakeCheck"),
        status = json.enumName("status"),
        startedAt = json.getLong("startedAt"),
        finishedAt = json.optLongOrNull("finishedAt"),
    )
}

private suspend fun settingsJson(
    settings: SettingsRepository,
    dao: GameDao,
): JSONObject {
    val current = settings.settings.first()
    val json =
        JSONObject()
            .put("inputMode", current.inputMode.toJsonName())
            .put("hintStyle", current.classic.hintStyle.toJsonName())
            .put("classic", modeJson(current.classic))
            .put("killer", modeJson(current.killer))
    ASSISTS.forEach { json.put(it.name.toJsonName(), it.read(current.classic)) }
    val pastBests = JSONArray()
    dao.observePastBests().first().forEach { best ->
        pastBests.put(
            JSONObject()
                .put("mode", best.mode.toJsonEnum())
                .put("difficulty", best.difficulty.toJsonEnum())
                .put("bestMs", best.bestMs),
        )
    }
    return json.put("pastBests", pastBests)
}

private fun modeJson(mode: ModeSettings): JSONObject {
    val json =
        JSONObject()
            .put("mistakeCheck", mode.mistakeCheck.toJsonName())
            .put("sameNumberStyle", mode.sameNumberStyle.toJsonName())
            .put("notesStyle", mode.notesStyle.toJsonName())
            .put("digitCounts", mode.digitCounts.toJsonName())
    Toggle.entries.filter { it !in ASSISTS }.forEach { json.put(it.name.toJsonName(), it.read(mode)) }
    return json
}

private suspend fun applySettingsJson(
    settings: SettingsRepository,
    dao: GameDao,
    json: JSONObject,
) {
    if (json.has("inputMode")) {
        InputMode.entries.find { it.name == json.enumName("inputMode") }?.let { settings.setInputMode(it) }
    }
    GameMode.entries.forEach { mode ->
        ASSISTS.filter { json.has(it.name.toJsonName()) }.forEach { settings.set(mode, it, json.getBoolean(it.name.toJsonName())) }
        if (json.has("hintStyle")) {
            HintStyle.entries.find { it.name == json.enumName("hintStyle") }?.let { settings.setHintStyle(mode, it) }
        }
        json.optJSONObject(mode.name.lowercase())?.let { applyModeJson(settings, mode, it) }
    }
    val pastBests = json.optJSONArray("pastBests") ?: return
    (0 until pastBests.length()).map(pastBests::getJSONObject).forEach { best ->
        dao.upsertPastBest(
            PastBestEntity(
                mode = best.enumName("mode"),
                difficulty = best.enumName("difficulty"),
                bestMs = best.getLong("bestMs"),
            ),
        )
    }
}

private suspend fun applyModeJson(
    settings: SettingsRepository,
    mode: GameMode,
    json: JSONObject,
) {
    Toggle.entries.filter { it !in ASSISTS && json.has(it.name.toJsonName()) }.forEach {
        settings.set(mode, it, json.getBoolean(it.name.toJsonName()))
    }
    if (json.has("mistakeCheck")) {
        MistakeCheck.entries.find { it.name == json.enumName("mistakeCheck") }?.let { settings.setMistakeCheck(mode, it) }
    }
    if (json.has("sameNumberStyle")) {
        HighlightStyle.entries.find { it.name == json.enumName("sameNumberStyle") }?.let { settings.setSameNumberStyle(mode, it) }
    }
    if (json.has("notesStyle")) {
        HighlightStyle.entries.find { it.name == json.enumName("notesStyle") }?.let { settings.setNotesStyle(mode, it) }
    }
    if (json.has("digitCounts")) {
        DigitCounts.entries.find { it.name == json.enumName("digitCounts") }?.let { settings.setDigitCounts(mode, it) }
    }
}

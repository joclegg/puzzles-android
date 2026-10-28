package com.joclegg.puzzles.core.sync

import org.json.JSONObject

data class SyncRow(
    val id: String,
    val data: JSONObject,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

data class SyncSettings(
    val data: JSONObject,
    val updatedAt: Long,
)

interface SyncSource {
    val game: String

    suspend fun changedSince(since: Long): List<SyncRow>

    suspend fun apply(row: SyncRow)

    suspend fun settings(): SyncSettings

    suspend fun applySettings(settings: SyncSettings)
}

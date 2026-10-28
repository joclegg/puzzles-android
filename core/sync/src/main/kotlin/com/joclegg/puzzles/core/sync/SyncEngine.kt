package com.joclegg.puzzles.core.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SyncEngine(
    private val sources: List<SyncSource>,
    private val auth: SupabaseAuth,
    private val store: SyncStore,
    private val http: Http = UrlConnectionHttp,
) {
    private val mutex = Mutex()

    suspend fun sync(): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching { syncAll() }.getOrDefault(false)
            }
        }

    private suspend fun syncAll(): Boolean {
        val token = auth.accessToken() ?: return false
        sources.forEach { push(it, token) }
        val saved = select("saved_games?select=game,id,data,updated_at,deleted", token) ?: return false
        val settings = select("game_settings?select=game,data,updated_at", token) ?: return false
        sources.forEach { source -> pull(source, saved, settings) }
        return true
    }

    private suspend fun push(
        source: SyncSource,
        token: String,
    ) {
        val rows = source.changedSince(store.pushed(source.game))
        if (rows.isNotEmpty()) {
            val body = JSONArray()
            rows.forEach { row ->
                body.put(
                    JSONObject()
                        .put("game", source.game)
                        .put("id", row.id)
                        .put("data", row.data)
                        .put("updated_at", formatTimestamp(row.updatedAt))
                        .put("deleted", row.deleted),
                )
            }
            if (upsert("saved_games", body, token)) {
                store.setPushed(source.game, rows.maxOf { it.updatedAt })
            }
        }
        val local = source.settings()
        if (local.updatedAt > store.settingsPushed(source.game)) {
            val merged = mergeSettings(store.remoteSettings(source.game), local.data)
            val body =
                JSONArray().put(
                    JSONObject()
                        .put("game", source.game)
                        .put("data", merged)
                        .put("updated_at", formatTimestamp(local.updatedAt)),
                )
            if (upsert("game_settings", body, token)) {
                store.setSettingsPushed(source.game, local.updatedAt)
                store.setRemoteSettings(source.game, merged)
            }
        }
    }

    private suspend fun pull(
        source: SyncSource,
        saved: JSONArray,
        settings: JSONArray,
    ) {
        saved.objects().filter { it.getString("game") == source.game }.forEach { row ->
            source.apply(
                SyncRow(
                    id = row.getString("id"),
                    data = row.getJSONObject("data"),
                    updatedAt = parseTimestamp(row.getString("updated_at")),
                    deleted = row.optBoolean("deleted"),
                ),
            )
        }
        val remote = settings.objects().find { it.getString("game") == source.game } ?: return
        val remoteAt = parseTimestamp(remote.getString("updated_at"))
        val data = remote.getJSONObject("data")
        store.setRemoteSettings(source.game, data)
        if (remoteAt > source.settings().updatedAt) {
            source.applySettings(SyncSettings(data, remoteAt))
            store.setSettingsPushed(source.game, remoteAt)
        }
    }

    private fun select(
        path: String,
        token: String,
    ): JSONArray? {
        val response = http.send("GET", "${SupabaseConfig.URL}/rest/v1/$path", headers(token), null)
        return if (response.ok) {
            JSONArray(response.body)
        } else {
            null
        }
    }

    private fun upsert(
        table: String,
        rows: JSONArray,
        token: String,
    ): Boolean =
        http
            .send(
                "POST",
                "${SupabaseConfig.URL}/rest/v1/$table",
                headers(token) + mapOf("Content-Type" to "application/json", "Prefer" to "resolution=merge-duplicates"),
                rows.toString(),
            ).ok

    private fun headers(token: String) = mapOf("apikey" to SupabaseConfig.KEY, "Authorization" to "Bearer $token")

    companion object {
        fun mergeSettings(
            remote: JSONObject,
            local: JSONObject,
        ): JSONObject {
            val merged = JSONObject(remote.toString())
            local.keys().forEach { key -> merged.put(key, local.get(key)) }
            return merged
        }

        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    }
}

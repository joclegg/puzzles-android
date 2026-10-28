package com.joclegg.puzzles.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class SyncStore(
    private val store: DataStore<Preferences>,
) {
    val session: Flow<Session?> = store.data.map { prefs -> prefs[SESSION]?.let(Session::parse) }

    suspend fun session(): Session? = session.first()

    suspend fun setSession(session: Session?) {
        store.edit { prefs ->
            if (session == null) {
                prefs.clear()
            } else {
                prefs[SESSION] = session.encode()
            }
        }
    }

    suspend fun verifier(): String? = store.data.first()[VERIFIER]

    suspend fun setVerifier(verifier: String) {
        store.edit { it[VERIFIER] = verifier }
    }

    suspend fun pushed(game: String): Long = store.data.first()[pushedKey(game)] ?: 0

    suspend fun setPushed(
        game: String,
        at: Long,
    ) {
        store.edit { it[pushedKey(game)] = at }
    }

    suspend fun settingsPushed(game: String): Long = store.data.first()[settingsPushedKey(game)] ?: 0

    suspend fun setSettingsPushed(
        game: String,
        at: Long,
    ) {
        store.edit { it[settingsPushedKey(game)] = at }
    }

    suspend fun remoteSettings(game: String): JSONObject = store.data.first()[remoteSettingsKey(game)]?.let(::JSONObject) ?: JSONObject()

    suspend fun setRemoteSettings(
        game: String,
        data: JSONObject,
    ) {
        store.edit { it[remoteSettingsKey(game)] = data.toString() }
    }

    private companion object {
        val SESSION = stringPreferencesKey("session")
        val VERIFIER = stringPreferencesKey("verifier")

        fun pushedKey(game: String) = longPreferencesKey("pushed_$game")

        fun settingsPushedKey(game: String) = longPreferencesKey("settings_pushed_$game")

        fun remoteSettingsKey(game: String) = stringPreferencesKey("remote_settings_$game")
    }
}

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val email: String?,
) {
    fun encode(): String =
        JSONObject()
            .put("access_token", accessToken)
            .put("refresh_token", refreshToken)
            .put("expires_at", expiresAt)
            .put("email", email)
            .toString()

    companion object {
        fun parse(text: String): Session {
            val json = JSONObject(text)
            return Session(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAt = json.getLong("expires_at"),
                email = json.optString("email").takeIf { it.isNotEmpty() },
            )
        }

        fun fromTokenResponse(
            text: String,
            now: Long,
        ): Session {
            val json = JSONObject(text)
            return Session(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAt = now + json.getLong("expires_in") * 1_000,
                email = json.optJSONObject("user")?.optString("email")?.takeIf { it.isNotEmpty() },
            )
        }
    }
}

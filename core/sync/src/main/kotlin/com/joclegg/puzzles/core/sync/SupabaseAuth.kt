package com.joclegg.puzzles.core.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class SupabaseAuth(
    private val store: SyncStore,
    private val http: Http = UrlConnectionHttp,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val email: Flow<String?> = store.session.map { session -> session?.let { it.email.orEmpty() } }

    suspend fun signInUrl(): String {
        val verifier = randomVerifier()
        store.setVerifier(verifier)
        val redirect = URLEncoder.encode(SupabaseConfig.REDIRECT, "UTF-8")
        return "${SupabaseConfig.URL}/auth/v1/authorize?provider=google&redirect_to=$redirect" +
            "&code_challenge=${challengeOf(verifier)}&code_challenge_method=s256"
    }

    suspend fun complete(code: String): Boolean {
        val verifier = store.verifier() ?: return false
        val body =
            JSONObject()
                .put("auth_code", code)
                .put("code_verifier", verifier)
                .toString()
        return requestSession("pkce", body)
    }

    suspend fun accessToken(): String? {
        val session = store.session() ?: return null
        if (session.expiresAt - REFRESH_MARGIN_MS > now()) {
            return session.accessToken
        }
        val body = JSONObject().put("refresh_token", session.refreshToken).toString()
        return if (requestSession("refresh_token", body)) {
            store.session()?.accessToken
        } else {
            null
        }
    }

    suspend fun signOut() {
        store.setSession(null)
    }

    private suspend fun requestSession(
        grant: String,
        body: String,
    ): Boolean {
        val response =
            withContext(Dispatchers.IO) {
                runCatching {
                    http.send(
                        "POST",
                        "${SupabaseConfig.URL}/auth/v1/token?grant_type=$grant",
                        mapOf("apikey" to SupabaseConfig.KEY, "Content-Type" to "application/json"),
                        body,
                    )
                }.getOrNull()
            }
        if (response == null || !response.ok) {
            return false
        }
        store.setSession(Session.fromTokenResponse(response.body, now()))
        return true
    }

    companion object {
        private const val REFRESH_MARGIN_MS = 60_000L

        fun challengeOf(verifier: String): String =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))

        private fun randomVerifier(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}

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

    suspend fun signInUrl(provider: SignInProvider): String {
        val verifier = randomVerifier()
        store.setVerifier(verifier)
        return authorizeUrl(provider, challengeOf(verifier))
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
        return when (requestSessionResult("refresh_token", body)) {
            SessionResult.OK -> {
                store.session()?.accessToken
            }

            SessionResult.REJECTED -> {
                signOut()
                null
            }

            SessionResult.UNREACHABLE -> {
                null
            }
        }
    }

    suspend fun signOut() {
        store.setSession(null)
    }

    private suspend fun requestSession(
        grant: String,
        body: String,
    ): Boolean = requestSessionResult(grant, body) == SessionResult.OK

    private suspend fun requestSessionResult(
        grant: String,
        body: String,
    ): SessionResult {
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
        if (response != null && response.code in REJECTED_CODES) {
            return SessionResult.REJECTED
        }
        if (response == null || !response.ok) {
            return SessionResult.UNREACHABLE
        }
        store.setSession(Session.fromTokenResponse(response.body, now()))
        return SessionResult.OK
    }

    private enum class SessionResult { OK, REJECTED, UNREACHABLE }

    companion object {
        private const val REFRESH_MARGIN_MS = 60_000L
        private val REJECTED_CODES = setOf(400, 401, 403)

        fun authorizeUrl(
            provider: SignInProvider,
            challenge: String,
        ): String {
            val scopes = provider.scopes?.let { "&scopes=$it" }.orEmpty()
            val redirect = URLEncoder.encode(SupabaseConfig.REDIRECT, "UTF-8")
            return "${SupabaseConfig.URL}/auth/v1/authorize?provider=${provider.id}$scopes&redirect_to=$redirect" +
                "&code_challenge=$challenge&code_challenge_method=s256"
        }

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

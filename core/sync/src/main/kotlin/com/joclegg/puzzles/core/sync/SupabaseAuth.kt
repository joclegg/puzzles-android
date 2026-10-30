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

    suspend fun sendEmailLink(email: String): Boolean {
        val verifier = randomVerifier()
        store.setVerifier(verifier)
        val response = post(emailLinkUrl(), emailLinkBody(email, challengeOf(verifier)))
        return response?.ok == true
    }

    suspend fun verifyEmailCode(
        email: String,
        code: String,
    ): Boolean {
        val response = post("${SupabaseConfig.URL}/auth/v1/verify", emailCodeBody(email, code))
        if (response == null || !response.ok) {
            return false
        }
        store.setSession(Session.fromTokenResponse(response.body, now()))
        return true
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
        val response = post("${SupabaseConfig.URL}/auth/v1/token?grant_type=$grant", body)
        if (response != null && response.code in REJECTED_CODES) {
            return SessionResult.REJECTED
        }
        if (response == null || !response.ok) {
            return SessionResult.UNREACHABLE
        }
        store.setSession(Session.fromTokenResponse(response.body, now()))
        return SessionResult.OK
    }

    private suspend fun post(
        url: String,
        body: String,
    ): HttpResponse? =
        withContext(Dispatchers.IO) {
            runCatching {
                http.send(
                    "POST",
                    url,
                    mapOf("apikey" to SupabaseConfig.KEY, "Content-Type" to "application/json"),
                    body,
                )
            }.getOrNull()
        }

    private enum class SessionResult { OK, REJECTED, UNREACHABLE }

    companion object {
        const val EMAIL_CODE_LENGTH = 8
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

        fun emailLinkUrl(): String {
            val redirect = URLEncoder.encode(SupabaseConfig.REDIRECT, "UTF-8")
            return "${SupabaseConfig.URL}/auth/v1/otp?redirect_to=$redirect"
        }

        fun emailLinkBody(
            email: String,
            challenge: String,
        ): String =
            JSONObject()
                .put("email", email.trim())
                .put("create_user", true)
                .put("code_challenge", challenge)
                .put("code_challenge_method", "s256")
                .toString()

        fun emailCodeBody(
            email: String,
            code: String,
        ): String =
            JSONObject()
                .put("type", "email")
                .put("email", email.trim())
                .put("token", code)
                .toString()

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

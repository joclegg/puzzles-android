package com.joclegg.puzzles.core.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class BankStore(
    private val directory: File,
    private val bundled: (String) -> ByteArray,
    private val http: Http = UrlConnectionHttp,
    private val baseUrl: String = BASE_URL,
) {
    private val applied = File(directory, MANIFEST)

    fun read(path: String): String = File(directory, path).takeIf { it.isFile }?.readText() ?: String(bundled(path))

    suspend fun update(): Int =
        withContext(Dispatchers.IO) {
            runCatching { updateNow() }.getOrDefault(0)
        }

    private fun updateNow(): Int {
        val response = http.send("GET", "$baseUrl/$MANIFEST", emptyMap(), null)
        if (!response.ok) {
            return 0
        }
        val remote = JSONObject(response.body).getJSONObject("files")
        val known = appliedHashes()
        var downloaded = 0
        remote.keys().forEach { path ->
            val hash = remote.getJSONObject(path).getString("sha256")
            val current = known[path] ?: runCatching { sha256(bundled(path)) }.getOrNull()
            if (hash == current) {
                known[path] = hash
            } else if (download(path, hash)) {
                known[path] = hash
                downloaded++
            }
        }
        directory.mkdirs()
        applied.writeText(JSONObject(known as Map<*, *>).toString())
        return downloaded
    }

    private fun download(
        path: String,
        hash: String,
    ): Boolean {
        val response = runCatching { http.send("GET", "$baseUrl/$path", emptyMap(), null) }.getOrNull()
        if (response == null || !response.ok || sha256(response.body.toByteArray()) != hash) {
            return false
        }
        val target = File(directory, path)
        target.parentFile?.mkdirs()
        val partial = File(target.path + ".part")
        partial.writeText(response.body)
        return partial.renameTo(target)
    }

    private fun appliedHashes(): MutableMap<String, String> {
        val json = applied.takeIf { it.isFile }?.let { JSONObject(it.readText()) } ?: JSONObject()
        return json
            .keys()
            .asSequence()
            .associateWith { json.getString(it) }
            .toMutableMap()
    }

    companion object {
        const val BASE_URL = "https://joclegg.github.io/puzzles-data"
        private const val MANIFEST = "manifest.json"

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

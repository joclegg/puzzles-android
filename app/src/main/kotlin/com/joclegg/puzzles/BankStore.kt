package com.joclegg.puzzles

import android.content.Context
import com.joclegg.puzzles.core.sync.Http
import com.joclegg.puzzles.core.sync.UrlConnectionHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class BankStore(
    private val context: Context,
    private val http: Http = UrlConnectionHttp,
    private val baseUrl: String = BASE_URL,
) {
    private val directory = File(context.filesDir, DIRECTORY)
    private val applied = File(directory, MANIFEST)

    fun read(path: String): String = File(directory, path).takeIf { it.isFile }?.readText() ?: bundled(path)

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
            val current = known[path] ?: runCatching { sha256(bundledBytes(path)) }.getOrNull()
            if (hash != current && download(path, hash)) {
                known[path] = hash
                downloaded++
            } else if (hash == current) {
                known[path] = hash
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
        val response = http.send("GET", "$baseUrl/$path", emptyMap(), null)
        if (!response.ok || sha256(response.body.toByteArray()) != hash) {
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

    private fun bundled(path: String): String = String(bundledBytes(path))

    private fun bundledBytes(path: String): ByteArray = context.assets.open(path).use { it.readBytes() }

    companion object {
        const val BASE_URL = "https://joclegg.github.io/puzzles-data"
        private const val DIRECTORY = "puzzles-data"
        private const val MANIFEST = "manifest.json"

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

package com.joclegg.puzzles.core.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest

class BankStore(
    private val directory: File,
    private val http: Http = UrlConnectionHttp,
    private val baseUrl: String = BASE_URL,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val applied = File(directory, MANIFEST)
    private val complete = File(directory, COMPLETE)
    private val wanted = File(directory, WANTED)
    private val etag = File(directory, ETAG)
    private val lock = Any()

    @Volatile
    private var appliedCache: Map<String, String>? = null

    @Volatile
    private var wantedCache: List<String>? = null

    @Volatile
    private var wantedLoaded = false

    private val _readyFolders = MutableStateFlow<Set<String>>(emptySet())
    val readyFolders: StateFlow<Set<String>> = _readyFolders.asStateFlow()

    val isReady: Boolean
        get() = complete.isFile && appliedHashes().keys.all { File(directory, it).isFile }

    fun isReady(folder: String): Boolean = folder in _readyFolders.value

    fun read(path: String): String = File(directory, path).readText()

    suspend fun refresh() = withContext(io) { refreshNow() }

    fun install(
        manifest: String,
        read: (String) -> ByteArray,
    ) {
        val files = JSONObject(manifest).getJSONObject("files")
        val known = mutableMapOf<String, String>()
        files.keys().forEach { path ->
            val target = File(directory, path)
            target.parentFile?.mkdirs()
            target.writeBytes(read(path))
            known[path] = files.getJSONObject(path).getString("sha256")
        }
        writeApplied(known)
        complete.writeText("")
        refreshNow()
    }

    suspend fun update(): Int =
        withContext(io) {
            val downloaded = runCatching { updateNow() }.getOrDefault(0)
            refreshNow()
            downloaded
        }

    private fun updateNow(): Int {
        val storedTag = etag.takeIf { it.isFile && isReady }?.readText()
        val response = http.send("GET", "$baseUrl/$MANIFEST", storedTag?.let { mapOf(IF_NONE_MATCH to it) }.orEmpty(), null)
        if (response.code == NOT_MODIFIED) {
            return 0
        }
        if (!response.ok) {
            return 0
        }
        val remote = JSONObject(response.body).getJSONObject("files")
        directory.mkdirs()
        etag.delete()
        writeWanted(remote.keys().asSequence().toList())
        val known = appliedHashes().toMutableMap()
        var downloaded = 0
        remote.keys().forEach { path ->
            val hash = remote.getJSONObject(path).getString("sha256")
            if ((hash != known[path] || !File(directory, path).isFile) && download(path, hash)) {
                known[path] = hash
                downloaded++
            }
        }
        directory.mkdirs()
        writeApplied(known)
        if (remote.keys().asSequence().all { known[it] == remote.getJSONObject(it).getString("sha256") && File(directory, it).isFile }) {
            complete.writeText("")
            response.header(ETAG_HEADER)?.let(etag::writeText)
        }
        return downloaded
    }

    private fun download(
        path: String,
        hash: String,
    ): Boolean {
        val target = File(directory, path)
        target.parentFile?.mkdirs()
        val partial = File(target.path + PART)
        val digest = MessageDigest.getInstance("SHA-256")
        val response =
            runCatching {
                DigestOutputStream(partial.outputStream().buffered(), digest).use { out ->
                    http.download("$baseUrl/$path", emptyMap(), out)
                }
            }.getOrNull()
        if (response == null || !response.ok || hex(digest.digest()) != hash) {
            partial.delete()
            return false
        }
        return partial.renameTo(target)
    }

    private fun refreshNow() {
        synchronized(lock) {
            val paths = wantedPaths() ?: appliedHashes().keys.takeIf { complete.isFile }.orEmpty()
            _readyFolders.value =
                paths
                    .filter { it.contains('/') }
                    .groupBy { it.substringBefore('/') }
                    .filterValues { files -> files.all { File(directory, it).isFile } }
                    .keys
        }
    }

    private fun writeWanted(paths: List<String>) {
        wanted.writeText(JSONArray(paths).toString())
        wantedCache = paths
        wantedLoaded = true
    }

    private fun writeApplied(hashes: Map<String, String>) {
        applied.writeText(JSONObject(hashes as Map<*, *>).toString())
        appliedCache = hashes.toMap()
    }

    private fun wantedPaths(): List<String>? {
        if (!wantedLoaded) {
            wantedCache =
                wanted.takeIf { it.isFile }?.let { file ->
                    val json = JSONArray(file.readText())
                    (0 until json.length()).map(json::getString)
                }
            wantedLoaded = true
        }
        return wantedCache
    }

    private fun appliedHashes(): Map<String, String> =
        appliedCache ?: run {
            val json = applied.takeIf { it.isFile }?.let { JSONObject(it.readText()) } ?: JSONObject()
            json
                .keys()
                .asSequence()
                .associateWith { json.getString(it) }
                .also { appliedCache = it }
        }

    companion object {
        const val BASE_URL = "https://joclegg.github.io/puzzles-data"
        private const val MANIFEST = "manifest.json"
        private const val COMPLETE = "complete"
        private const val WANTED = "wanted.json"
        private const val ETAG = "manifest.etag"
        private const val PART = ".part"
        private const val ETAG_HEADER = "ETag"
        private const val IF_NONE_MATCH = "If-None-Match"
        private const val NOT_MODIFIED = 304

        fun sha256(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

        private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    }
}

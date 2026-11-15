package com.joclegg.puzzles

import com.joclegg.puzzles.core.sync.BankStore
import org.json.JSONObject
import java.io.File

object BankFixtures {
    const val EXTRA = "banks"
    private const val ASSETS = "banks"
    private const val OFFLINE = "bank_fixtures"

    fun install(
        app: PuzzlesApplication,
        names: String?,
    ) {
        if (names == null) {
            return
        }
        val files = names.split(',').flatMap { name -> files(app, "$ASSETS/${name.trim()}") }.toMap()
        val entries = JSONObject()
        files.forEach { (path, bytes) -> entries.put(path, JSONObject().put("sha256", BankStore.sha256(bytes))) }
        File(app.filesDir, OFFLINE).writeText("")
        app.bankStore.install(JSONObject().put("files", entries).toString()) { files.getValue(it) }
    }

    fun offline(app: PuzzlesApplication): Boolean = File(app.filesDir, OFFLINE).isFile

    private fun files(
        app: PuzzlesApplication,
        root: String,
    ): List<Pair<String, ByteArray>> {
        val assets = app.assets
        return assets.list(root).orEmpty().flatMap { folder ->
            assets.list("$root/$folder").orEmpty().map { file ->
                "$folder/$file" to assets.open("$root/$folder/$file").use { it.readBytes() }
            }
        }
    }
}

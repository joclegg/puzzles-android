package com.joclegg.puzzles.core.sync

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BankStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val bundledBanks = mapOf("slideout/easy.txt" to "old\n", "kakuro/6.txt" to "same\n")

    private fun store(served: Map<String, HttpResponse>): Pair<BankStore, MutableList<String>> {
        val requested = mutableListOf<String>()
        val http =
            Http { _, url, _, _ ->
                val path = url.removePrefix("$BASE/")
                requested += path
                served[path] ?: HttpResponse(404, "")
            }
        val store = BankStore(folder.root, { path -> bundledBanks.getValue(path).toByteArray() }, http, BASE)
        return store to requested
    }

    private fun manifest(vararg files: Pair<String, String>): HttpResponse {
        val entries = JSONObject()
        files.forEach { (path, text) -> entries.put(path, JSONObject().put("sha256", BankStore.sha256(text.toByteArray()))) }
        return HttpResponse(200, JSONObject().put("files", entries).toString())
    }

    @Test
    fun `given nothing downloaded when a bank is read then it is the bundled one`() {
        // Arrange
        val (store) = store(emptyMap())

        // Act
        val text = store.read("slideout/easy.txt")

        // Assert
        assertEquals("old\n", text)
    }

    @Test
    fun `given a changed bank when banks update then only that bank is downloaded and read from then on`() =
        runBlocking {
            // Arrange
            val (store, requested) =
                store(
                    mapOf(
                        "manifest.json" to manifest("slideout/easy.txt" to "new\n", "kakuro/6.txt" to "same\n"),
                        "slideout/easy.txt" to HttpResponse(200, "new\n"),
                    ),
                )

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(1, "new\n", "same\n"), listOf(downloaded, store.read("slideout/easy.txt"), store.read("kakuro/6.txt")))
            assertEquals(listOf("manifest.json", "slideout/easy.txt"), requested)
        }

    @Test
    fun `given a download that doesn't match its hash when banks update then the bundled bank is kept`() =
        runBlocking {
            // Arrange
            val (store) =
                store(
                    mapOf(
                        "manifest.json" to manifest("slideout/easy.txt" to "new\n"),
                        "slideout/easy.txt" to HttpResponse(200, "tampered\n"),
                    ),
                )

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(0, "old\n"), listOf(downloaded, store.read("slideout/easy.txt")))
        }

    @Test
    fun `given the manifest can't be fetched when banks update then nothing changes`() =
        runBlocking {
            // Arrange
            val (store) = store(emptyMap())

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(0, "old\n"), listOf(downloaded, store.read("slideout/easy.txt")))
        }

    @Test
    fun `given a bank already downloaded when banks update again then it isn't downloaded twice`() =
        runBlocking {
            // Arrange
            val served =
                mapOf(
                    "manifest.json" to manifest("slideout/easy.txt" to "new\n"),
                    "slideout/easy.txt" to HttpResponse(200, "new\n"),
                )
            val (store, requested) = store(served)
            store.update()
            requested.clear()

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(0, listOf("manifest.json")), listOf(downloaded, requested))
        }

    private companion object {
        const val BASE = "https://example.test/data"
    }
}

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

    private fun store(served: Map<String, HttpResponse>): Pair<BankStore, MutableList<String>> {
        val requested = mutableListOf<String>()
        val http =
            Http { _, url, _, _ ->
                val path = url.removePrefix("$BASE/")
                requested += path
                served[path] ?: HttpResponse(404, "")
            }
        val store = BankStore(folder.root, http, BASE)
        return store to requested
    }

    private fun manifest(vararg files: Pair<String, String>): HttpResponse {
        val entries = JSONObject()
        files.forEach { (path, text) -> entries.put(path, JSONObject().put("sha256", BankStore.sha256(text.toByteArray()))) }
        return HttpResponse(200, JSONObject().put("files", entries).toString())
    }

    @Test
    fun `given every bank downloads when banks update then they can be read and the store is ready`() =
        runBlocking {
            // Arrange
            val (store) =
                store(
                    mapOf(
                        "manifest.json" to manifest("slideout/easy.txt" to "new\n", "kakuro/6.txt" to "same\n"),
                        "slideout/easy.txt" to HttpResponse(200, "new\n"),
                        "kakuro/6.txt" to HttpResponse(200, "same\n"),
                    ),
                )

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(
                listOf(2, "new\n", "same\n", true),
                listOf(downloaded, store.read("slideout/easy.txt"), store.read("kakuro/6.txt"), store.isReady),
            )
        }

    @Test
    fun `given a download that doesn't match its hash when banks update then the store isn't ready`() =
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
            assertEquals(listOf(0, false), listOf(downloaded, store.isReady))
        }

    @Test
    fun `given the manifest can't be fetched when banks update then the store isn't ready`() =
        runBlocking {
            // Arrange
            val (store) = store(emptyMap())

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(0, false), listOf(downloaded, store.isReady))
        }

    @Test
    fun `given a ready store when a bank changes then only that bank is downloaded`() =
        runBlocking {
            // Arrange
            var served =
                mapOf(
                    "manifest.json" to manifest("slideout/easy.txt" to "old\n", "kakuro/6.txt" to "same\n"),
                    "slideout/easy.txt" to HttpResponse(200, "old\n"),
                    "kakuro/6.txt" to HttpResponse(200, "same\n"),
                )
            val requested = mutableListOf<String>()
            val http =
                Http { _, url, _, _ ->
                    val path = url.removePrefix("$BASE/")
                    requested += path
                    served[path] ?: HttpResponse(404, "")
                }
            val store = BankStore(folder.root, http, BASE)
            store.update()
            requested.clear()
            served =
                served +
                mapOf(
                    "manifest.json" to manifest("slideout/easy.txt" to "new\n", "kakuro/6.txt" to "same\n"),
                    "slideout/easy.txt" to HttpResponse(200, "new\n"),
                )

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(1, "new\n"), listOf(downloaded, store.read("slideout/easy.txt")))
            assertEquals(listOf("manifest.json", "slideout/easy.txt"), requested)
        }

    @Test
    fun `given a ready store when offline then banks update does nothing and banks can still be read`() =
        runBlocking {
            // Arrange
            val (online) =
                store(
                    mapOf(
                        "manifest.json" to manifest("slideout/easy.txt" to "new\n"),
                        "slideout/easy.txt" to HttpResponse(200, "new\n"),
                    ),
                )
            online.update()
            val offline = BankStore(folder.root, { _, _, _, _ -> throw java.io.IOException("offline") }, BASE)

            // Act
            val downloaded = offline.update()

            // Assert
            assertEquals(listOf(0, true, "new\n"), listOf(downloaded, offline.isReady, offline.read("slideout/easy.txt")))
        }

    @Test
    fun `given installed banks when banks update then nothing is downloaded`() =
        runBlocking {
            // Arrange
            val manifest = manifest("slideout/easy.txt" to "new\n")
            val (store, requested) = store(mapOf("manifest.json" to manifest))
            store.install(manifest.body) { "new\n".toByteArray() }

            // Act
            val downloaded = store.update()

            // Assert
            assertEquals(listOf(0, true, "new\n"), listOf(downloaded, store.isReady, store.read("slideout/easy.txt")))
            assertEquals(listOf("manifest.json"), requested)
        }

    private companion object {
        const val BASE = "https://example.test/data"
    }

    @Test
    fun `given a bank file has gone missing when banks update then the store is not ready until it is downloaded again`() =
        runBlocking {
            // Arrange
            val (store) =
                store(
                    mapOf(
                        "manifest.json" to manifest("sudoku/classic_fast.txt" to "puzzle\n"),
                        "sudoku/classic_fast.txt" to HttpResponse(200, "puzzle\n"),
                    ),
                )
            store.update()
            java.io.File(folder.root, "sudoku/classic_fast.txt").delete()
            val readyWhileMissing = store.isReady

            // Act
            store.update()

            // Assert
            assertEquals(false to "puzzle\n", readyWhileMissing to store.read("sudoku/classic_fast.txt"))
        }
}

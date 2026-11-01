package com.joclegg.puzzles.core.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeleteAccountTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val store by lazy {
        SyncStore(PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("sync.preferences_pb") })
    }

    private val requests = mutableListOf<Request>()

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `given a signed-in player when the account is deleted then it calls the delete_account RPC with their token and signs out`() =
        runBlocking {
            // Arrange
            store.setSession(SESSION)
            val auth = authAnswering(204)

            // Act
            val deleted = auth.deleteAccount()

            // Assert
            assertTrue(deleted)
            val request = requests.single()
            assertEquals("POST", request.method)
            assertEquals("${SupabaseConfig.URL}/rest/v1/rpc/delete_account", request.url)
            assertEquals("Bearer access", request.headers["Authorization"])
            assertEquals(SupabaseConfig.KEY, request.headers["apikey"])
            assertNull(store.session())
        }

    @Test
    fun `given the server refuses when the account is deleted then the player stays signed in`() =
        runBlocking {
            // Arrange
            store.setSession(SESSION)
            val auth = authAnswering(500)

            // Act
            val deleted = auth.deleteAccount()

            // Assert
            assertFalse(deleted)
            assertNotNull(store.session())
        }

    @Test
    fun `given no connection when the account is deleted then the player stays signed in`() =
        runBlocking {
            // Arrange
            store.setSession(SESSION)
            val auth = SupabaseAuth(store, http = { _, _, _, _ -> throw java.io.IOException("offline") }, now = { NOW })

            // Act
            val deleted = auth.deleteAccount()

            // Assert
            assertFalse(deleted)
            assertNotNull(store.session())
        }

    @Test
    fun `given a signed-out player when the account is deleted then nothing is sent`() =
        runBlocking {
            // Arrange
            val auth = authAnswering(204)

            // Act
            val deleted = auth.deleteAccount()

            // Assert
            assertFalse(deleted)
            assertTrue(requests.isEmpty())
        }

    private fun authAnswering(code: Int) =
        SupabaseAuth(
            store,
            http = { method, url, headers, body ->
                requests += Request(method, url, headers, body)
                HttpResponse(code, "")
            },
            now = { NOW },
        )

    private data class Request(
        val method: String,
        val url: String,
        val headers: Map<String, String>,
        val body: String?,
    )

    private companion object {
        const val NOW = 1_000_000L
        val SESSION = Session("access", "refresh", NOW + 3_600_000L, "player@example.com")
    }
}

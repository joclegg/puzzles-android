package com.joclegg.puzzles.core.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {
    @Test
    fun `given remote settings with a web-only field when local settings are merged then the web-only field is kept`() {
        // Arrange
        val remote = JSONObject().put("hintStyle", "fill").put("showTimer", true)
        val local = JSONObject().put("showTimer", false)

        // Act
        val merged = SyncEngine.mergeSettings(remote, local)

        // Assert
        assertEquals("""{"hintStyle":"fill","showTimer":false}""", merged.toString())
    }

    @Test
    fun `given a PKCE verifier when its challenge is made then it is the unpadded base64url SHA-256 of the verifier`() {
        // Act
        val challenge = SupabaseAuth.challengeOf("dBjftJeZ4CVP-mJ0kF5_OW-tjvNJpRL6wX5ROG4w8zM")

        // Assert
        assertEquals("DbC1snRZOVoipAXVvVZPSrh6e3JgPBCKMq65iE8dsdI", challenge)
    }

    @Test
    fun `given no earlier pull when the pull start is worked out then it starts from the beginning`() {
        // Act
        val since = SyncEngine.pullSince(null)

        // Assert
        assertEquals("1970-01-01T00:00:00Z", since)
    }

    @Test
    fun `given an earlier pull when the pull start is worked out then it overlaps by a minute`() {
        // Act
        val since = SyncEngine.pullSince("2026-10-29T12:00:00Z")

        // Assert
        assertEquals("2026-10-29T11:59:00Z", since)
    }

    @Test
    fun `given pulled rows when the cursor is moved then it is the latest synced time`() {
        // Arrange
        val rows =
            listOf(
                JSONObject().put("synced_at", "2026-10-29T12:00:00.5+00:00"),
                JSONObject().put("synced_at", "2026-10-29T11:00:00+00:00"),
            )

        // Act
        val cursor = SyncEngine.pulledUpTo("2026-10-29T10:00:00Z", rows)

        // Assert
        assertEquals("2026-10-29T12:00:00.5+00:00", cursor)
    }
}

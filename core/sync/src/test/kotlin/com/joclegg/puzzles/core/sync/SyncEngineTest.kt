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
}

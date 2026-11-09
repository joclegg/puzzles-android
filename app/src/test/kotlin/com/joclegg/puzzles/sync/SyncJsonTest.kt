package com.joclegg.puzzles.sync

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncJsonTest {
    @Test
    fun `given a fixture puzzle id when checked then it is a fixture`() {
        // Arrange
        val ids =
            listOf(
                JSONObject().put("puzzleId", "fixture_two_tone_duplicate_line"),
                JSONObject().put("puzzleId", "fixture:killer_one_cage"),
                JSONObject().put("wordId", "fixture_word"),
                JSONObject().put("dealId", "fixture_deal"),
            )

        // Act
        val fixtures = ids.map { it.isFixture() }

        // Assert
        assertTrue(fixtures.all { it })
    }

    @Test
    fun `given a real puzzle id when checked then it is not a fixture`() {
        // Arrange
        val ids =
            listOf(
                JSONObject().put("puzzleId", "daily_2026-11-09_4"),
                JSONObject().put("wordId", "daily2_2026-11-09"),
                JSONObject(),
            )

        // Act
        val fixtures = ids.map { it.isFixture() }

        // Assert
        assertFalse(fixtures.any { it })
    }
}

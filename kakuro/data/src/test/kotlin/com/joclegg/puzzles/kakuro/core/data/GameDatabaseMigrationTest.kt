package com.joclegg.puzzles.kakuro.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), GameDatabase::class.java)

    @Test
    fun `given a version 1 database with a game when it is migrated to version 2 then the game is kept with no mistakes`() {
        // Arrange
        helper.createDatabase(NAME, 1).apply {
            execSQL(
                "INSERT INTO games (id, puzzleId, size, puzzle, changes, elapsedMs, hints, status, startedAt, finishedAt) " +
                    "VALUES (7, 'bank_6_1', 6, 'p', '', 1000, 0, 'IN_PROGRESS', 5, NULL)",
            )
            close()
        }

        // Act
        val migrated = helper.runMigrationsAndValidate(NAME, 2, true)

        // Assert
        migrated.query("SELECT id, mistakes FROM games").use { cursor ->
            cursor.moveToFirst()
            assertEquals(7L to 0, cursor.getLong(0) to cursor.getInt(1))
        }
    }

    private companion object {
        const val NAME = "migration-test"
    }
}

package com.joclegg.puzzles.sudoku.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.Puzzle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuzzleSupplyTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val games = GameRepository(database.gameDao())
    private val bankLine = "0".repeat(81) + "|" + "1".repeat(81) + "|"
    private val bank = PuzzleBank { List(BUNDLED) { bankLine }.joinToString("\n") }
    private var generatedCount = 0
    private var lowSignals = 0
    private val supply =
        PuzzleSupply(
            bank = bank,
            games = games,
            generated = database.generatedPuzzleDao(),
            onRunningLow = { lowSignals++ },
            generate = { _, _ ->
                generatedCount++
                Puzzle(List(81) { 2 }, List(81) { 2 })
            },
        )

    @After
    fun close() = database.close()

    private suspend fun play(times: Int): List<String> =
        List(times) {
            val ref = supply.next(GameMode.CLASSIC, Difficulty.EASY)
            games.start(ref, MistakeCheck.SHOW_WRONG)
            ref.id
        }

    @Test
    fun `given unplayed bundled puzzles when the next is taken then a bundled one is used and nothing is generated`() =
        runTest {
            // Act
            val ids = play(BUNDLED)

            // Assert
            assertEquals(true to 0, ids.all { it.startsWith("classic_easy:") } to generatedCount)
        }

    @Test
    fun `given every bundled puzzle played when the next is taken then one is generated on the spot`() =
        runTest {
            // Arrange
            play(BUNDLED)

            // Act
            val id = play(1).single()

            // Assert
            assertEquals(true to 1, id.startsWith("generated_classic_easy:") to generatedCount)
        }

    @Test
    fun `given a topped up stock when the bundled puzzles run out then stocked ones are used without generating`() =
        runTest {
            // Arrange
            supply.topUp(target = BUNDLED + 2)
            val made = generatedCount
            play(BUNDLED)

            // Act
            play(2)

            // Assert
            assertEquals(made, generatedCount)
        }

    @Test
    fun `given the stock falls below the low water mark when a puzzle is taken then a top up is requested`() =
        runTest {
            // Act
            play(1)

            // Assert
            assertTrue(lowSignals > 0)
        }

    private companion object {
        const val BUNDLED = 3
    }
}

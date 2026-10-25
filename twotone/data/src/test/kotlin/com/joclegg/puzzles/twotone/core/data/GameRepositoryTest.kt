package com.joclegg.puzzles.twotone.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.twotone.engine.Change
import com.joclegg.puzzles.twotone.engine.Tile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GameRepositoryTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repository = GameRepository(database.gameDao(), Dispatchers.Unconfined) { 1_000L }

    @After
    fun close() = database.close()

    private suspend fun solveIn(
        elapsedMs: Long,
        size: Int = 4,
    ): Boolean = repository.solve(repository.start { PuzzleRef.random(elapsedMs, size) }.copy(elapsedMs = elapsedMs))

    @Test
    fun `given a best time when a slower and then a faster puzzle are solved then only the faster is a new best`() =
        runTest {
            // Arrange
            solveIn(60_000)

            // Act
            val results = listOf(solveIn(90_000), solveIn(30_000))

            // Assert
            assertEquals(listOf(false, true), results)
        }

    @Test
    fun `given solved and abandoned puzzles when stats are read then played, solved, best and average are right`() =
        runTest {
            // Arrange
            solveIn(60_000)
            solveIn(30_000)
            repository.start { PuzzleRef.random(1, 4) }
            repository.start { PuzzleRef.random(2, 4) }

            // Act
            val stats = repository.stats.first().first { it.size == 4 }

            // Assert
            assertEquals(SizeStats(4, played = 3, solved = 2, bestMs = 30_000, averageMs = 45_000), stats)
        }

    @Test
    fun `given a puzzle in progress when one of another size or the daily starts then the first is kept`() =
        runTest {
            // Arrange
            repository.start { PuzzleRef.random(1, 4) }

            // Act
            repository.start { PuzzleRef.random(2, 6) }
            repository.start { PuzzleRef.daily(LocalDate.of(2026, 10, 25), 4) }

            // Assert
            assertEquals(3, repository.inProgress.first().size)
        }

    @Test
    fun `given a saved game with changes when it is read back then it is unchanged`() =
        runTest {
            // Arrange
            val game = repository.start { PuzzleRef.random(5, 6) }
            val changes = listOf(Change(3, Tile.EMPTY, Tile.RED), Change(3, Tile.RED, Tile.BLUE))

            // Act
            repository.save(game.copy(changes = changes, elapsedMs = 12_000, hints = 2))

            // Assert
            assertEquals(game.copy(changes = changes, elapsedMs = 12_000, hints = 2), repository.get(game.id))
        }
}

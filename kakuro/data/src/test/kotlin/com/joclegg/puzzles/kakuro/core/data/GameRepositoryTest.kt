package com.joclegg.puzzles.kakuro.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.kakuro.engine.Change
import com.joclegg.puzzles.kakuro.engine.Generator
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
    private val bank =
        PuzzleBank { path ->
            val size = path.substringAfter('/').substringBefore('.').toInt()
            (1L..3L).joinToString("\n") { Generator.generate(size, it).encode() }
        }
    private val repository = GameRepository(database.gameDao(), Dispatchers.Unconfined) { 1_000L }

    @After
    fun close() = database.close()

    private suspend fun solveIn(
        elapsedMs: Long,
        size: Int = 6,
    ): Boolean = repository.solve(repository.start { PuzzleRef.random(bank, size, elapsedMs) }.copy(elapsedMs = elapsedMs))

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
            repository.start { PuzzleRef.random(bank, 6, 1) }
            repository.start { PuzzleRef.random(bank, 6, 2) }

            // Act
            val stats = repository.stats.first().first { it.size == 6 }

            // Assert
            assertEquals(SizeStats(6, played = 3, solved = 2, bestMs = 30_000, averageMs = 45_000), stats)
        }

    @Test
    fun `given a puzzle in progress when one of another size or the daily starts then the first is kept`() =
        runTest {
            // Arrange
            repository.start { PuzzleRef.random(bank, 6, 1) }

            // Act
            repository.start { PuzzleRef.random(bank, 8, 2) }
            repository.start { PuzzleRef.daily(bank, LocalDate.of(2026, 10, 25), 6) }

            // Assert
            assertEquals(3, repository.inProgress.first().size)
        }

    @Test
    fun `given a saved game with changes when it is read back then it is unchanged`() =
        runTest {
            // Arrange
            val game = repository.start { PuzzleRef.random(bank, 6, 5) }
            val changes = listOf(Change(7, 0, 0, 3, 0), Change(7, 3, 0, 0, 5))

            // Act
            repository.save(game.copy(changes = changes, elapsedMs = 12_000, hints = 2))

            // Assert
            assertEquals(game.copy(changes = changes, elapsedMs = 12_000, hints = 2), repository.get(game.id))
        }
}

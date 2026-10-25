package com.joclegg.puzzles.sudoku.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.sudoku.engine.CellChange
import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import com.joclegg.puzzles.sudoku.engine.Puzzle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameRepositoryTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private var now = 1_000L
    private val repository = GameRepository(database.gameDao()) { now }
    private val puzzle = Puzzle(List(81) { it % 9 + 1 }, List(81) { it % 9 + 1 })

    @After
    fun close() = database.close()

    private fun ref(
        id: String,
        difficulty: Difficulty = Difficulty.EASY,
        custom: Boolean = false,
    ) = PuzzleRef(id, GameMode.CLASSIC, difficulty, puzzle, custom)

    private suspend fun completeIn(
        id: String,
        elapsedMs: Long,
    ): Boolean = repository.complete(repository.start(ref(id), mistakeCheck = MistakeCheck.SHOW_WRONG).copy(elapsedMs = elapsedMs))

    @Test
    fun `given no completed games when one is completed then it is a new best`() =
        runTest {
            // Act
            val newBest = completeIn("a", 60_000)

            // Assert
            assertEquals(true, newBest)
        }

    @Test
    fun `given a best time when a slower and then a faster game are completed then only the faster is a new best`() =
        runTest {
            // Arrange
            completeIn("a", 60_000)

            // Act
            val results = listOf(completeIn("b", 90_000), completeIn("c", 30_000))

            // Assert
            assertEquals(listOf(false, true), results)
        }

    @Test
    fun `given three completed games when best times are read then best, average and count are right`() =
        runTest {
            // Arrange
            completeIn("a", 60_000)
            completeIn("b", 90_000)
            completeIn("c", 30_000)

            // Act
            val best = repository.bestTimes.first().single()

            // Assert
            assertEquals(Triple(30_000L, 60_000L, 3), Triple(best.bestMs, best.averageMs, best.completed))
        }

    @Test
    fun `given past and played bests when read then best is the fastest and average counts played games only`() =
        runTest {
            // Arrange
            repository.setPastBest(GameMode.CLASSIC, Difficulty.EASY, 25_000)
            completeIn("a", 60_000)
            completeIn("b", 90_000)

            // Act
            val best = repository.bestTimes.first().single()

            // Assert
            assertEquals(Triple(25_000L, 75_000L, 2), Triple(best.bestMs, best.averageMs, best.completed))
        }

    @Test
    fun `given only a past best when best times are read then it shows with no games solved`() =
        runTest {
            // Arrange
            repository.setPastBest(GameMode.KILLER, Difficulty.EXPERT, 474_000)

            // Act
            val best = repository.bestTimes.first().single()

            // Assert
            assertEquals(BestTime(GameMode.KILLER, Difficulty.EXPERT, 474_000, 0, 0), best)
        }

    @Test
    fun `given a past best when a slower game is completed then it is not a new best`() =
        runTest {
            // Arrange
            repository.setPastBest(GameMode.CLASSIC, Difficulty.EASY, 25_000)

            // Act
            val newBest = completeIn("a", 60_000)

            // Assert
            assertEquals(false, newBest)
        }

    @Test
    fun `given a custom game when it is completed then it is in history but not in best times`() =
        runTest {
            // Arrange
            val custom = repository.start(ref("custom:1", custom = true), mistakeCheck = MistakeCheck.SHOW_WRONG)

            // Act
            val newBest = repository.complete(custom.copy(elapsedMs = 1_000))

            // Assert
            assertEquals(
                Triple(false, emptyList<BestTime>(), 1),
                Triple(newBest, repository.bestTimes.first(), repository.history.first().size),
            )
        }

    @Test
    fun `given games in progress on two levels when a new game starts on one then only that level's game is abandoned`() =
        runTest {
            // Arrange
            repository.start(ref("easy1"), mistakeCheck = MistakeCheck.SHOW_WRONG)
            repository.start(ref("hard1", Difficulty.HARD), mistakeCheck = MistakeCheck.SHOW_WRONG)
            now += 1

            // Act
            repository.start(ref("easy2"), mistakeCheck = MistakeCheck.SHOW_WRONG)

            // Assert
            val statuses = repository.history.first().associate { it.ref.id to it.status }
            assertEquals(
                mapOf("easy1" to GameStatus.ABANDONED, "hard1" to GameStatus.IN_PROGRESS, "easy2" to GameStatus.IN_PROGRESS),
                statuses,
            )
        }

    @Test
    fun `given a game with moves when it is saved and read back then everything is restored`() =
        runTest {
            // Arrange
            val game = repository.start(ref("a"), mistakeCheck = MistakeCheck.OFF)
            val moves = listOf(listOf(CellChange(0, 1, 0)), listOf(CellChange(5, 0, 6), CellChange(6, 0, 2)))
            val played = game.copy(values = game.values.toMutableList().also { it[0] = 0 }, elapsedMs = 4_200, mistakes = 2, moves = moves)

            // Act
            repository.save(played)

            // Assert
            assertEquals(played, repository.get(game.id))
        }
}

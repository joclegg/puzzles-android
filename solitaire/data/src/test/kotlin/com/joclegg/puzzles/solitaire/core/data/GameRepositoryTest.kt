package com.joclegg.puzzles.solitaire.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joclegg.puzzles.solitaire.engine.BuildOrder
import com.joclegg.puzzles.solitaire.engine.DrawMode
import com.joclegg.puzzles.solitaire.engine.Move
import com.joclegg.puzzles.solitaire.engine.Pile
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
    private val repository = GameRepository(database.gameDao()) { 1_000L }

    @After
    fun close() = database.close()

    private suspend fun winIn(
        elapsedMs: Long,
        drawMode: DrawMode = DrawMode.DRAW_ONE,
    ): Boolean = repository.win(repository.start(DealRef.random(elapsedMs, drawMode)).copy(elapsedMs = elapsedMs))

    @Test
    fun `given no wins when a game is won then it is a new best`() =
        runTest {
            // Act
            val newBest = winIn(60_000)

            // Assert
            assertEquals(true, newBest)
        }

    @Test
    fun `given a best time when a slower and then a faster game are won then only the faster is a new best`() =
        runTest {
            // Arrange
            winIn(60_000)

            // Act
            val results = listOf(winIn(90_000), winIn(30_000))

            // Assert
            assertEquals(listOf(false, true), results)
        }

    @Test
    fun `given a best time in draw one when a slower draw three game is won then it is a new best for draw three`() =
        runTest {
            // Arrange
            winIn(30_000, DrawMode.DRAW_ONE)

            // Act
            val newBest = winIn(90_000, DrawMode.DRAW_THREE)

            // Assert
            assertEquals(true, newBest)
        }

    @Test
    fun `given wins and an abandoned game when stats are read then played, won, best and average are right`() =
        runTest {
            // Arrange
            winIn(60_000)
            winIn(30_000)
            repository.start(DealRef.random(1, DrawMode.DRAW_ONE))
            repository.start(DealRef.random(2, DrawMode.DRAW_ONE))

            // Act
            val stats = repository.stats.first().first { it.drawMode == DrawMode.DRAW_ONE }

            // Assert
            assertEquals(DrawStats(DrawMode.DRAW_ONE, played = 3, won = 2, bestMs = 30_000, averageMs = 45_000, fewestMoves = 0), stats)
        }

    @Test
    fun `given a game in progress when a game in the same slot starts then the first is abandoned`() =
        runTest {
            // Arrange
            val first = repository.start(DealRef.random(1, DrawMode.DRAW_ONE))
            repository.start(DealRef.daily(LocalDate.of(2026, 10, 25), DrawMode.DRAW_ONE))

            // Act
            repository.start(DealRef.random(2, DrawMode.DRAW_THREE))

            // Assert
            assertEquals(GameStatus.ABANDONED, repository.get(first.id)?.status)
            assertEquals(2, repository.inProgress.first().size)
        }

    @Test
    fun `given a saved reverse game with moves when it is read back then the deal, order and moves are unchanged`() =
        runTest {
            // Arrange
            val game = repository.start(DealRef.random(5, DrawMode.DRAW_ONE, BuildOrder.UP))
            val moves = listOf(Move.Draw, Move.Transfer(Pile.Waste, 1, Pile.Tableau(3)))

            // Act
            repository.save(game.copy(moves = moves, elapsedMs = 12_000))

            // Assert
            assertEquals(game.copy(moves = moves, elapsedMs = 12_000), repository.get(game.id))
        }

    @Test
    fun `given a game left as lost when stats are read then it counts as played but not won, and it is no longer in progress`() =
        runTest {
            // Arrange
            val game = repository.start(DealRef.random(9, DrawMode.DRAW_ONE))

            // Act
            repository.lose(game)

            // Assert
            val stats = repository.stats.first().first { it.drawMode == DrawMode.DRAW_ONE }
            assertEquals(1 to 0, stats.played to stats.won)
            assertEquals(GameStatus.LOST, repository.get(game.id)?.status)
            assertEquals(emptyList<SavedGame>(), repository.inProgress.first())
        }
}

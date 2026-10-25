package com.joclegg.puzzles.fiveletters.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
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
    private var clock = 0L
    private val repository = GameRepository(database.gameDao(), Dispatchers.Unconfined) { ++clock }

    @After
    fun close() = database.close()

    private suspend fun play(
        guesses: Int,
        won: Boolean,
    ) {
        val game = repository.start(WordRef("practice_$clock", "CRANE"), hardMode = false)
        repository.finish(game.copy(guesses = List(guesses) { "CRANE" }), won)
    }

    @Test
    fun `given wins and a loss when stats are read then played, won, streaks and guess counts are right`() =
        runTest {
            // Arrange
            play(3, won = true)
            play(4, won = true)
            play(6, won = false)
            play(3, won = true)

            // Act
            val stats = repository.stats.first()

            // Assert
            assertEquals(WordStats(played = 4, won = 3, currentStreak = 1, maxStreak = 2, guessCounts = listOf(0, 0, 2, 1, 0, 0)), stats)
        }

    @Test
    fun `given a saved game with guesses when it is read back then it is unchanged`() =
        runTest {
            // Arrange
            val game = repository.start(WordRef("daily_2026-10-25", "CRANE"), hardMode = true)

            // Act
            repository.save(game.copy(guesses = listOf("ADIEU", "SLATE")))

            // Assert
            assertEquals(game.copy(guesses = listOf("ADIEU", "SLATE")), repository.get(game.id))
        }

    @Test
    fun `given a practice game in progress when another practice game starts then the first is abandoned and the daily is kept`() =
        runTest {
            // Arrange
            val first = repository.start(WordRef("practice_1", "CRANE"), hardMode = false)
            repository.start(WordRef("daily_2026-10-25", "SLATE"), hardMode = false)

            // Act
            repository.start(WordRef("practice_2", "TRACE"), hardMode = false)

            // Assert
            assertEquals(GameStatus.ABANDONED, repository.get(first.id)?.status)
            assertEquals(2, repository.inProgress.first().size)
        }
}

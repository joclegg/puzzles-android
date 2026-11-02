package com.joclegg.puzzles.core.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionTest {
    private var nowMs = 0L
    private var ticks = 0
    private var saves = 0
    private var running = true

    private fun TestScope.session() =
        GameSession(
            scope = backgroundScope,
            monotonicNowMs = { nowMs },
            canRun = { running },
            onTick = { ticks++ },
            onSave = { saves++ },
        )

    @Test
    fun `given a restored session when it is visible for a while then elapsed time adds to the saved time`() =
        runTest {
            // Arrange
            val session = session()
            session.restore(10_000)

            // Act
            session.onVisible()
            nowMs = 3_000
            session.onHidden()
            nowMs = 9_000

            // Assert
            assertEquals(13_000, session.elapsedMs())
        }

    @Test
    fun `given a game that cannot run when the session becomes visible then the timer stays paused`() =
        runTest {
            // Arrange
            running = false
            val session = session()

            // Act
            session.onVisible()
            nowMs = 5_000

            // Assert
            assertEquals(0, session.elapsedMs())
        }

    @Test
    fun `given a visible session when time passes then it ticks every second`() =
        runTest {
            // Arrange
            val session = session()

            // Act
            session.onVisible()
            runCurrent()
            advanceTimeBy(2_100)

            // Assert
            assertEquals(3, ticks)
        }

    @Test
    fun `given a session part way through a second when it resumes then the next tick lands on the whole second`() =
        runTest {
            // Arrange
            val session = session()
            session.restore(10_400)

            // Act
            session.onVisible()
            runCurrent()
            advanceTimeBy(601)

            // Assert
            assertEquals(2, ticks)
        }

    @Test
    fun `given several quick saves when the session goes idle then it saves once`() =
        runTest {
            // Arrange
            val session = session()

            // Act
            session.saveSoon()
            advanceTimeBy(200)
            session.saveSoon()
            advanceTimeBy(500)

            // Assert
            assertEquals(1, saves)
        }

    @Test
    fun `given a pending save when the session stops then nothing is saved`() =
        runTest {
            // Arrange
            val session = session()
            session.saveSoon()

            // Act
            session.stop()
            advanceTimeBy(1_000)

            // Assert
            assertEquals(0, saves)
        }

    @Test
    fun `given a running session when it restarts then elapsed time starts from zero`() =
        runTest {
            // Arrange
            val session = session()
            session.restore(20_000)
            session.onVisible()
            nowMs = 4_000

            // Act
            session.restart()
            nowMs = 6_000

            // Assert
            assertEquals(2_000, session.elapsedMs())
        }
}

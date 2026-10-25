package com.joclegg.puzzles.sudoku.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class GameTimerTest {
    @ParameterizedTest
    @MethodSource("timelines")
    fun `given starts and pauses when elapsed time is read then only running time counts`(
        timer: GameTimer,
        readAtMs: Long,
        expectedMs: Long,
    ) {
        // Act
        val elapsed = timer.elapsedMs(readAtMs)

        // Assert
        assertEquals(expectedMs, elapsed)
    }

    companion object {
        @JvmStatic
        fun timelines() =
            listOf(
                Arguments.of(GameTimer(), 5_000L, 0L),
                Arguments.of(GameTimer().start(1_000), 4_000L, 3_000L),
                Arguments.of(GameTimer().start(1_000).pause(4_000), 60_000L, 3_000L),
                Arguments.of(GameTimer().start(1_000).pause(4_000).start(10_000), 12_000L, 5_000L),
                Arguments.of(GameTimer(accumulatedMs = 90_000).start(0), 1_000L, 91_000L),
            )
    }
}

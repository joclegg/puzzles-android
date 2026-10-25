package com.joclegg.puzzles.sudoku.core.data

import com.joclegg.puzzles.sudoku.engine.Difficulty
import com.joclegg.puzzles.sudoku.engine.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PuzzleRefSlotTest(
    private val id: String,
    private val mode: GameMode,
    private val difficulty: Difficulty,
    private val custom: Boolean,
    private val expectedSlot: String,
) {
    @Test
    fun `given a puzzle when its save slot is read then each level, daily date and custom has its own`() {
        // Arrange
        val ref = PuzzleRef(id, mode, difficulty, Puzzle(List(81) { 0 }, null), custom)

        // Act
        val slot = ref.slot

        // Assert
        assertEquals(expectedSlot, slot)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {4}")
        fun cases() =
            listOf(
                arrayOf<Any?>("classic_easy:3", GameMode.CLASSIC, Difficulty.EASY, false, "CLASSIC_EASY"),
                arrayOf<Any?>("killer_evil:9", GameMode.KILLER, Difficulty.EVIL, false, "KILLER_EVIL"),
                arrayOf<Any?>("daily_classic:2026-10-24", GameMode.CLASSIC, Difficulty.HARD, false, "daily_classic:2026-10-24"),
                arrayOf<Any?>("daily_killer:2026-10-25", GameMode.KILLER, Difficulty.EASY, false, "daily_killer:2026-10-25"),
                arrayOf<Any?>("custom:123", GameMode.KILLER, Difficulty.MEDIUM, true, "custom"),
            )
    }
}

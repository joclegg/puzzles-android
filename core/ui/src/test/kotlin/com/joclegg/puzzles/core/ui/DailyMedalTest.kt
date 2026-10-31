package com.joclegg.puzzles.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DailyMedalTest(
    private val count: Int,
    private val total: Int,
    private val firstMedal: Medal,
    private val expected: Medal,
) {
    @Test
    fun `given dailies done on the day when the medal is worked out then it follows the rule`() {
        // Act
        val medal = medalFor(count, total, firstMedal)

        // Assert
        assertEquals(expected, medal)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} of {1}, first {2}")
        fun cases() =
            listOf(
                arrayOf<Any>(0, 5, Medal.BRONZE, Medal.NONE),
                arrayOf<Any>(1, 5, Medal.BRONZE, Medal.BRONZE),
                arrayOf<Any>(2, 5, Medal.BRONZE, Medal.SILVER),
                arrayOf<Any>(5, 5, Medal.BRONZE, Medal.GOLD),
                arrayOf<Any>(1, 2, Medal.SILVER, Medal.SILVER),
                arrayOf<Any>(2, 2, Medal.SILVER, Medal.GOLD),
                arrayOf<Any>(1, 1, Medal.BRONZE, Medal.GOLD),
            )
    }
}

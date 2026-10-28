package com.joclegg.puzzles.core.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SyncNamesTest(
    private val enumName: String,
    private val jsonName: String,
) {
    @Test
    fun `given an enum name when it is converted then it matches the web value both ways`() {
        // Act
        val json = enumName.toJsonName()
        val back = json.toEnumName()

        // Assert
        assertEquals(listOf(jsonName, enumName), listOf(json, back))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun names() =
            listOf(
                arrayOf("IN_PROGRESS", "inProgress"),
                arrayOf("SOLVED", "solved"),
                arrayOf("DRAW_ONE", "drawOne"),
                arrayOf("TAP_TO_CYCLE", "tapToCycle"),
                arrayOf("SHOW_WRONG", "showWrong"),
            )
    }
}

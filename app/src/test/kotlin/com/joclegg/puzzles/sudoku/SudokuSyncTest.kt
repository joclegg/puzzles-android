package com.joclegg.puzzles.sudoku

import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.HintStyle
import com.joclegg.puzzles.sudoku.core.data.ModeSettings
import com.joclegg.puzzles.sudoku.core.data.Settings
import com.joclegg.puzzles.sudoku.core.data.Toggle
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SudokuSyncTest {
    @Test
    fun `given different assists per mode when settings are written then each mode object has its own and the top level has classic`() {
        // Arrange
        val settings =
            Settings(
                classic = ModeSettings(showHints = true, allowReveal = false, showTutorial = true, hintStyle = HintStyle.FILL),
                killer = ModeSettings(showHints = false, allowReveal = true, showTutorial = false, hintStyle = HintStyle.POINT),
            )

        // Act
        val json = settingsJson(settings)

        // Assert
        val classic = json.getJSONObject("classic")
        val killer = json.getJSONObject("killer")
        assertEquals(listOf(true, false, true, "fill"), assistValues(classic))
        assertEquals(listOf(false, true, false, "point"), assistValues(killer))
        assertEquals(listOf(true, false, true, "fill"), assistValues(json))
    }

    @Test
    fun `given assists in each mode object when settings are read then each mode takes its own`() {
        // Arrange
        val json =
            JSONObject()
                .put("showHints", true)
                .put("hintStyle", "fill")
                .put("classic", assistsJson(hints = true, reveal = false, tutorial = true, style = "fill"))
                .put("killer", assistsJson(hints = false, reveal = true, tutorial = false, style = "point"))

        // Act
        val classic = assistsFor(json, GameMode.CLASSIC)
        val killer = assistsFor(json, GameMode.KILLER)

        // Assert
        assertEquals(assists(hints = true, reveal = false, tutorial = true, style = HintStyle.FILL), classic)
        assertEquals(assists(hints = false, reveal = true, tutorial = false, style = HintStyle.POINT), killer)
    }

    @Test
    fun `given an old payload with assists only at the top level when settings are read then both modes fall back to them`() {
        // Arrange
        val json =
            JSONObject()
                .put("showHints", true)
                .put("allowReveal", true)
                .put("showTutorial", false)
                .put("hintStyle", "fill")
                .put("classic", JSONObject().put("showTimer", false))
                .put("killer", JSONObject().put("showTimer", true))
        val expected = assists(hints = true, reveal = true, tutorial = false, style = HintStyle.FILL)

        // Act
        val classic = assistsFor(json, GameMode.CLASSIC)
        val killer = assistsFor(json, GameMode.KILLER)

        // Assert
        assertEquals(expected, classic)
        assertEquals(expected, killer)
    }

    @Test
    fun `given a mode object missing some assists when settings are read then only the missing ones fall back to the top level`() {
        // Arrange
        val json =
            JSONObject()
                .put("showHints", true)
                .put("hintStyle", "fill")
                .put("killer", JSONObject().put("showHints", false))

        // Act
        val killer = assistsFor(json, GameMode.KILLER)

        // Assert
        assertEquals(ModeAssists(mapOf(Toggle.SHOW_HINTS to false), HintStyle.FILL), killer)
    }

    private fun assistsJson(
        hints: Boolean,
        reveal: Boolean,
        tutorial: Boolean,
        style: String,
    ): JSONObject =
        JSONObject()
            .put("showHints", hints)
            .put("allowReveal", reveal)
            .put("showTutorial", tutorial)
            .put("hintStyle", style)

    private fun assists(
        hints: Boolean,
        reveal: Boolean,
        tutorial: Boolean,
        style: HintStyle,
    ) = ModeAssists(mapOf(Toggle.SHOW_HINTS to hints, Toggle.ALLOW_REVEAL to reveal, Toggle.SHOW_TUTORIAL to tutorial), style)

    private fun assistValues(json: JSONObject): List<Any> =
        listOf(json.getBoolean("showHints"), json.getBoolean("allowReveal"), json.getBoolean("showTutorial"), json.getString("hintStyle"))
}

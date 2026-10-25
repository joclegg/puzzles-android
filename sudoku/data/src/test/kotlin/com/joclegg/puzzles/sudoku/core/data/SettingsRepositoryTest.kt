package com.joclegg.puzzles.sudoku.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.joclegg.puzzles.sudoku.engine.MistakeCheck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsRepositoryTest(
    private val description: String,
    private val classic: MistakeCheck?,
    private val killer: MistakeCheck?,
    private val expected: Pair<MistakeCheck, MistakeCheck>,
) {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `given saved mistake levels when settings are read then each mode gets its own`() =
        runTest {
            // Arrange
            val repository =
                SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.root.resolve("s.preferences_pb") })
            classic?.let { repository.setMistakeCheck(GameMode.CLASSIC, it) }
            killer?.let { repository.setMistakeCheck(GameMode.KILLER, it) }

            // Act
            val settings = repository.settings.first()

            // Assert
            assertEquals(expected, settings.mistakeCheckFor(GameMode.CLASSIC) to settings.mistakeCheckFor(GameMode.KILLER))
        }

    @Test
    fun `given nothing saved when settings are read then killer has plain starting cells and classic grey`() =
        runTest {
            // Arrange
            val repository =
                SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.root.resolve("d.preferences_pb") })

            // Act
            val settings = repository.settings.first()

            // Assert
            assertEquals(true to false, settings.classic.greyGivenCells to settings.killer.greyGivenCells)
        }

    @Test
    fun `given a toggle turned off for killer when settings are read then classic keeps it on`() =
        runTest {
            // Arrange
            val repository =
                SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.root.resolve("t.preferences_pb") })
            repository.set(GameMode.KILLER, Toggle.SHOW_TIMER, false)

            // Act
            val settings = repository.settings.first()

            // Assert
            assertEquals(true to false, settings.classic.showTimer to settings.killer.showTimer)
        }

    @Test
    fun `given dark chosen when settings are read then the theme is dark, and system before anything is chosen`() =
        runTest {
            // Arrange
            val repository =
                SettingsRepository(
                    PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.root.resolve("theme.preferences_pb") },
                )
            val before = repository.settings.first().theme
            repository.setTheme(ThemeMode.DARK)

            // Act
            val after = repository.settings.first().theme

            // Assert
            assertEquals(ThemeMode.SYSTEM to ThemeMode.DARK, before to after)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any?>("nothing saved", null, null, MistakeCheck.SHOW_WRONG to MistakeCheck.SHOW_WRONG),
                arrayOf<Any?>("classic off, killer shows wrong", MistakeCheck.OFF, null, MistakeCheck.OFF to MistakeCheck.SHOW_WRONG),
                arrayOf<Any?>(
                    "classic blocks clashes, killer off",
                    MistakeCheck.BLOCK_CLASHES,
                    MistakeCheck.OFF,
                    MistakeCheck.BLOCK_CLASHES to MistakeCheck.OFF,
                ),
            )
    }
}

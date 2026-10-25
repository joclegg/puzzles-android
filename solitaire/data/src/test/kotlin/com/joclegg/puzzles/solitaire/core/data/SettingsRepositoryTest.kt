package com.joclegg.puzzles.solitaire.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsRepositoryTest(
    private val toggle: Toggle,
) {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository() =
        SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("settings.preferences_pb") })

    @Test
    fun `given default settings when a toggle is read then it has its default`() =
        runTest {
            // Act
            val settings = repository().settings.first()

            // Assert
            assertEquals(toggle.default, toggle.read(settings))
        }

    @Test
    fun `given a toggle flipped when settings are read then only that toggle has changed`() =
        runTest {
            // Arrange
            val repository = repository()

            // Act
            repository.set(toggle, !toggle.default)

            // Assert
            val settings = repository.settings.first()
            assertEquals(Toggle.entries.associateWith { it.default != (it == toggle) }, Toggle.entries.associateWith { it.read(settings) })
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun toggles() = Toggle.entries.toList()
    }
}

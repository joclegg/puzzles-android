package com.joclegg.puzzles

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsHubScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<String>()

    private fun show(state: AppSettingsUiState) {
        val uriHandler =
            object : UriHandler {
                override fun openUri(uri: String) {
                    opened += uri
                }
            }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                SettingsHubScreen(
                    state = state,
                    initialTab = SettingsTab.GENERAL,
                    onThemeChange = {},
                    onDeleteAccount = {},
                    onBack = {},
                    accentOf = { Color.Unspecified },
                    tabContent = {},
                )
            }
        }
    }

    @Test
    fun `given a signed out player when general settings are shown then the privacy policy link is shown`() {
        // Arrange
        val state = AppSettingsUiState(signedIn = false)

        // Act
        show(state)

        // Assert
        compose.onNodeWithTag("privacy_policy").assertIsDisplayed()
    }

    @Test
    fun `given a signed in player when the privacy policy link is tapped then the privacy page opens`() {
        // Arrange
        show(AppSettingsUiState(signedIn = true))

        // Act
        compose.onNodeWithTag("privacy_policy").performClick()

        // Assert
        assertEquals(listOf(SettingsHub.PRIVACY_POLICY_URL), opened)
    }
}

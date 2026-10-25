package com.joclegg.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.ThemeMode
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsTab(
    val label: String,
) {
    GENERAL("General"),
    SUDOKU("Sudoku"),
    SOLITAIRE("Solitaire"),
    TWO_TONE("Two Tone"),
    SIGHTLINES("Sightlines"),
    FIVE_LETTERS("Five Letters"),
    KAKURO("Kakuro"),
}

object SettingsHub {
    const val ROUTE = "settings/{tab}?mode={mode}"

    fun route(
        tab: SettingsTab,
        mode: String? = null,
    ) = "settings/${tab.name}" + mode?.let { "?mode=$it" }.orEmpty()
}

data class AppSettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

class AppSettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<AppSettingsUiState> =
        settings.settings
            .map { AppSettingsUiState(it.theme) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettingsUiState())

    fun onThemeChange(theme: ThemeMode) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun SettingsHubRoute(
    viewModel: AppSettingsViewModel,
    initialTab: SettingsTab,
    onBack: () -> Unit,
    tabContent: @Composable (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsHubScreen(state, initialTab, viewModel::onThemeChange, onBack, tabContent, modifier)
}

@Composable
fun SettingsHubScreen(
    state: AppSettingsUiState,
    initialTab: SettingsTab,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    tabContent: @Composable (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 16.dp) {
            SettingsTab.entries.forEach { option ->
                Tab(
                    selected = tab == option,
                    onClick = { tab = option },
                    text = { Text(option.label) },
                    modifier = Modifier.testTag("settings_tab_${option.name.lowercase()}"),
                )
            }
        }
        Box(Modifier.weight(1f)) {
            if (tab == SettingsTab.GENERAL) {
                GeneralSettings(state.theme, onThemeChange)
            } else {
                tabContent(tab)
            }
        }
    }
}

@Composable
private fun GeneralSettings(
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = theme == option,
                            onClick = { onThemeChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            modifier = Modifier.testTag("setting_theme_${option.name.lowercase()}"),
                            icon = {},
                        ) {
                            Text(
                                when (option) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            )
                        }
                    }
                }
                Text(
                    "Applies to every game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

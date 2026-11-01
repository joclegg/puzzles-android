package com.joclegg.puzzles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.joclegg.puzzles.core.ui.BackButton
import com.joclegg.puzzles.core.ui.HELPERS_NOTE
import com.joclegg.puzzles.sudoku.core.data.SettingsRepository
import com.joclegg.puzzles.sudoku.core.data.ThemeMode
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    SLIDE_OUT("Slide Out"),
    WORD_WHEEL("Word Wheel"),
}

object SettingsHub {
    const val ROUTE = "settings/{tab}?mode={mode}"

    fun route(
        tab: SettingsTab,
        mode: String? = null,
    ) = "settings/${tab.name}" + mode?.let { "?mode=$it" }.orEmpty()
}

enum class DeleteAccountStatus { IDLE, DELETING, FAILED }

data class AppSettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val signedIn: Boolean = false,
    val deleteAccount: DeleteAccountStatus = DeleteAccountStatus.IDLE,
)

class AppSettingsViewModel(
    private val settings: SettingsRepository,
    account: Flow<String?> = flowOf(null),
    private val deleteAccount: suspend () -> Boolean = { false },
) : ViewModel() {
    private val deleteStatus = MutableStateFlow(DeleteAccountStatus.IDLE)

    val uiState: StateFlow<AppSettingsUiState> =
        combine(settings.settings, account, deleteStatus) { current, email, status ->
            AppSettingsUiState(current.theme, signedIn = email != null, deleteAccount = status)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettingsUiState())

    fun onThemeChange(theme: ThemeMode) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun onDeleteAccount() {
        if (deleteStatus.value == DeleteAccountStatus.DELETING) {
            return
        }
        deleteStatus.value = DeleteAccountStatus.DELETING
        viewModelScope.launch {
            deleteStatus.value =
                if (deleteAccount()) {
                    DeleteAccountStatus.IDLE
                } else {
                    DeleteAccountStatus.FAILED
                }
        }
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
    accentOf: @Composable (SettingsTab) -> Color,
    tabContent: @Composable (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsHubScreen(state, initialTab, viewModel::onThemeChange, viewModel::onDeleteAccount, onBack, accentOf, tabContent, modifier)
}

@Composable
fun SettingsHubScreen(
    state: AppSettingsUiState,
    initialTab: SettingsTab,
    onThemeChange: (ThemeMode) -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit,
    accentOf: @Composable (SettingsTab) -> Color,
    tabContent: @Composable (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val accent = accentOf(tab)
    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        PrimaryScrollableTabRow(
            selectedTabIndex = tab.ordinal,
            edgePadding = 16.dp,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(tab.ordinal, matchContentSize = true),
                    width = Dp.Unspecified,
                    color = accent,
                )
            },
        ) {
            SettingsTab.entries.forEach { option ->
                Tab(
                    selected = tab == option,
                    onClick = { tab = option },
                    text = { Text(option.label) },
                    selectedContentColor = accentOf(option),
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("settings_tab_${option.name.lowercase()}"),
                )
            }
        }
        if (tab != SettingsTab.GENERAL) {
            Text(
                HELPERS_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp).testTag("helpers_note"),
            )
        }
        AccentColors(accent) {
            Box(
                Modifier
                    .weight(1f)
                    .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.1f), Color.Transparent))),
            ) {
                if (tab == SettingsTab.GENERAL) {
                    GeneralSettings(state, onThemeChange, onDeleteAccount)
                } else {
                    tabContent(tab)
                }
            }
        }
    }
}

@Composable
private fun GeneralSettings(
    state: AppSettingsUiState,
    onThemeChange: (ThemeMode) -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = state.theme
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
        if (state.signedIn) {
            AccountSettings(state.deleteAccount, onDeleteAccount)
        }
    }
}

@Composable
private fun AccountSettings(
    status: DeleteAccountStatus,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (confirming) {
        DeleteAccountDialog(
            onConfirm = {
                confirming = false
                onDeleteAccount()
            },
            onDismiss = { confirming = false },
        )
    }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { confirming = true },
                enabled = status != DeleteAccountStatus.DELETING,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("delete_account"),
            ) {
                Text(
                    if (status == DeleteAccountStatus.DELETING) {
                        "Deleting account…"
                    } else {
                        "Delete account"
                    },
                )
            }
            if (status == DeleteAccountStatus.FAILED) {
                Text(
                    "Couldn't delete your account. Check your connection and try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("delete_account_error"),
                )
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = {
            Text(
                "Your synced scores, history and settings are permanently deleted from the server, " +
                    "and you are signed out. Games and settings on this phone stay.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("delete_account_confirm")) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("delete_account_cancel")) {
                Text("Cancel")
            }
        },
        modifier = modifier.dialogTestTags(),
    )
}

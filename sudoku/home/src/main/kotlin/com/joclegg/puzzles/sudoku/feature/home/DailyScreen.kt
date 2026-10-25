package com.joclegg.puzzles.sudoku.feature.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joclegg.puzzles.sudoku.core.data.DailyDay
import com.joclegg.puzzles.sudoku.core.data.DailyStatus
import com.joclegg.puzzles.sudoku.core.data.GameMode
import com.joclegg.puzzles.sudoku.core.data.Medal
import com.joclegg.puzzles.sudoku.core.ui.BackButton
import com.joclegg.puzzles.sudoku.core.ui.MedalBadge
import com.joclegg.puzzles.sudoku.core.ui.MedalColour
import com.joclegg.puzzles.sudoku.core.ui.SudokuIcons
import com.joclegg.puzzles.sudoku.core.ui.dialogTestTags
import com.joclegg.puzzles.sudoku.core.ui.label
import com.joclegg.puzzles.sudoku.engine.Difficulty
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun DailyRoute(
    viewModel: DailyViewModel,
    onOpenGame: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    DailyScreen(
        state = state,
        levelFor = viewModel::levelFor,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onPlay = { mode, date -> scope.launch { onOpenGame(viewModel.play(mode, date)) } },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun DailyScreen(
    state: DailyUiState,
    levelFor: (GameMode, LocalDate) -> Difficulty,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPlay: (GameMode, LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var chosenDate by remember { mutableStateOf<LocalDate?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text("Daily puzzles", style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.testTag("month_previous")) {
                Icon(SudokuIcons.Back, contentDescription = "Previous month")
            }
            Text(
                "${state.month.month.getDisplayName(TextStyle.FULL, locale)} ${state.month.year}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag("month_title"),
            )
            IconButton(onClick = onNextMonth, enabled = state.canGoForward, modifier = Modifier.testTag("month_next")) {
                Icon(SudokuIcons.Forward, contentDescription = "Next month")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        MonthGrid(state, onDayClick = { chosenDate = it })
        Legend()
    }
    chosenDate?.let { date ->
        DailyChoiceDialog(
            date = date,
            day = state.days[date],
            levelFor = levelFor,
            onPlay = { mode ->
                chosenDate = null
                onPlay(mode, date)
            },
            onDismiss = { chosenDate = null },
        )
    }
}

@Composable
private fun MonthGrid(
    state: DailyUiState,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = state.month.atDay(1)
    val leadingBlanks = first.dayOfWeek.value - 1
    val cells = List(leadingBlanks) { null } + (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(date, state.days[date], state.today, onDayClick, Modifier.weight(1f))
                    }
                }
                repeat(DAYS_IN_WEEK - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    day: DailyDay?,
    today: LocalDate,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val future = date > today
    val dayFormat = rememberDayFormat()
    val medal = day?.medal ?: Medal.NONE
    val description =
        when {
            medal == Medal.GOLD -> "gold medal"
            medal == Medal.SILVER -> "silver medal"
            day?.doneLate == true -> "done later"
            else -> "not done"
        }
    Box(
        modifier
            .aspectRatio(1f)
            .then(
                if (date == today) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            ).clickable(enabled = !future) { onClick(date) }
            .testTag(
                if (date == today) {
                    "daily_today"
                } else {
                    "daily_day_$date"
                },
            ).semantics { contentDescription = "${date.format(dayFormat)}, $description" },
        contentAlignment = Alignment.Center,
    ) {
        when (medal) {
            Medal.GOLD -> MedalBadge(MedalColour.GOLD, Modifier.testTag("medal_gold"), size = 30.dp)
            Medal.SILVER -> MedalBadge(MedalColour.SILVER, Modifier.testTag("medal_silver"), size = 30.dp)
            Medal.NONE -> Unit
        }
        Text(
            date.dayOfMonth.toString(),
            color =
                if (future) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        if (day?.doneLate == true) {
            Text("✓", color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendRow({ MedalBadge(MedalColour.GOLD) }, "Gold: both dailies on the day")
        LegendRow({ MedalBadge(MedalColour.SILVER) }, "Silver: one daily on the day")
        LegendRow({ Text("✓", color = MaterialTheme.colorScheme.primary) }, "Done on a later day")
    }
}

@Composable
private fun LegendRow(
    marker: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { marker() }
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DailyChoiceDialog(
    date: LocalDate,
    day: DailyDay?,
    levelFor: (GameMode, LocalDate) -> Difficulty,
    onPlay: (GameMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val dayFormat = rememberDayFormat()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.dialogTestTags().testTag("daily_choice"),
        title = { Text(date.format(dayFormat)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameMode.entries.forEach { mode ->
                    val done = day?.status(mode) ?: DailyStatus.NOT_DONE
                    OutlinedButton(
                        onClick = { onPlay(mode) },
                        modifier = Modifier.fillMaxWidth().testTag("daily_play_${mode.name.lowercase()}"),
                    ) {
                        Text(
                            buildString {
                                append(
                                    if (mode == GameMode.KILLER) {
                                        "Killer"
                                    } else {
                                        "Classic"
                                    },
                                )
                                append(" · ")
                                append(levelFor(mode, date).label())
                                if (done != DailyStatus.NOT_DONE) {
                                    append("  ✓")
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private const val DAYS_IN_WEEK = 7

@Composable
private fun rememberDayFormat(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
}

package com.joclegg.puzzles.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

data class DailyCalendars(
    val todaysMedal: (String) -> Flow<Medal>,
    val onOpen: (String) -> Unit,
)

data class DailyChoice(
    val label: String,
    val done: Boolean,
    val tag: String,
    val onPlay: () -> Unit,
)

val LocalDailyCalendars = staticCompositionLocalOf { DailyCalendars({ flowOf(Medal.NONE) }, {}) }

private const val DAYS_IN_WEEK = 7

@Composable
fun medalColour(medal: Medal): Color {
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return when (medal) {
        Medal.NONE -> MaterialTheme.colorScheme.onSurface
        Medal.BRONZE -> {
            if (light) {
                Color(0xFFA9713F)
            } else {
                Color(0xFFC58A55)
            }
        }

        Medal.SILVER -> {
            if (light) {
                Color(0xFF8E97A3)
            } else {
                Color(0xFFAEB6C1)
            }
        }

        Medal.GOLD -> {
            if (light) {
                Color(0xFFD9A93A)
            } else {
                Color(0xFFE2B33C)
            }
        }

    }
}

@Composable
fun DailyCalendarButton(
    game: String,
    modifier: Modifier = Modifier,
) {
    val calendars = LocalDailyCalendars.current
    val flow = remember(game, calendars) { calendars.todaysMedal(game) }
    val medal by flow.collectAsState(Medal.NONE)
    IconButton(onClick = { calendars.onOpen(game) }, modifier = modifier.testTag("daily_calendar")) {
        Icon(
            MenuIcons.Calendar,
            contentDescription = "Daily calendar",
            tint = medalColour(medal),
            modifier = Modifier.testTag("calendar_medal_${medal.name.lowercase()}"),
        )
    }
}

@Composable
fun DailyCalendarScreen(
    today: LocalDate,
    days: Map<LocalDate, DailyDay>,
    backButton: @Composable () -> Unit,
    choices: DailyChoices?,
    onDayClick: (LocalDate) -> Unit,
    onDismissChoices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            backButton()
            Text("Daily calendar", style = MaterialTheme.typography.headlineMedium)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }, modifier = Modifier.testTag("month_previous")) { Text("‹") }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag("month_title"),
            )
            TextButton(
                onClick = { month = month.plusMonths(1) },
                enabled = month < YearMonth.from(today),
                modifier = Modifier.testTag("month_next"),
            ) { Text("›") }
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
        MonthGrid(month, today, days, onDayClick)
    }
    choices?.let { ChoicesDialog(it, onDismissChoices) }
}

data class DailyChoices(
    val date: LocalDate,
    val items: List<DailyChoice>,
)

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    days: Map<LocalDate, DailyDay>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(date, days[date], today, onDayClick, Modifier.weight(1f))
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
            medal != Medal.NONE -> "${medal.name.lowercase()} medal"
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
        if (medal != Medal.NONE) {
            MedalBadge(medal, Modifier.testTag("medal_${medal.name.lowercase()}"), size = 30.dp)
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
private fun MedalBadge(
    medal: Medal,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Box(
        modifier
            .size(size)
            .background(medalColour(medal), CircleShape)
            .border(size / 8, Color.White.copy(alpha = 0.6f), CircleShape),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoicesDialog(
    choices: DailyChoices,
    onDismiss: () -> Unit,
) {
    val dayFormat = rememberDayFormat()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag("daily_choice"),
        title = { Text(choices.date.format(dayFormat)) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.items.forEach { choice ->
                    OutlinedButton(onClick = choice.onPlay, modifier = Modifier.testTag(choice.tag)) {
                        Text(
                            if (choice.done) {
                                "${choice.label} ✓"
                            } else {
                                choice.label
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun rememberDayFormat(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
}

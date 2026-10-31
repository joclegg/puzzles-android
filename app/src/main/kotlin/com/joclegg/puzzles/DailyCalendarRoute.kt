package com.joclegg.puzzles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import com.joclegg.puzzles.core.ui.DailyCalendarScreen
import com.joclegg.puzzles.core.ui.DailyChoices
import com.joclegg.puzzles.core.ui.dailyDays
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object DailyCalendarRoutes {
    const val ROUTE = "calendar/{tab}"

    fun route(tab: SettingsTab) = "calendar/${tab.name}"
}

@Composable
fun DailyCalendarRoute(
    app: PuzzlesApplication,
    tab: SettingsTab,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val rules = remember(tab) { calendarRules(tab) }
    val flow = remember(tab) { dailyResults(app, tab) }
    val results by flow.collectAsState(emptyList())
    var choices by remember { mutableStateOf<DailyChoices?>(null) }
    val scope = rememberCoroutineScope()
    DailyCalendarScreen(
        today = LocalDate.now(),
        days = dailyDays(results, rules.total, rules.firstMedal, ZoneId.systemDefault()),
        backButton = {
            IconButton(onClick = navController::popBackStack, modifier = Modifier.testTag("back")) {
                Text("←")
            }
        },
        choices = choices,
        onDayClick = { date ->
            scope.launch {
                choices = DailyChoices(date, dailyChoices(dailiesOn(app, tab, date), navController, scope) { choices = null })
            }
        },
        onDismissChoices = { choices = null },
        modifier = modifier.fillMaxSize(),
    )
}

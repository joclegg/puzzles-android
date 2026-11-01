package com.joclegg.puzzles.slideout

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.room.Room
import com.joclegg.puzzles.GameDefinition
import com.joclegg.puzzles.PuzzleGame
import com.joclegg.puzzles.banks
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.slideout.core.data.GameDatabase
import com.joclegg.puzzles.slideout.core.data.GameRepository
import com.joclegg.puzzles.slideout.core.data.PuzzleBank
import com.joclegg.puzzles.slideout.core.data.SettingsRepository
import com.joclegg.puzzles.slideout.ui.LocalSlideOutColors
import com.joclegg.puzzles.slideout.ui.SlideOutTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

private val Context.slideOutSettingsStore by preferencesDataStore(name = "slide_out_settings")

class SlideOutDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "slide_out.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.slideOutSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = slideOutSyncSource(database.gameDao(), settings, settingsStore)
    val bank =
        PuzzleBank { path ->
            context.banks.read(path)
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now

    override val game = PuzzleGame.SLIDE_OUT
    override val homeRoute = SlideOutRoutes.HOME
    override val bankFolder: String? = "slideout"
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(SlideOutRoutes) { builder.slideOutGraph(this@SlideOutDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(SlideOutRoutes) { navController.openSlideOutGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        SlideOutTheme(darkTheme) { accent = LocalSlideOutColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        SlideOutRoutes.SettingsTabContent(this, darkTheme)
    }
}

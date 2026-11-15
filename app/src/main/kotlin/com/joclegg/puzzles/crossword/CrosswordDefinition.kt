package com.joclegg.puzzles.crossword

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
import com.joclegg.puzzles.crossword.core.data.GameDatabase
import com.joclegg.puzzles.crossword.core.data.GameRepository
import com.joclegg.puzzles.crossword.core.data.PuzzleBank
import com.joclegg.puzzles.crossword.core.data.SettingsRepository
import com.joclegg.puzzles.crossword.ui.CrosswordTheme
import com.joclegg.puzzles.crossword.ui.LocalCrosswordColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

private val Context.crosswordSettingsStore by preferencesDataStore(name = "crossword_settings")

class CrosswordDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "crossword.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.crosswordSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = crosswordSyncSource(database.gameDao(), settings, settingsStore)
    val bank =
        PuzzleBank { path ->
            context.banks.read(path)
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now

    override val game = PuzzleGame.CROSSWORD
    override val homeRoute = CrosswordRoutes.HOME
    override val bankFolder: String? = "crossword"
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(CrosswordRoutes) { builder.crosswordGraph(this@CrosswordDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(CrosswordRoutes) { navController.openCrosswordGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        CrosswordTheme(darkTheme) { accent = LocalCrosswordColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        CrosswordRoutes.SettingsTabContent(this, darkTheme)
    }
}

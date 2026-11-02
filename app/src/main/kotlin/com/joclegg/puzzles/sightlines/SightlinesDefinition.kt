package com.joclegg.puzzles.sightlines

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.room.Room
import com.joclegg.puzzles.GameDefinition
import com.joclegg.puzzles.PuzzleGame
import com.joclegg.puzzles.core.sync.StampedPreferences
import com.joclegg.puzzles.sightlines.core.data.GameDatabase
import com.joclegg.puzzles.sightlines.core.data.GameRepository
import com.joclegg.puzzles.sightlines.core.data.SettingsRepository
import com.joclegg.puzzles.sightlines.ui.LocalSightlinesColors
import com.joclegg.puzzles.sightlines.ui.SightlinesTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

private val Context.sightlinesSettingsStore by preferencesDataStore(name = "sightlines_settings")

class SightlinesDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "sightlines.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.sightlinesSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = sightlinesSyncSource(database.gameDao(), settings, settingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now

    override val game = PuzzleGame.SIGHTLINES
    override val homeRoute = SightlinesRoutes.HOME
    override val bankFolder: String? = null
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(SightlinesRoutes) { builder.sightlinesGraph(this@SightlinesDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(SightlinesRoutes) { navController.openSightlinesGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        SightlinesTheme(darkTheme) { accent = LocalSightlinesColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        SightlinesRoutes.SettingsTabContent(this, darkTheme)
    }
}

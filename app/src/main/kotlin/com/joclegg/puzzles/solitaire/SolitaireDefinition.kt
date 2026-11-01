package com.joclegg.puzzles.solitaire

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
import com.joclegg.puzzles.solitaire.core.data.GameDatabase
import com.joclegg.puzzles.solitaire.core.data.GameRepository
import com.joclegg.puzzles.solitaire.core.data.SettingsRepository
import com.joclegg.puzzles.solitaire.core.data.WinnableDeals
import com.joclegg.puzzles.solitaire.core.ui.LocalTableColors
import com.joclegg.puzzles.solitaire.core.ui.SolitaireTheme
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.solitaireSettingsStore by preferencesDataStore(name = "solitaire_settings")

class SolitaireDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "solitaire.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.solitaireSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = solitaireSyncSource(database.gameDao(), settings, settingsStore)
    val winnableDeals =
        WinnableDeals { path ->
            context.banks.read(path)
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now

    override val game = PuzzleGame.SOLITAIRE
    override val homeRoute = SolitaireRoutes.HOME
    override val bankFolder: String? = "solitaire"
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(SolitaireRoutes) { builder.solitaireGraph(this@SolitaireDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(SolitaireRoutes) { navController.openSolitaireGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        SolitaireTheme(darkTheme) { accent = LocalTableColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        SolitaireRoutes.SettingsTabContent(this, darkTheme)
    }
}

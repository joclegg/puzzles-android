package com.joclegg.puzzles.twotone

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
import com.joclegg.puzzles.twotone.core.data.GameDatabase
import com.joclegg.puzzles.twotone.core.data.GameRepository
import com.joclegg.puzzles.twotone.core.data.SettingsRepository
import com.joclegg.puzzles.twotone.ui.LocalTwoToneColors
import com.joclegg.puzzles.twotone.ui.TwoToneTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

private val Context.twotoneSettingsStore by preferencesDataStore(name = "two_tone_settings")

class TwoToneDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "two_tone.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.twotoneSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = twoToneSyncSource(database.gameDao(), settings, settingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now

    override val game = PuzzleGame.TWO_TONE
    override val homeRoute = TwoToneRoutes.HOME
    override val bankFolder: String? = null
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(TwoToneRoutes) { builder.twoToneGraph(this@TwoToneDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(TwoToneRoutes) { navController.openTwoToneGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        TwoToneTheme(darkTheme) { accent = LocalTwoToneColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        TwoToneRoutes.SettingsTabContent(this, darkTheme)
    }
}

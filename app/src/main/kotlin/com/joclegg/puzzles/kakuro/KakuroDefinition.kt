package com.joclegg.puzzles.kakuro

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
import com.joclegg.puzzles.kakuro.core.data.GameDatabase
import com.joclegg.puzzles.kakuro.core.data.GameRepository
import com.joclegg.puzzles.kakuro.core.data.PuzzleBank
import com.joclegg.puzzles.kakuro.core.data.SettingsRepository
import com.joclegg.puzzles.kakuro.ui.KakuroTheme
import com.joclegg.puzzles.kakuro.ui.LocalKakuroColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

private val Context.kakuroSettingsStore by preferencesDataStore(name = "kakuro_settings")

class KakuroDefinition(
    context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "kakuro.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.kakuroSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = kakuroSyncSource(database.gameDao(), settings, settingsStore)
    val bank =
        PuzzleBank { path ->
            context.banks.read(path)
        }
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now

    override val game = PuzzleGame.KAKURO
    override val homeRoute = KakuroRoutes.HOME
    override val bankFolder: String? = "kakuro"
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(KakuroRoutes) { builder.kakuroGraph(this@KakuroDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(KakuroRoutes) { navController.openKakuroGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        KakuroTheme(darkTheme) { accent = LocalKakuroColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        KakuroRoutes.SettingsTabContent(this, darkTheme)
    }
}

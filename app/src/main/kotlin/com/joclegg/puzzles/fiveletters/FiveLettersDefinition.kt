package com.joclegg.puzzles.fiveletters

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
import com.joclegg.puzzles.fiveletters.core.data.Daily2WordSource
import com.joclegg.puzzles.fiveletters.core.data.GameDatabase
import com.joclegg.puzzles.fiveletters.core.data.GameRepository
import com.joclegg.puzzles.fiveletters.core.data.SettingsRepository
import com.joclegg.puzzles.fiveletters.core.ui.FiveLettersTheme
import com.joclegg.puzzles.fiveletters.core.ui.LocalLetterColors
import com.joclegg.puzzles.fiveletters.engine.Words
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.fiveLettersSettingsStore by preferencesDataStore(name = "five_letters_settings")

class FiveLettersDefinition(
    private val context: Context,
) : GameDefinition {
    private val database =
        Room
            .databaseBuilder(context, GameDatabase::class.java, "five_letters.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    val games = GameRepository(database.gameDao())
    private val settingsStore = StampedPreferences(context.fiveLettersSettingsStore)
    val settings = SettingsRepository(settingsStore)
    override val sync = fiveLettersSyncSource(database.gameDao(), settings, settingsStore)
    val newSeed: () -> Long = { Random.nextLong() }
    val today: () -> LocalDate = LocalDate::now
    val daily2Words = Daily2WordSource()

    private val loading = Mutex()
    private var loaded: Words? = null

    suspend fun words(): Words =
        loading.withLock {
            loaded ?: withContext(Dispatchers.IO) { Words(read("words/answers.txt"), read("words/guesses.txt")) }.also { loaded = it }
        }

    private fun read(path: String): List<String> = context.banks.read(path).lines()

    override val game = PuzzleGame.FIVE_LETTERS
    override val homeRoute = FiveLettersRoutes.HOME
    override val bankFolder: String? = "words"
    override val inProgressCount: Flow<Int> = games.inProgress.map { it.size }

    override suspend fun keepLegacyDefaults() = settings.keepLegacyDefaults()

    override fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    ) {
        with(FiveLettersRoutes) { builder.fiveLettersGraph(this@FiveLettersDefinition, navController, darkTheme) }
    }

    override fun openGame(
        navController: NavHostController,
        id: Long,
    ) {
        with(FiveLettersRoutes) { navController.openFiveLettersGame(id) }
    }

    @Composable
    override fun accent(darkTheme: Boolean): Color {
        var accent = Color.Unspecified
        FiveLettersTheme(darkTheme) { accent = LocalLetterColors.current.accent }
        return accent
    }

    @Composable
    override fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    ) {
        FiveLettersRoutes.SettingsTabContent(this, darkTheme)
    }
}

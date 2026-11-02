package com.joclegg.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.joclegg.puzzles.core.sync.SyncSource
import kotlinx.coroutines.flow.Flow

interface GameDefinition {
    val game: PuzzleGame
    val homeRoute: String
    val bankFolder: String?
    val sync: SyncSource
    val inProgressCount: Flow<Int>

    suspend fun keepLegacyDefaults()

    fun registerGraph(
        builder: NavGraphBuilder,
        navController: NavHostController,
        darkTheme: Boolean,
    )

    fun openGame(
        navController: NavHostController,
        id: Long,
    )

    @Composable
    fun accent(darkTheme: Boolean): Color

    @Composable
    fun SettingsContent(
        darkTheme: Boolean,
        argument: String?,
    )
}

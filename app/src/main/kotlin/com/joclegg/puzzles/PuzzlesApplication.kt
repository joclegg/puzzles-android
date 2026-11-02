package com.joclegg.puzzles

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.preferencesDataStore
import com.joclegg.puzzles.core.sync.BankStore
import com.joclegg.puzzles.core.sync.SupabaseAuth
import com.joclegg.puzzles.core.sync.SyncEngine
import com.joclegg.puzzles.core.sync.SyncStore
import com.joclegg.puzzles.core.ui.Medal
import com.joclegg.puzzles.fiveletters.FiveLettersDefinition
import com.joclegg.puzzles.kakuro.KakuroDefinition
import com.joclegg.puzzles.sightlines.SightlinesDefinition
import com.joclegg.puzzles.slideout.SlideOutDefinition
import com.joclegg.puzzles.solitaire.SolitaireDefinition
import com.joclegg.puzzles.sudoku.SudokuDefinition
import com.joclegg.puzzles.twotone.TwoToneDefinition
import com.joclegg.puzzles.wordwheel.WordWheelDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

private val Context.syncPreferences by preferencesDataStore(name = "sync")

class PuzzlesApplication : Application() {
    val sudoku by lazy { SudokuDefinition(this) }
    val solitaire by lazy { SolitaireDefinition(this) }
    val twoTone by lazy { TwoToneDefinition(this) }
    val sightlines by lazy { SightlinesDefinition(this) }
    val fiveLetters by lazy { FiveLettersDefinition(this) }
    val kakuro by lazy { KakuroDefinition(this) }
    val slideOut by lazy { SlideOutDefinition(this) }
    val wordWheel by lazy { WordWheelDefinition(this) }
    val games: List<GameDefinition> by lazy { listOf(sudoku, solitaire, twoTone, sightlines, fiveLetters, kakuro, slideOut, wordWheel) }

    fun definition(game: PuzzleGame): GameDefinition = games.first { it.game == game }

    private val syncStore by lazy { SyncStore(syncPreferences) }
    val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val auth by lazy { SupabaseAuth(syncStore) }
    val sync by lazy {
        SyncEngine(
            sources = games.map { it.sync },
            auth = auth,
            store = syncStore,
        )
    }
    val bankStore by lazy { BankStore(File(filesDir, BANKS_FOLDER)) }
    private val _bankStatus = MutableStateFlow(BankStatus())
    val bankStatus: StateFlow<BankStatus> = _bankStatus.asStateFlow()

    fun updateBanks() {
        if (_bankStatus.value.updating) {
            return
        }
        _bankStatus.update { it.copy(updating = true) }
        background.launch {
            bankStore.update()
            _bankStatus.update { BankStatus(updating = false, checks = it.checks + 1) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        background.launch { bankStore.refresh() }
        val meta = getSharedPreferences(APP_META, MODE_PRIVATE)
        if (!meta.getBoolean(HELPER_DEFAULTS_CHECKED, false)) {
            val existingUser = File(filesDir, BANKS_FOLDER).exists() || databaseList().isNotEmpty()
            meta.edit { putBoolean(HELPER_DEFAULTS_CHECKED, true) }
            if (existingUser) {
                background.launch { keepLegacyHelperDefaults() }
            }
        }
    }

    suspend fun keepLegacyHelperDefaults() {
        games.forEach { it.keepLegacyDefaults() }
    }

    private val celebratedOn by lazy { MutableStateFlow(getSharedPreferences(APP_META, MODE_PRIVATE).getString(CELEBRATED_ON, null)) }

    fun celebrateAllGolds(medals: Flow<Map<PuzzleGame, Medal>>): Flow<Boolean> =
        combine(medals, celebratedOn) { today, celebrated ->
            holdsEveryGold(today) && celebrated != LocalDate.now().toString()
        }

    fun markAllGoldsCelebrated() {
        val today = LocalDate.now().toString()
        celebratedOn.value = today
        getSharedPreferences(APP_META, MODE_PRIVATE).edit { putString(CELEBRATED_ON, today) }
    }

    val openGameCount: Flow<Int> by lazy {
        combine(games.map { it.inProgressCount }) { counts -> counts.sum() }.distinctUntilChanged()
    }
}

data class BankStatus(
    val updating: Boolean = false,
    val checks: Int = 0,
)

val Context.banks: BankStore
    get() = (applicationContext as PuzzlesApplication).bankStore

fun Context.installSeedBanks() {
    val manifest = runCatching { assets.open(SEED_MANIFEST) }.getOrNull()
    if (manifest != null && !banks.isReady) {
        banks.install(manifest.use { String(it.readBytes()) }) { path ->
            assets.open("puzzles-data/$path").use { it.readBytes() }
        }
    }
}

private const val APP_META = "app_meta"
private const val HELPER_DEFAULTS_CHECKED = "helper_defaults_checked"
private const val CELEBRATED_ON = "all_golds_celebrated_on"
private const val BANKS_FOLDER = "puzzles-data"
private const val SEED_MANIFEST = "puzzles-data/manifest.json"

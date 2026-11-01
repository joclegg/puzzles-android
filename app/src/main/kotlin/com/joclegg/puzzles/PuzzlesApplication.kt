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
import com.joclegg.puzzles.fiveletters.FiveLettersContainer
import com.joclegg.puzzles.kakuro.KakuroContainer
import com.joclegg.puzzles.sightlines.SightlinesContainer
import com.joclegg.puzzles.slideout.SlideOutContainer
import com.joclegg.puzzles.solitaire.SolitaireContainer
import com.joclegg.puzzles.sudoku.SudokuContainer
import com.joclegg.puzzles.twotone.TwoToneContainer
import com.joclegg.puzzles.wordwheel.WordWheelContainer
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
    val sudoku by lazy { SudokuContainer(this) }
    val solitaire by lazy { SolitaireContainer(this) }
    val twoTone by lazy { TwoToneContainer(this) }
    val sightlines by lazy { SightlinesContainer(this) }
    val fiveLetters by lazy { FiveLettersContainer(this) }
    val kakuro by lazy { KakuroContainer(this) }
    val slideOut by lazy { SlideOutContainer(this) }
    val wordWheel by lazy { WordWheelContainer(this) }
    private val syncStore by lazy { SyncStore(syncPreferences) }
    val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val auth by lazy { SupabaseAuth(syncStore) }
    val sync by lazy {
        SyncEngine(
            sources =
                listOf(
                    sudoku.sync,
                    solitaire.sync,
                    twoTone.sync,
                    sightlines.sync,
                    fiveLetters.sync,
                    kakuro.sync,
                    slideOut.sync,
                    wordWheel.sync,
                ),
            auth = auth,
            store = syncStore,
        )
    }
    private val _bankStatus = MutableStateFlow(BankStatus())
    val bankStatus: StateFlow<BankStatus> = _bankStatus.asStateFlow()

    fun updateBanks() {
        if (_bankStatus.value.updating) {
            return
        }
        _bankStatus.update { it.copy(updating = true) }
        background.launch {
            banks.update()
            _bankStatus.update { BankStatus(updating = false, checks = it.checks + 1) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val meta = getSharedPreferences(APP_META, MODE_PRIVATE)
        if (!meta.getBoolean(HELPER_DEFAULTS_CHECKED, false)) {
            val existingUser = File(filesDir, "puzzles-data").exists() || databaseList().isNotEmpty()
            meta.edit { putBoolean(HELPER_DEFAULTS_CHECKED, true) }
            if (existingUser) {
                background.launch { keepLegacyHelperDefaults() }
            }
        }
    }

    suspend fun keepLegacyHelperDefaults() {
        sudoku.settings.keepLegacyDefaults()
        solitaire.settings.keepLegacyDefaults()
        twoTone.settings.keepLegacyDefaults()
        sightlines.settings.keepLegacyDefaults()
        fiveLetters.settings.keepLegacyDefaults()
        kakuro.settings.keepLegacyDefaults()
        slideOut.settings.keepLegacyDefaults()
        wordWheel.settings.keepLegacyDefaults()
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
        combine(
            sudoku.games.inProgress.map { it.size },
            solitaire.games.inProgress.map { it.size },
            twoTone.games.inProgress.map { it.size },
            sightlines.games.inProgress.map { it.size },
            fiveLetters.games.inProgress.map { it.size },
            kakuro.games.inProgress.map { it.size },
            slideOut.games.inProgress.map { it.size },
            wordWheel.games.inProgress.map { it.size },
        ) { counts -> counts.sum() }.distinctUntilChanged()
    }
}

data class BankStatus(
    val updating: Boolean = false,
    val checks: Int = 0,
)

val Context.banks: BankStore
    get() = BankStore(File(filesDir, "puzzles-data"))

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
private const val SEED_MANIFEST = "puzzles-data/manifest.json"

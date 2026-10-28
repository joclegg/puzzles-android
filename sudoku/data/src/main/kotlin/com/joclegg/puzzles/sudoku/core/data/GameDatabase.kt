package com.joclegg.puzzles.sudoku.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "games", indices = [Index("uuid", unique = true), Index("updatedAt"), Index("status"), Index("puzzleId")])
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val updatedAt: Long,
    val puzzleId: String,
    val mode: String,
    val difficulty: String?,
    val custom: Boolean,
    val puzzle: String,
    val cellValues: String,
    val notes: String,
    val elapsedMs: Long,
    val mistakes: Int,
    val mistakeLevel: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val undoHistory: String,
)

@Entity(tableName = "past_bests", primaryKeys = ["mode", "difficulty"])
data class PastBestEntity(
    val mode: String,
    val difficulty: String,
    val bestMs: Long,
)

data class BestTimeRow(
    val mode: String,
    val difficulty: String,
    val bestMs: Long,
    val averageMs: Double,
    val completed: Int,
)

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("SELECT uuid FROM games WHERE id = :id")
    suspend fun uuidOf(id: Long): String?

    @Query("SELECT * FROM games WHERE uuid = :uuid")
    suspend fun byUuid(uuid: String): GameEntity?

    @Query("SELECT * FROM games WHERE updatedAt > :since")
    suspend fun changedSince(since: Long): List<GameEntity>

    @Query("DELETE FROM games WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    @Transaction
    suspend fun save(game: GameEntity) {
        update(game.copy(uuid = uuidOf(game.id) ?: game.uuid))
    }

    @Transaction
    suspend fun importGame(game: GameEntity) {
        val existing = byUuid(game.uuid)
        if (existing == null) {
            insert(game.copy(id = 0))
        } else {
            update(game.copy(id = existing.id))
        }
    }

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun get(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC, id DESC")
    fun observeInProgress(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS'")
    suspend fun inProgress(): List<GameEntity>

    @Query("SELECT * FROM games ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<GameEntity>>

    @Query("SELECT puzzleId FROM games WHERE mode = :mode")
    suspend fun playedPuzzleIds(mode: String): List<String>

    @Query(
        """
        SELECT mode, difficulty, MIN(elapsedMs) AS bestMs, AVG(elapsedMs) AS averageMs, COUNT(*) AS completed
        FROM games
        WHERE status = 'COMPLETED' AND custom = 0 AND difficulty IS NOT NULL
        GROUP BY mode, difficulty
        """,
    )
    fun observeBestTimes(): Flow<List<BestTimeRow>>

    @Query(
        """
        SELECT MIN(elapsedMs) FROM games
        WHERE status = 'COMPLETED' AND custom = 0 AND mode = :mode AND difficulty = :difficulty AND id != :excludeId
        """,
    )
    suspend fun bestTimeExcluding(
        mode: String,
        difficulty: String,
        excludeId: Long,
    ): Long?

    @Upsert
    suspend fun upsertPastBest(pastBest: PastBestEntity)

    @Query("SELECT * FROM past_bests")
    fun observePastBests(): Flow<List<PastBestEntity>>

    @Query("SELECT bestMs FROM past_bests WHERE mode = :mode AND difficulty = :difficulty")
    suspend fun pastBest(
        mode: String,
        difficulty: String,
    ): Long?
}

@Entity(tableName = "generated_puzzles", indices = [Index("mode", "difficulty", "used")])
data class GeneratedPuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val difficulty: String,
    val puzzle: String,
    val createdAt: Long,
    val used: Boolean = false,
)

@Dao
interface GeneratedPuzzleDao {
    @Insert
    suspend fun insert(puzzle: GeneratedPuzzleEntity): Long

    @Query("SELECT COUNT(*) FROM generated_puzzles WHERE mode = :mode AND difficulty = :difficulty AND used = 0")
    suspend fun unusedCount(
        mode: String,
        difficulty: String,
    ): Int

    @Query("SELECT * FROM generated_puzzles WHERE mode = :mode AND difficulty = :difficulty AND used = 0 ORDER BY id LIMIT 1")
    suspend fun firstUnused(
        mode: String,
        difficulty: String,
    ): GeneratedPuzzleEntity?

    @Query("UPDATE generated_puzzles SET used = 1 WHERE id = :id")
    suspend fun markUsed(id: Long)

    @Transaction
    suspend fun takeUnused(
        mode: String,
        difficulty: String,
    ): GeneratedPuzzleEntity? = firstUnused(mode, difficulty)?.also { markUsed(it.id) }
}

@Database(
    entities = [GameEntity::class, GeneratedPuzzleEntity::class, PastBestEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun generatedPuzzleDao(): GeneratedPuzzleDao
}

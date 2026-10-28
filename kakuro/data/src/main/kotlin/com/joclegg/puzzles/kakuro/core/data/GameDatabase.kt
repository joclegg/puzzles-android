package com.joclegg.puzzles.kakuro.core.data

import androidx.room.ColumnInfo
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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "games", indices = [Index("uuid", unique = true), Index("updatedAt"), Index("status")])
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val updatedAt: Long,
    val puzzleId: String,
    val size: Int,
    val puzzle: String,
    val changes: String,
    val elapsedMs: Long,
    val hints: Int,
    @ColumnInfo(defaultValue = "0") val mistakes: Int,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
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

    @Query("SELECT * FROM games ORDER BY startedAt DESC, id DESC")
    fun observeHistory(): Flow<List<GameEntity>>

    @Query("SELECT MIN(elapsedMs) FROM games WHERE status = 'SOLVED' AND size = :size AND id != :excludeId")
    suspend fun bestTimeExcluding(
        size: Int,
        excludeId: Long,
    ): Long?
}

@Database(
    entities = [GameEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

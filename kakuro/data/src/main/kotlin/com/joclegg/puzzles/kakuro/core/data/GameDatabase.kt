package com.joclegg.puzzles.kakuro.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "games", indices = [Index("status")])
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleId: String,
    val size: Int,
    val puzzle: String,
    val changes: String,
    val elapsedMs: Long,
    val hints: Int,
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

@Database(entities = [GameEntity::class], version = 1, exportSchema = true)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

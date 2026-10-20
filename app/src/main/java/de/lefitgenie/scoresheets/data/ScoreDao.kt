package de.lefitgenie.scoresheets.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId")
    fun observeEntries(sessionId: Long): Flow<List<EntryEntity>>

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Upsert
    suspend fun upsertEntry(entry: EntryEntity)

    @Query(
        "DELETE FROM entries WHERE sessionId = :sessionId AND rowIndex = :rowIndex " +
            "AND fieldIndex = :fieldIndex AND playerIndex = :playerIndex",
    )
    suspend fun deleteEntry(sessionId: Long, rowIndex: Int, fieldIndex: Int, playerIndex: Int)

    @Query("UPDATE sessions SET dealerIndex = :dealerIndex, updatedAt = :now WHERE id = :id")
    suspend fun setDealer(id: Long, dealerIndex: Int, now: Long)

    @Query("UPDATE sessions SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("SELECT * FROM saved_players ORDER BY name COLLATE NOCASE")
    fun observeSavedPlayers(): Flow<List<SavedPlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedPlayer(player: SavedPlayerEntity)

    @Delete
    suspend fun deleteSavedPlayer(player: SavedPlayerEntity)
}

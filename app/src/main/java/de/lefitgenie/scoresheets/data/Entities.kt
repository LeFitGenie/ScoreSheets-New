package de.lefitgenie.scoresheets.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val PLAYER_SEPARATOR = "\n"

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameType: String,
    /** Spielernamen in Reihenfolge, getrennt durch [PLAYER_SEPARATOR]. */
    val players: String,
    val dealerIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val playerNames: List<String> get() = players.split(PLAYER_SEPARATOR)
}

@Entity(
    tableName = "entries",
    primaryKeys = ["sessionId", "rowIndex", "fieldIndex", "playerIndex"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class EntryEntity(
    val sessionId: Long,
    val rowIndex: Int,
    val fieldIndex: Int,
    val playerIndex: Int,
    val amount: Int,
)

@Entity(tableName = "saved_players")
data class SavedPlayerEntity(@PrimaryKey val name: String)

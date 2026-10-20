package de.lefitgenie.scoresheets.data

import de.lefitgenie.scoresheets.game.Cell
import de.lefitgenie.scoresheets.game.Entries
import de.lefitgenie.scoresheets.game.GameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoreRepository(private val dao: ScoreDao) {
    val sessions: Flow<List<SessionEntity>> = dao.observeSessions()

    val savedPlayers: Flow<List<String>> =
        dao.observeSavedPlayers().map { list -> list.map { it.name } }

    fun session(id: Long): Flow<SessionEntity?> = dao.observeSession(id)

    fun entries(id: Long): Flow<Entries> = dao.observeEntries(id).map { list ->
        Entries(list.associate { Cell(it.rowIndex, it.fieldIndex, it.playerIndex) to it.amount })
    }

    suspend fun createSession(type: GameType, players: List<String>): Long {
        val now = System.currentTimeMillis()
        val id = dao.insertSession(
            SessionEntity(
                gameType = type.id,
                players = players.joinToString(PLAYER_SEPARATOR),
                dealerIndex = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        players.forEach { dao.insertSavedPlayer(SavedPlayerEntity(it)) }
        return id
    }

    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    /** Speichert die Felder einer Zelle; ein null-Wert löscht das jeweilige Feld. */
    suspend fun setCell(sessionId: Long, row: Int, player: Int, values: Map<Int, Int?>) {
        for ((field, value) in values) {
            if (value == null) {
                dao.deleteEntry(sessionId, row, field, player)
            } else {
                dao.upsertEntry(EntryEntity(sessionId, row, field, player, value))
            }
        }
        dao.touch(sessionId, System.currentTimeMillis())
    }

    suspend fun setDealer(sessionId: Long, dealerIndex: Int) =
        dao.setDealer(sessionId, dealerIndex, System.currentTimeMillis())

    suspend fun forgetPlayer(name: String) = dao.deleteSavedPlayer(SavedPlayerEntity(name))
}

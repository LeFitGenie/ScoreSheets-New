package de.lefitgenie.scoresheets.game

/**
 * Die unterstützten Spiele. [id] wird in der Datenbank gespeichert und darf sich nie ändern.
 */
enum class GameType(val id: String, val minPlayers: Int, val maxPlayers: Int) {
    RUMMY("rummy", 2, 8),
    SIXTY_SIX("sixty_six", 2, 4),
    PIRATES("pirates", 2, 8),
    TRICK_BIDDING("trick_bidding", 3, 6),
    PASCH("pasch", 1, 8),
    FREE("free", 1, 8),
    ;

    companion object {
        fun fromId(id: String): GameType = entries.firstOrNull { it.id == id } ?: FREE
    }
}

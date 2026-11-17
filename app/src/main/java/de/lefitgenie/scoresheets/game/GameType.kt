package de.lefitgenie.scoresheets.game

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Die unterstützten Spiele. [id] wird in der Datenbank gespeichert und darf sich nie ändern.
 */
enum class GameType(
    val id: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    RUMMY(
        "rummy", 2, 8,
        R.string.game_rummy,
        R.drawable.ic_rummy
    ),
    SIXTY_SIX(
        "sixty_six", 2, 4,
        R.string.game_sixty_six,
        R.drawable.ic_sixty_six
    ),
    PIRATES(
        "pirates", 2, 8,
        R.string.game_pirates,
        R.drawable.ic_pirates
    ),
    TRICK_BIDDING(
        "trick_bidding", 3, 6,
        R.string.game_trick_bidding,
        R.drawable.ic_trick_bidding
    ),
    PASCH(
        "pasch", 1, 8,
        R.string.game_pasch,
        R.drawable.ic_pasch
    ),
    FREE(
        "free", 1, 8,
        R.string.game_free,
        R.drawable.ic_free
    );

    companion object {
        fun fromId(id: String): GameType =
            entries.firstOrNull { it.id == id } ?: FREE
    }
}

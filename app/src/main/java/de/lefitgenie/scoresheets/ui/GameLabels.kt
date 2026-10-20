package de.lefitgenie.scoresheets.ui

import androidx.annotation.StringRes
import de.lefitgenie.scoresheets.R
import de.lefitgenie.scoresheets.game.FieldKind
import de.lefitgenie.scoresheets.game.GameType
import de.lefitgenie.scoresheets.game.PaschCategory

@StringRes
fun GameType.titleRes(): Int = when (this) {
    GameType.RUMMY -> R.string.game_rummy
    GameType.SIXTY_SIX -> R.string.game_sixty_six
    GameType.PIRATES -> R.string.game_pirates
    GameType.TRICK_BIDDING -> R.string.game_trick_bidding
    GameType.PASCH -> R.string.game_pasch
    GameType.FREE -> R.string.game_free
}

@StringRes
fun FieldKind.labelRes(): Int = when (this) {
    FieldKind.SCORE -> R.string.field_points
    FieldKind.BID -> R.string.field_bid
    FieldKind.TRICKS -> R.string.field_tricks
    FieldKind.BONUS -> R.string.field_bonus
    FieldKind.COUNT -> R.string.field_count
}

@StringRes
fun PaschCategory.labelRes(): Int = when (this) {
    PaschCategory.ONES -> R.string.pasch_ones
    PaschCategory.TWOS -> R.string.pasch_twos
    PaschCategory.THREES -> R.string.pasch_threes
    PaschCategory.FOURS -> R.string.pasch_fours
    PaschCategory.FIVES -> R.string.pasch_fives
    PaschCategory.SIXES -> R.string.pasch_sixes
    PaschCategory.UPPER_BONUS -> R.string.pasch_upper_bonus
    PaschCategory.THREE_KIND -> R.string.pasch_three_kind
    PaschCategory.FOUR_KIND -> R.string.pasch_four_kind
    PaschCategory.FULL_HOUSE -> R.string.pasch_full_house
    PaschCategory.SMALL_STRAIGHT -> R.string.pasch_small_straight
    PaschCategory.LARGE_STRAIGHT -> R.string.pasch_large_straight
    PaschCategory.FIVE_KIND -> R.string.pasch_five_kind
    PaschCategory.CHANCE -> R.string.pasch_chance
    PaschCategory.EXTRA_BONUS -> R.string.pasch_extra_bonus
}

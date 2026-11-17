package de.lefitgenie.scoresheets.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    private fun cell(row: Int, field: Int, player: Int, value: Int) =
        Cell(row, field, player) to value

    @Test
    fun pirates_exactBidScoresTwentyPerTrick() {
        assertEquals(40, PiratesRules.score(round = 3, bid = 2, tricks = 2, bonus = 0))
    }

    @Test
    fun pirates_bonusOnlyCountsWhenBidIsExact() {
        assertEquals(50, PiratesRules.score(round = 2, bid = 1, tricks = 1, bonus = 30))
        assertEquals(-10, PiratesRules.score(round = 2, bid = 1, tricks = 2, bonus = 30))
    }

    @Test
    fun pirates_zeroBidScalesWithRound() {
        assertEquals(40, PiratesRules.score(round = 4, bid = 0, tricks = 0, bonus = 0))
        assertEquals(-40, PiratesRules.score(round = 4, bid = 0, tricks = 1, bonus = 0))
    }

    @Test
    fun pirates_missedBidCostsTenPerDifference() {
        assertEquals(-20, PiratesRules.score(round = 5, bid = 3, tricks = 1, bonus = 0))
    }

    @Test
    fun pirates_rowIsIncompleteWithoutBidAndTricks() {
        val entries = entriesOf(cell(0, 0, 0, 1))
        assertNull(PiratesRules.rowScore(0, 0, 2, entries))
    }

    @Test
    fun trickBidding_scores() {
        assertEquals(40, TrickBiddingRules.score(bid = 2, tricks = 2))
        assertEquals(20, TrickBiddingRules.score(bid = 0, tricks = 0))
        assertEquals(-20, TrickBiddingRules.score(bid = 1, tricks = 3))
    }

    @Test
    fun trickBidding_roundCountDependsOnPlayers() {
        val empty = Entries()
        assertEquals(20, TrickBiddingRules.rowCount(3, empty))
        assertEquals(15, TrickBiddingRules.rowCount(4, empty))
        assertEquals(12, TrickBiddingRules.rowCount(5, empty))
        assertEquals(10, TrickBiddingRules.rowCount(6, empty))
    }

    @Test
    fun pasch_upperBonusAtSixtyThree() {
        val entries = entriesOf(
            cell(0, 0, 0, 3), cell(1, 0, 0, 6), cell(2, 0, 0, 9),
            cell(3, 0, 0, 12), cell(4, 0, 0, 15), cell(5, 0, 0, 18),
        )
        val bonusRow = PaschCategory.UPPER_BONUS.ordinal
        assertEquals(35, PaschRules.rowScore(bonusRow, 0, 1, entries))
        assertEquals(63 + 35, PaschRules.total(0, 1, entries))
    }

    @Test
    fun pasch_noBonusBelowThresholdOnceUpperPartIsComplete() {
        val entries = entriesOf(
            cell(0, 0, 0, 1), cell(1, 0, 0, 2), cell(2, 0, 0, 3),
            cell(3, 0, 0, 4), cell(4, 0, 0, 5), cell(5, 0, 0, 6),
        )
        val bonusRow = PaschCategory.UPPER_BONUS.ordinal
        assertEquals(0, PaschRules.rowScore(bonusRow, 0, 1, entries))
    }

    @Test
    fun pasch_bonusRowStaysEmptyWhileUpperPartIsIncomplete() {
        val entries = entriesOf(cell(0, 0, 0, 3))
        assertNull(PaschRules.rowScore(PaschCategory.UPPER_BONUS.ordinal, 0, 1, entries))
    }

    @Test
    fun pasch_extraFivesAreWorthOneHundredEach() {
        val extraRow = PaschCategory.EXTRA_BONUS.ordinal
        val entries = entriesOf(cell(extraRow, 0, 0, 2))
        assertEquals(200, PaschRules.rowScore(extraRow, 0, 1, entries))
    }

    @Test
    fun pasch_bonusRowIsNotEditable() {
        assertFalse(PaschRules.fieldType(PaschCategory.UPPER_BONUS.ordinal) != FieldType.DEFAULT)
        assertTrue(PaschRules.fieldType(PaschCategory.CHANCE.ordinal) == FieldType.DEFAULT)
    }

    @Test
    fun openEnded_alwaysOffersOneEmptyRow() {
        assertEquals(1, RummyRules.rowCount(2, Entries()))
        assertEquals(4, RummyRules.rowCount(2, entriesOf(cell(2, 0, 1, 10))))
    }

    @Test
    fun rummy_sumsAndLowestWins() {
        val entries = entriesOf(
            cell(0, 0, 0, 10), cell(0, 0, 1, 0),
            cell(1, 0, 0, -5), cell(1, 0, 1, 30),
        )
        val totals = listOf(RummyRules.total(0, 2, entries), RummyRules.total(1, 2, entries))
        assertEquals(listOf(5, 30), totals)
        assertEquals(setOf(0), leaders(totals, RummyRules.lowestWins, hasEntries = true))
    }

    @Test
    fun sixtySix_hasTargetOfSeven() {
        assertEquals(7, SixtySixRules.targetScore)
        assertEquals(3, SixtySixRules.maxValue(0, 0))
    }

    @Test
    fun leaders_returnsAllTiedPlayersAndNothingForEmptySheet() {
        assertEquals(setOf(1, 2), leaders(listOf(3, 5, 5), lowestWins = false, hasEntries = true))
        assertTrue(leaders(listOf(0, 0), lowestWins = false, hasEntries = false).isEmpty())
    }

    @Test
    fun gameTypeIds_roundTripAndUnknownFallsBackToFree() {
        GameType.entries.forEach { assertEquals(it, GameType.fromId(it.id)) }
        assertEquals(GameType.FREE, GameType.fromId("unbekannt"))
    }

    @Test
    fun everyGameTypeHasRules() {
        GameType.entries.forEach { assertEquals(it, rulesFor(it).type) }
    }
}

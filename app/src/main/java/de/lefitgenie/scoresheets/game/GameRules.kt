package de.lefitgenie.scoresheets.game

import kotlin.math.abs

/** Art eines Eingabefelds; die Oberfläche ordnet jedem Eintrag eine Beschriftung zu. */
enum class FieldKind { SCORE, BID, TRICKS, BONUS, COUNT }

/** Zeilen des Pasch-Blocks, in der Reihenfolge wie auf dem Papierblock. */
enum class PaschCategory {
    ONES, TWOS, THREES, FOURS, FIVES, SIXES, 
    UPPER_BONUS,
    THREE_KIND, FOUR_KIND, FULL_HOUSE, SMALL_STRAIGHT, LARGE_STRAIGHT, FIVE_KIND, CHANCE,
    EXTRA_BONUS,
}

enum class FieldType {
    DEFAULT, READONLY, SPECIAL
}

enum class OptionType {
    BOOL, NUMBER, STRING
}

data class GameOption(
    val name: String,
    val value: Any,
    val hidden: Boolean = true
) {
    val type: OptionType
        get() = when (value) {
            is Boolean -> OptionType.BOOL
            is Int -> OptionType.NUMBER
            is String -> OptionType.STRING
            else -> throw IllegalArgumentException(
                "Unsupported option type: ${value::class.simpleName}"
            )
        }
}

/**
 * Regeln und Berechnung eines Spiels. Rein funktional und ohne Android-Abhängigkeit,
 * damit alles mit normalen JVM-Tests geprüft werden kann.
 */
interface GameRules {
    val type: GameType

    /** Eingabefelder pro Zelle, der Index entspricht [Cell.field]. */
    val fields: List<FieldKind>

    /** Gesamtpunktzahl, ab der das Spiel gewonnen ist (falls es so etwas gibt). */
    val targetScore: Int? get() = null
    
    private val defaultOptions: List<GameOption>
        get() = listOf(
            GameOption("lowestWins", false),
            GameOption("allowNegative", false)
        )

    val additionalOptions: List<GameOption>
        get() = emptyList()

    val options: List<GameOption>
        get() = (defaultOptions + additionalOptions)
            .associateBy { it.name }
            .values
            .toList()

    private fun option(name: String): Any? =
        options.firstOrNull { it.name == name }?.value 
        
    fun optionBoolean(name: String, default: Boolean = false): Boolean =
        option(name) as? Boolean ?: default

    fun optionInt(name: String, default: Int = 0): Int =
        option(name) as? Int ?: default

    fun optionString(name: String, default: String = ""): String =
        option(name) as? String ?: default        
    
    fun rowCount(playerCount: Int, entries: Entries): Int

    /** Punkte einer Zeile für einen Spieler, oder null solange die Zeile unvollständig ist. */
    fun rowScore(row: Int, player: Int, playerCount: Int, entries: Entries): Int?

    fun subtotal(
        player: Int,
        playerCount: Int,
        entries: Entries,
        rows: List<Int>,
    ): Int? {
        var sum = 0

        for (row in rows) {
            sum += rowScore(row, player, playerCount, entries) ?: 0
        }

        return sum
    }        
        
    fun total(player: Int, playerCount: Int, entries: Entries): Int {
        var sum = 0
        for (row in 0 until rowCount(playerCount, entries)) {
            sum += rowScore(row, player, playerCount, entries) ?: 0
        }
        return sum
    }

    fun fieldType(row: Int): FieldType = FieldType.DEFAULT
    
    fun isValid(row: Int, value: Int?): Boolean = true
	
	fun maxValue(row: Int, field: Int): Int? = null

    fun paschCategory(row: Int): PaschCategory? = null
}

/** Spiele ohne feste Rundenzahl: unten gibt es immer eine leere Zeile für die nächste Runde. */
abstract class OpenEndedRules(override val type: GameType) : GameRules {
    override val fields: List<FieldKind> = listOf(FieldKind.SCORE)

    override fun rowCount(playerCount: Int, entries: Entries): Int = entries.lastRow + 2

    override fun rowScore(row: Int, player: Int, playerCount: Int, entries: Entries): Int? =
        entries[row, 0, player]
}

object RummyRules : OpenEndedRules(GameType.RUMMY) {    
    override val additionalOptions = listOf(        
        GameOption("lowestWins", true)
    )
}

object SixtySixRules : OpenEndedRules(GameType.SIXTY_SIX) {
    override val targetScore: Int = 7

    /** Pro Spiel gibt es 1 bis 3 Punkte; 0 für die Verlierer. */
    override fun maxValue(row: Int, field: Int): Int = 3
}

object FreeRules : OpenEndedRules(GameType.FREE) {    
}

/**
 * Piraten (Stichspiel mit Ansage): 10 Runden, Ansage und Stiche pro Runde plus optionale Bonuspunkte.
 */
object PiratesRules : GameRules {
    override val type: GameType = GameType.PIRATES
    override val fields: List<FieldKind> = listOf(FieldKind.BID, FieldKind.TRICKS, FieldKind.BONUS)

    const val ROUNDS = 10

    override fun rowCount(playerCount: Int, entries: Entries): Int = ROUNDS

    override fun rowScore(row: Int, player: Int, playerCount: Int, entries: Entries): Int? {
        val bid = entries[row, 0, player] ?: return null
        val tricks = entries[row, 1, player] ?: return null
        return score(round = row + 1, bid = bid, tricks = tricks, bonus = entries[row, 2, player] ?: 0)
    }

    override fun maxValue(row: Int, field: Int): Int? = if (field == 2) null else row + 1

    /**
     * Ansage 0: 10 Punkte pro Runde, wenn kein Stich gemacht wurde, sonst 10 Minuspunkte pro Runde.
     * Ansage über 0: genau getroffen gibt 20 pro Stich plus Bonus, sonst 10 Minuspunkte pro Abweichung.
     */
    fun score(round: Int, bid: Int, tricks: Int, bonus: Int): Int = when {
        bid == 0 -> if (tricks == 0) 10 * round else -10 * round
        bid == tricks -> 20 * bid + bonus
        else -> -10 * abs(bid - tricks)
    }
}

/**
 * Stichspiel mit Ansage (3 bis 6 Spieler): 60 geteilt durch Spielerzahl Runden.
 * Richtig angesagt gibt 20 Punkte plus 10 pro Stich, sonst 10 Minuspunkte pro Abweichung.
 */
object TrickBiddingRules : GameRules {
    override val type: GameType = GameType.TRICK_BIDDING
    override val fields: List<FieldKind> = listOf(FieldKind.BID, FieldKind.TRICKS)

    override fun rowCount(playerCount: Int, entries: Entries): Int = 60 / maxOf(playerCount, 1)

    override fun rowScore(row: Int, player: Int, playerCount: Int, entries: Entries): Int? {
        val bid = entries[row, 0, player] ?: return null
        val tricks = entries[row, 1, player] ?: return null
        return score(bid, tricks)
    }

    override fun maxValue(row: Int, field: Int): Int = row + 1

    fun score(bid: Int, tricks: Int): Int =
        if (bid == tricks) 20 + 10 * tricks else -10 * abs(bid - tricks)
}

/** Würfelspiel mit 13 Kategorien, Bonus ab 63 Punkten im oberen Teil und Zusatz-Päschen. */
object PaschRules : GameRules {
    override val type: GameType = GameType.PASCH
    override val fields: List<FieldKind> = listOf(FieldKind.SCORE)

    private val upperRows = 0..5
    const val UPPER_BONUS_THRESHOLD = 63
    const val UPPER_BONUS = 35
    const val EXTRA_BONUS_VALUE = 100

    override fun paschCategory(row: Int): PaschCategory? = PaschCategory.entries.getOrNull(row)

    override fun rowCount(playerCount: Int, entries: Entries): Int = PaschCategory.entries.size

    override fun fieldType(row: Int): FieldType =
        when (paschCategory(row)) {
            PaschCategory.UPPER_BONUS -> FieldType.READONLY

            PaschCategory.FULL_HOUSE,
            PaschCategory.SMALL_STRAIGHT,
            PaschCategory.LARGE_STRAIGHT,
            PaschCategory.FIVE_KIND -> FieldType.SPECIAL

            else -> FieldType.DEFAULT
        }

    override fun rowScore(row: Int, player: Int, playerCount: Int, entries: Entries): Int? =
        when (paschCategory(row)) {
            PaschCategory.UPPER_BONUS -> upperBonus(player, entries)
            PaschCategory.EXTRA_BONUS -> entries[row, 0, player]?.let { it * EXTRA_BONUS_VALUE }
            else -> entries[row, 0, player]
        }
            
    private fun upperBonus(player: Int, entries: Entries): Int? {
        var sum = 0
        var complete = true
        for (row in upperRows) {
            val value = entries[row, 0, player]
            if (value == null) complete = false else sum += value
        }
        return when {
            sum >= UPPER_BONUS_THRESHOLD -> UPPER_BONUS
            complete -> 0
            else -> null
        }
    }
	
    override fun isValid(row: Int, value: Int?): Boolean {
        if (value == null) return false

        return when (paschCategory(row)) {
            PaschCategory.ONES, PaschCategory.TWOS, PaschCategory.THREES,
            PaschCategory.FOURS, PaschCategory.FIVES, PaschCategory.SIXES -> value % (row + 1) == 0
            else -> true
        }
    }
			 
    override fun maxValue(row: Int, field: Int): Int? = when (paschCategory(row)) {
        PaschCategory.ONES, PaschCategory.TWOS, PaschCategory.THREES,
        PaschCategory.FOURS, PaschCategory.FIVES, PaschCategory.SIXES -> 5 * (row + 1)
        PaschCategory.THREE_KIND, PaschCategory.FOUR_KIND, PaschCategory.CHANCE -> 30
        PaschCategory.FULL_HOUSE -> 25
        PaschCategory.SMALL_STRAIGHT -> 30
        PaschCategory.LARGE_STRAIGHT -> 40
        PaschCategory.FIVE_KIND -> 50
        PaschCategory.EXTRA_BONUS -> 9
        else -> null
    }
}

fun rulesFor(type: GameType): GameRules = when (type) {
    GameType.RUMMY -> RummyRules
    GameType.SIXTY_SIX -> SixtySixRules
    GameType.PIRATES -> PiratesRules
    GameType.TRICK_BIDDING -> TrickBiddingRules
    GameType.PASCH -> PaschRules
    GameType.FREE -> FreeRules
}

/** Indizes der Spieler mit der besten Punktzahl. Leer, solange noch nichts eingetragen wurde. */
fun leaders(totals: List<Int>, lowestWins: Boolean, hasEntries: Boolean): Set<Int> {
    if (!hasEntries || totals.isEmpty()) return emptySet()
    val best = if (lowestWins) totals.min() else totals.max()
    return totals.indices.filter { totals[it] == best }.toSet()
}

package de.lefitgenie.scoresheets.game

/** Eine Zelle des Blocks: Zeile, Eingabefeld innerhalb der Zeile und Spieler (jeweils 0-basiert). */
data class Cell(val row: Int, val field: Int, val player: Int)

/** Alle eingetragenen Werte eines Spiels. Unveränderlich, damit Compose sauber vergleichen kann. */
class Entries(private val values: Map<Cell, Int> = emptyMap()) {
    operator fun get(row: Int, field: Int, player: Int): Int? = values[Cell(row, field, player)]

    val isEmpty: Boolean get() = values.isEmpty()

    /** Höchster Zeilenindex mit einem Eintrag, oder -1 wenn noch nichts eingetragen ist. */
    val lastRow: Int get() = values.keys.maxOfOrNull { it.row } ?: -1
}

fun entriesOf(vararg cells: Pair<Cell, Int>): Entries = Entries(mapOf(*cells))

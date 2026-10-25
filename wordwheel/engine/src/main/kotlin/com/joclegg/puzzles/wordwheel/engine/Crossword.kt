package com.joclegg.puzzles.wordwheel.engine

data class Placement(
    val word: String,
    val row: Int,
    val col: Int,
    val across: Boolean,
) {
    val cells: List<Pair<Int, Int>>
        get() =
            word.indices.map { step ->
                if (across) {
                    row to col + step
                } else {
                    row + step to col
                }
            }

    fun encode() =
        "$word@$row,$col," +
            if (across) {
                "A"
            } else {
                "D"
            }

    companion object {
        fun decode(text: String): Placement {
            val (word, position) = text.split('@')
            val (row, col, direction) = position.split(',')
            return Placement(word, row.toInt(), col.toInt(), direction == "A")
        }
    }
}

data class Crossword(
    val placements: List<Placement>,
) {
    val letters: Map<Pair<Int, Int>, Char>
        get() = placements.flatMap { placement -> placement.cells.zip(placement.word.toList()) }.toMap()

    val rows: Int get() = (letters.keys.maxOfOrNull { it.first } ?: -1) + 1

    val cols: Int get() = (letters.keys.maxOfOrNull { it.second } ?: -1) + 1

    val words: Set<String> get() = placements.map { it.word }.toSet()

    fun canPlace(placement: Placement): Boolean {
        val existing = letters
        var crossings = 0
        placement.cells.forEachIndexed { index, cell ->
            val letter = placement.word[index]
            val there = existing[cell]
            if (there != null) {
                if (there != letter || placements.any { it.across == placement.across && cell in it.cells }) {
                    return false
                }
                crossings++
            } else {
                val (row, col) = cell
                val sides =
                    if (placement.across) {
                        listOf(row - 1 to col, row + 1 to col)
                    } else {
                        listOf(row to col - 1, row to col + 1)
                    }
                if (sides.any { it in existing }) {
                    return false
                }
            }
        }
        val before =
            if (placement.across) {
                placement.row to placement.col - 1
            } else {
                placement.row - 1 to placement.col
            }
        val after =
            placement.cells.last().let { (row, col) ->
                if (placement.across) {
                    row to col + 1
                } else {
                    row + 1 to col
                }
            }
        return crossings > 0 && before !in existing && after !in existing && placements.none { it.word == placement.word }
    }

    fun normalised(): Crossword {
        val top = letters.keys.minOf { it.first }
        val left = letters.keys.minOf { it.second }
        return Crossword(placements.map { it.copy(row = it.row - top, col = it.col - left) })
    }

    fun encode() = placements.joinToString("|") { it.encode() }

    companion object {
        fun decode(text: String) = Crossword(text.split('|').map(Placement::decode))
    }
}

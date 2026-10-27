package com.joclegg.puzzles.kakuro.engine

data class Hint(
    val cell: Int,
    val digit: Int,
    val message: String,
)

private val ALL_DIGITS = (1..MAX_DIGIT).fold(0) { mask, digit -> mask or Combinations.bit(digit) }

fun GameState.findHint(): Hint? {
    val wrong = values.indices.firstOrNull { values[it] != 0 && values[it] != puzzle.solution[it] }
    if (wrong != null) {
        return Hint(wrong, puzzle.solution[wrong], "This cell is wrong.")
    }
    val empty =
        puzzle.runsOf.keys
            .filter { values[it] == 0 }
            .sorted()
    val candidates =
        empty.associateWith { cell ->
            puzzle.runsOf
                .getValue(cell)
                .fold(ALL_DIGITS) { mask, run -> mask and runCandidates(run) }
        }
    val single = empty.firstOrNull { Integer.bitCount(candidates.getValue(it)) == 1 }
    if (single != null) {
        val digit = Combinations.digitsOf(candidates.getValue(single)).single()
        return Hint(single, digit, "Only $digit fits here.")
    }
    puzzle.runs.forEach { run ->
        val open = run.cells.filter { values[it] == 0 }
        Combinations.digitsOf(neededDigits(run)).forEach { digit ->
            val places = open.filter { candidates.getValue(it) and Combinations.bit(digit) != 0 }
            if (places.size == 1) {
                return Hint(places.single(), digit, "$digit must be in this run, and only fits here.")
            }
        }
    }
    val first = empty.firstOrNull() ?: return null
    return Hint(first, puzzle.solution[first], "Try this cell next.")
}

private fun GameState.placedMask(run: Run) =
    run.cells.filter { values[it] != 0 }.fold(0) { mask, cell ->
        mask or
            Combinations.bit(values[cell])
    }

private fun GameState.fittingMasks(run: Run): List<Int> {
    val placed = placedMask(run)
    return Combinations.masks(run.cells.size, run.sum).filter { it and placed == placed }
}

private fun GameState.runCandidates(run: Run) = fittingMasks(run).fold(0) { all, mask -> all or mask } and placedMask(run).inv()

private fun GameState.neededDigits(run: Run): Int {
    val masks = fittingMasks(run)
    if (masks.isEmpty()) {
        return 0
    }
    return masks.fold(ALL_DIGITS) { all, mask -> all and mask } and placedMask(run).inv()
}

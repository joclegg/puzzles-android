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
    if (empty.isEmpty()) {
        return null
    }
    val candidates = narrowedCandidates(empty)
    val single = empty.firstOrNull { Integer.bitCount(candidates.getValue(it)) == 1 }
    if (single != null) {
        val digit = Combinations.digitsOf(candidates.getValue(single)).single()
        return Hint(single, digit, "Only $digit fits here: ${runNames(single)} leave no other digit.")
    }
    puzzle.runs.forEach { run ->
        val open = run.cells.filter { values[it] == 0 }
        Combinations.digitsOf(neededDigits(run)).forEach { digit ->
            val places = open.filter { candidates.getValue(it) and Combinations.bit(digit) != 0 }
            if (places.size == 1) {
                return Hint(
                    places.single(),
                    digit,
                    "Every way to make ${run.label()} uses a $digit, and this is the only cell in it where $digit fits.",
                )
            }
        }
    }
    val first = empty.first()
    return Hint(first, puzzle.solution[first], "No cell can be worked out yet, so here is the first empty one.")
}

private fun GameState.narrowedCandidates(empty: List<Int>): Map<Int, Int> {
    val candidates = empty.associateWith { ALL_DIGITS }.toMutableMap()
    var changed = true
    while (changed) {
        changed = false
        puzzle.runs.forEach { run ->
            val open = run.cells.filter { values[it] == 0 }
            if (open.isNotEmpty()) {
                val supported = open.associateWith { 0 }.toMutableMap()
                val placed = placedMask(run)
                fittingMasks(run).forEach { mask ->
                    assign(open, candidates, mask and placed.inv(), emptyList()) { digits ->
                        digits.forEachIndexed { index, digit ->
                            supported[open[index]] = supported.getValue(open[index]) or Combinations.bit(digit)
                        }
                    }
                }
                open.forEach { cell ->
                    val next = candidates.getValue(cell) and supported.getValue(cell)
                    if (next != candidates.getValue(cell)) {
                        candidates[cell] = next
                        changed = true
                    }
                }
            }
        }
    }
    return candidates
}

private fun assign(
    cells: List<Int>,
    candidates: Map<Int, Int>,
    remaining: Int,
    chosen: List<Int>,
    onFound: (List<Int>) -> Unit,
) {
    if (chosen.size == cells.size) {
        onFound(chosen)
        return
    }
    Combinations.digitsOf(candidates.getValue(cells[chosen.size]) and remaining).forEach { digit ->
        assign(cells, candidates, remaining and Combinations.bit(digit).inv(), chosen + digit, onFound)
    }
}

private fun Run.label() =
    "$sum in ${cells.size} " +
        if (isAcross) {
            "across"
        } else {
            "down"
        }

private fun GameState.runNames(cell: Int) = puzzle.runsOf.getValue(cell).joinToString(" and ") { it.label() }

private fun GameState.placedMask(run: Run) =
    run.cells.filter { values[it] != 0 }.fold(0) { mask, cell ->
        mask or
            Combinations.bit(values[cell])
    }

private fun GameState.fittingMasks(run: Run): List<Int> {
    val placed = placedMask(run)
    return Combinations.masks(run.cells.size, run.sum).filter { it and placed == placed }
}

private fun GameState.neededDigits(run: Run): Int {
    val masks = fittingMasks(run)
    if (masks.isEmpty()) {
        return 0
    }
    return masks.fold(ALL_DIGITS) { all, mask -> all and mask } and placedMask(run).inv()
}

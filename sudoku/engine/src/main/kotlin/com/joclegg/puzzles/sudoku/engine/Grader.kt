package com.joclegg.puzzles.sudoku.engine

enum class Difficulty { FAST, BEGINNER, EASY, MEDIUM, HARD, EXPERT, EVIL }

val CLASSIC_LEVELS = listOf(Difficulty.FAST, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)
val KILLER_LEVELS = Difficulty.entries.toList()

enum class Technique(
    val level: Difficulty,
    val weight: Int,
) {
    NAKED_SINGLE(Difficulty.EASY, 1),
    HIDDEN_SINGLE(Difficulty.EASY, 2),
    CAGE_COMBINATION(Difficulty.EASY, 3),
    LOCKED_CANDIDATES(Difficulty.MEDIUM, 8),
    NAKED_PAIR(Difficulty.MEDIUM, 10),
    HIDDEN_PAIR(Difficulty.MEDIUM, 12),
    RULE_OF_45(Difficulty.MEDIUM, 12),
    NAKED_TRIPLE(Difficulty.HARD, 20),
    HIDDEN_TRIPLE(Difficulty.HARD, 25),
    X_WING(Difficulty.HARD, 30),
    XY_WING(Difficulty.HARD, 35),
    NAKED_QUAD(Difficulty.EXPERT, 40),
    SWORDFISH(Difficulty.EXPERT, 50),
    XYZ_WING(Difficulty.EXPERT, 55),
}

data class Grade(
    val level: Difficulty?,
    val hardest: Technique?,
    val score: Int,
    val solved: Boolean,
)

class Grader(
    private val givens: List<Int>,
    private val cages: List<Cage> = emptyList(),
) {
    private var cageEffort = 0
    private val grid = Grid.forCellCount(givens.size)
    private val houseTotal = grid.digits.sum()
    private val values = givens.toIntArray()
    private val cand = IntArray(grid.cells)
    private val cageOf = IntArray(grid.cells) { NO_CAGE }

    private val seen: List<IntArray>

    init {
        cages.forEachIndexed { index, cage -> cage.cells.forEach { cageOf[it] = index } }
        seen =
            List(grid.cells) { cell ->
                val cage = cageOf[cell]
                if (cage == NO_CAGE) {
                    grid.peers[cell]
                } else {
                    (grid.peers[cell].toSet() + cages[cage].cells - cell).toIntArray()
                }
            }
        for (cell in 0 until grid.cells) {
            cand[cell] =
                if (values[cell] != 0) {
                    0
                } else {
                    grid.allDigits
                }
        }
        for (cell in 0 until grid.cells) {
            if (values[cell] != 0) {
                eliminateFromSeen(cell, values[cell])
            }
        }
    }

    fun grade(maxLevel: Difficulty = Difficulty.EXPERT): Grade {
        val techniques = Technique.entries.filter { it.level <= maxLevel && (cages.isNotEmpty() || !it.isKillerOnly) }
        var hardest: Technique? = null
        var score = 0
        while (values.any { it == 0 }) {
            if ((0 until grid.cells).any { values[it] == 0 && cand[it] == 0 }) {
                break
            }
            cageEffort = 0
            val used = techniques.firstOrNull { apply(it) } ?: break
            if (hardest == null || used > hardest) {
                hardest = used
            }
            score += used.weight + cageEffort
        }
        val solved = values.none { it == 0 } && isValid()
        return Grade(
            if (solved) {
                hardest?.level ?: Difficulty.EASY
            } else {
                null
            },
            hardest,
            score,
            solved,
        )
    }

    fun values(): List<Int> = values.toList()

    private val Technique.isKillerOnly get() = this == Technique.CAGE_COMBINATION || this == Technique.RULE_OF_45

    private fun apply(technique: Technique): Boolean =
        when (technique) {
            Technique.NAKED_SINGLE -> nakedSingle()
            Technique.HIDDEN_SINGLE -> hiddenSingle()
            Technique.CAGE_COMBINATION -> cages.indices.any { restrictCage(cages[it].cells, cages[it].sum) }
            Technique.LOCKED_CANDIDATES -> lockedCandidates()
            Technique.NAKED_PAIR -> nakedSubset(2)
            Technique.HIDDEN_PAIR -> hiddenSubset(2)
            Technique.RULE_OF_45 -> ruleOf45()
            Technique.NAKED_TRIPLE -> nakedSubset(3)
            Technique.HIDDEN_TRIPLE -> hiddenSubset(3)
            Technique.X_WING -> fish(2)
            Technique.XY_WING -> xyWing()
            Technique.NAKED_QUAD -> nakedSubset(4)
            Technique.SWORDFISH -> fish(3)
            Technique.XYZ_WING -> xyzWing()
        }

    private fun place(
        cell: Int,
        digit: Int,
    ) {
        values[cell] = digit
        cand[cell] = 0
        eliminateFromSeen(cell, digit)
    }

    private fun eliminateFromSeen(
        cell: Int,
        digit: Int,
    ) {
        val clear = digitBit(digit).inv()
        for (other in seen[cell]) cand[other] = cand[other] and clear
    }

    private fun eliminate(
        cell: Int,
        mask: Int,
    ): Boolean {
        if (cand[cell] and mask == 0) {
            return false
        }
        cand[cell] = cand[cell] and mask.inv()
        return true
    }

    private fun isValid(): Boolean =
        grid.houses.all { house -> house.fold(0) { acc, cell -> acc or digitBit(values[cell]) } == grid.allDigits } &&
            cages.all { cage -> cage.cells.sumOf { values[it] } == cage.sum }

    private fun nakedSingle(): Boolean {
        for (cell in 0 until grid.cells) {
            if (values[cell] == 0 && Integer.bitCount(cand[cell]) == 1) {
                place(cell, singleDigit(cand[cell]))
                return true
            }
        }
        return false
    }

    private fun hiddenSingle(): Boolean {
        for (house in grid.houses) {
            for (digit in grid.digits) {
                val bit = digitBit(digit)
                var only = -1
                var count = 0
                for (cell in house) {
                    if (cand[cell] and bit != 0) {
                        only = cell
                        count++
                    }
                }
                if (count == 1) {
                    place(only, digit)
                    return true
                }
            }
        }
        return false
    }

    private fun lockedCandidates(): Boolean {
        var progress = false
        for (digit in grid.digits) {
            val bit = digitBit(digit)
            for (box in grid.boxes) {
                val cells = box.filter { cand[it] and bit != 0 }
                if (cells.isEmpty()) {
                    continue
                }
                if (cells.all { grid.rowOf(it) == grid.rowOf(cells[0]) }) {
                    for (cell in grid.rows[grid.rowOf(cells[0])]) {
                        if (cell !in box) {
                            progress = eliminate(cell, bit) || progress
                        }
                    }
                }
                if (cells.all { grid.colOf(it) == grid.colOf(cells[0]) }) {
                    for (cell in grid.cols[grid.colOf(cells[0])]) {
                        if (cell !in box) {
                            progress = eliminate(cell, bit) || progress
                        }
                    }
                }
                if (progress) {
                    return true
                }
            }
            for (line in grid.rows + grid.cols) {
                val cells = line.filter { cand[it] and bit != 0 }
                if (cells.isEmpty() || !cells.all { grid.boxOf(it) == grid.boxOf(cells[0]) }) {
                    continue
                }
                for (cell in grid.boxes[grid.boxOf(cells[0])]) {
                    if (cell !in line) {
                        progress = eliminate(cell, bit) || progress
                    }
                }
                if (progress) {
                    return true
                }
            }
        }
        return false
    }

    private fun nakedSubset(size: Int): Boolean {
        for (house in grid.houses) {
            val open = house.filter { values[it] == 0 && Integer.bitCount(cand[it]) in 2..size }
            for (subset in combinations(open, size)) {
                val union = subset.fold(0) { acc, cell -> acc or cand[cell] }
                if (Integer.bitCount(union) != size) {
                    continue
                }
                var progress = false
                for (cell in house) {
                    if (values[cell] == 0 && cell !in subset) {
                        progress = eliminate(cell, union) || progress
                    }
                }
                if (progress) {
                    return true
                }
            }
        }
        return false
    }

    private fun hiddenSubset(size: Int): Boolean {
        for (house in grid.houses) {
            val digits = grid.digits.filter { digit -> house.none { values[it] == digit } }
            for (subset in combinations(digits, size)) {
                val mask = subset.fold(0) { acc, digit -> acc or digitBit(digit) }
                val cells = house.filter { cand[it] and mask != 0 }
                if (cells.size != size) {
                    continue
                }
                var progress = false
                for (cell in cells) progress = eliminate(cell, grid.allDigits and mask.inv()) || progress
                if (progress) {
                    return true
                }
            }
        }
        return false
    }

    private fun fish(size: Int): Boolean {
        for (digit in grid.digits) {
            val bit = digitBit(digit)
            for ((base, cover) in listOf(grid.rows to grid.cols, grid.cols to grid.rows)) {
                val positions =
                    base.map { line -> line.indices.filter { cand[line[it]] and bit != 0 } }
                val lines = base.indices.filter { positions[it].size in 2..size }
                for (subset in combinations(lines, size)) {
                    val covered = subset.flatMap { positions[it] }.toSet()
                    if (covered.size != size) {
                        continue
                    }
                    var progress = false
                    for (coverIndex in covered) {
                        for ((baseIndex, cell) in cover[coverIndex].withIndex()) {
                            if (baseIndex !in subset) {
                                progress = eliminate(cell, bit) || progress
                            }
                        }
                    }
                    if (progress) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun sees(
        a: Int,
        b: Int,
    ) = a != b && b in seen[a]

    private fun xyWing(): Boolean {
        val pairs = (0 until grid.cells).filter { values[it] == 0 && Integer.bitCount(cand[it]) == 2 }
        for (pivot in pairs) {
            val wings = pairs.filter { sees(pivot, it) && Integer.bitCount(cand[it] and cand[pivot]) == 1 }
            for ((a, b) in combinations(wings, 2).map { it[0] to it[1] }) {
                val shared = cand[a] and cand[b]
                if (Integer.bitCount(shared) != 1 || shared and cand[pivot] != 0) {
                    continue
                }
                if ((cand[a] or cand[b]) and cand[pivot] != cand[pivot]) {
                    continue
                }
                var progress = false
                for (cell in 0 until grid.cells) {
                    if (cell != a && cell != b && sees(cell, a) && sees(cell, b)) {
                        progress = eliminate(cell, shared) || progress
                    }
                }
                if (progress) {
                    return true
                }
            }
        }
        return false
    }

    private fun xyzWing(): Boolean {
        for (pivot in 0 until grid.cells) {
            if (values[pivot] != 0 || Integer.bitCount(cand[pivot]) != 3) {
                continue
            }
            val wings =
                (0 until grid.cells).filter {
                    values[it] == 0 && Integer.bitCount(cand[it]) == 2 && cand[it] and cand[pivot] == cand[it] && sees(pivot, it)
                }
            for ((a, b) in combinations(wings, 2).map { it[0] to it[1] }) {
                if (cand[a] or cand[b] != cand[pivot]) {
                    continue
                }
                val shared = cand[a] and cand[b]
                if (Integer.bitCount(shared) != 1) {
                    continue
                }
                var progress = false
                for (cell in 0 until grid.cells) {
                    if (cell != pivot && cell != a && cell != b && sees(cell, pivot) && sees(cell, a) && sees(cell, b)) {
                        progress = eliminate(cell, shared) || progress
                    }
                }
                if (progress) {
                    return true
                }
            }
        }
        return false
    }

    private fun restrictCage(
        cells: List<Int>,
        sum: Int,
    ): Boolean {
        val open = cells.filter { values[it] == 0 }
        if (open.isEmpty()) {
            return false
        }
        val placed = cells.filter { values[it] != 0 }
        val usedMask = placed.fold(0) { acc, cell -> acc or digitBit(values[cell]) }
        val remaining = sum - placed.sumOf { values[it] }
        val possible = IntArray(open.size)

        fun assign(
            index: Int,
            available: Int,
            chosen: IntArray,
        ): Boolean {
            if (index == open.size) {
                chosen.forEachIndexed { i, digit -> possible[i] = possible[i] or digitBit(digit) }
                return true
            }
            var found = false
            var mask = cand[open[index]] and available
            while (mask != 0) {
                val bit = mask and -mask
                mask = mask xor bit
                chosen[index] = singleDigit(bit)
                found = assign(index + 1, available xor bit, chosen) || found
            }
            return found
        }
        val fittingCombos =
            CageCombos.of(open.size, remaining, grid.allDigits).count { combo ->
                combo and usedMask == 0 &&
                    assign(0, combo, IntArray(open.size))
            }
        var progress = false
        open.forEachIndexed { i, cell -> progress = eliminate(cell, grid.allDigits and possible[i].inv()) || progress }
        if (progress) {
            cageEffort += (fittingCombos - 1).coerceAtLeast(0) * EFFORT_PER_EXTRA_COMBINATION
        }
        return progress
    }

    private fun ruleOf45(): Boolean {
        for (house in grid.houses) {
            val houseCells = house.toSet()
            val touching = cages.filter { cage -> cage.cells.any { it in houseCells } }
            val inside = touching.filter { cage -> cage.cells.all { it in houseCells } }
            val innies = houseCells - inside.flatMap { it.cells }.toSet()
            if (innies.size in 1..3 && restrictCage(innies.toList(), houseTotal - inside.sumOf { it.sum })) {
                return true
            }

            val outies = touching.flatMap { it.cells }.filter { it !in houseCells }
            val distinct = outies.all { a -> outies.all { b -> a == b || sees(a, b) } }
            if (outies.size in 1..3 && distinct && touching.flatMap { it.cells }.containsAll(houseCells)) {
                if (restrictCage(outies, touching.sumOf { it.sum } - houseTotal)) {
                    return true
                }
            }
        }
        return false
    }
}

private const val EFFORT_PER_EXTRA_COMBINATION = 4

internal fun <T> combinations(
    items: List<T>,
    size: Int,
): Sequence<List<T>> =
    sequence {
        if (size == 0) {
            yield(emptyList())
            return@sequence
        }
        for (i in 0..items.size - size) {
            for (rest in combinations(items.subList(i + 1, items.size), size - 1)) yield(listOf(items[i]) + rest)
        }
    }

val Difficulty.grid: Grid get() =
    if (this == Difficulty.FAST) {
        Grid.FAST
    } else {
        Grid.STANDARD
    }

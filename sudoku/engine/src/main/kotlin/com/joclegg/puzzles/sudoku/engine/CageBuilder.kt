package com.joclegg.puzzles.sudoku.engine

enum class CageProblem { EMPTY, NOT_CONNECTED, IMPOSSIBLE_SUM }

data class CageBuilder(
    val cages: List<Cage> = emptyList(),
    val selection: List<Int> = emptyList(),
    val sumInput: String = "",
    val editingCage: Int? = null,
    val nextDigitReplacesSum: Boolean = false,
    val grid: Grid = Grid.STANDARD,
) {
    val cageOwner: IntArray by lazy {
        IntArray(grid.cells) { NO_CAGE }.also { owner -> cages.forEachIndexed { index, cage -> cage.cells.forEach { owner[it] = index } } }
    }

    fun tapCell(cell: Int): CageBuilder {
        val owner = cageOwner[cell]
        return when {
            cell in selection -> copy(selection = selection - cell)
            owner != NO_CAGE && owner != editingCage -> selectCage(owner)
            selection.isNotEmpty() && selection.any { grid.isOrthogonalNeighbour(it, cell) } -> copy(selection = selection + cell)
            else -> CageBuilder(cages, selection = listOf(cell), grid = grid)
        }
    }

    fun typeDigit(digit: Int): CageBuilder {
        val typed =
            if (nextDigitReplacesSum || sumInput.length >= MAX_SUM_DIGITS) {
                digit.toString()
            } else {
                sumInput + digit
            }
        return copy(sumInput = typed, nextDigitReplacesSum = false)
    }

    fun problem(): CageProblem? {
        val sum = sumInput.toIntOrNull()
        return when {
            selection.isEmpty() || sum == null -> CageProblem.EMPTY
            !grid.isConnected(selection) -> CageProblem.NOT_CONNECTED
            CageCombos.of(selection.size, sum, grid.allDigits).isEmpty() -> CageProblem.IMPOSSIBLE_SUM
            else -> null
        }
    }

    fun confirm(): CageBuilder {
        if (problem() != null) {
            return copy(nextDigitReplacesSum = true)
        }
        val cage = Cage(selection.sorted(), sumInput.toInt())
        val kept = cages.filterIndexed { index, _ -> index != editingCage }
        return CageBuilder(kept + cage, grid = grid)
    }

    fun deleteSelection(): CageBuilder {
        val kept = cages.filterIndexed { index, _ -> index != editingCage }
        return CageBuilder(kept, grid = grid)
    }

    private fun selectCage(index: Int) =
        copy(selection = cages[index].cells, sumInput = cages[index].sum.toString(), editingCage = index, nextDigitReplacesSum = true)

    private companion object {
        const val MAX_SUM_DIGITS = 2
    }
}

fun Grid.isOrthogonalNeighbour(
    a: Int,
    b: Int,
): Boolean = b in neighbours(a)

fun Grid.isConnected(cells: List<Int>): Boolean {
    if (cells.isEmpty()) {
        return false
    }
    val reached = mutableSetOf(cells.first())
    val frontier = ArrayDeque(listOf(cells.first()))
    while (frontier.isNotEmpty()) {
        val cell = frontier.removeFirst()
        for (next in cells) {
            if (next !in reached && isOrthogonalNeighbour(cell, next)) {
                reached += next
                frontier += next
            }
        }
    }
    return reached.size == cells.size
}

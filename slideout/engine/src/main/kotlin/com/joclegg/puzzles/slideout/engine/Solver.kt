package com.joclegg.puzzles.slideout.engine

object Solver {
    fun shortest(start: Board): List<Move>? {
        val space = Space(start)
        val first = space.key(start)
        if (space.isSolved(first)) {
            return emptyList()
        }
        val parent = LongLongMap()
        parent.put(first, first)
        val queue = LongQueue()
        queue.add(first)
        while (queue.isNotEmpty()) {
            val key = queue.remove()
            var found = -1L
            space.forEachNeighbour(key) { next ->
                if (found < 0 && parent.putIfAbsent(next, key)) {
                    if (space.isSolved(next)) {
                        found = next
                    } else {
                        queue.add(next)
                    }
                }
            }
            if (found >= 0) {
                return space.path(parent, first, found)
            }
        }
        return null
    }

    fun hardest(
        start: Board,
        band: IntRange,
    ): Pair<Board, Int>? {
        val space = Space(start)
        val seen = LongLongMap()
        val all = LongQueue()
        val first = space.key(start)
        seen.put(first, 0)
        all.add(first)
        var index = 0
        while (index < all.size) {
            space.forEachNeighbour(all[index]) { next ->
                if (seen.putIfAbsent(next, 0)) {
                    all.add(next)
                }
            }
            index++
        }
        val distance = LongLongMap()
        val frontier = LongQueue()
        for (i in 0 until all.size) {
            if (space.isSolved(all[i])) {
                distance.put(all[i], 0)
                frontier.add(all[i])
            }
        }
        var bestKey = -1L
        var bestDistance = -1L
        while (frontier.isNotEmpty()) {
            val key = frontier.remove()
            val depth = distance.get(key)
            if (depth in bestDistance + 1..band.last.toLong()) {
                bestKey = key
                bestDistance = depth
            }
            space.forEachNeighbour(key) { next ->
                if (distance.putIfAbsent(next, depth + 1)) {
                    frontier.add(next)
                }
            }
        }
        return if (bestKey < 0) {
            null
        } else {
            space.board(bestKey) to bestDistance.toInt()
        }
    }
}

private class Space(
    board: Board,
) {
    private val blocks = board.blocks
    private val count = blocks.size
    private val lengths = IntArray(count) { blocks[it].length }
    private val masks =
        Array(count) { i ->
            LongArray(SIZE) { position ->
                if (position + lengths[i] > SIZE) {
                    -1L
                } else {
                    blocks[i].movedTo(position).cells.fold(0L) { mask, cell -> mask or (1L shl cell) }
                }
            }
        }

    fun key(board: Board): Long = board.blocks.foldIndexed(0L) { i, key, block -> key or (block.position.toLong() shl (BITS * i)) }

    fun position(
        key: Long,
        block: Int,
    ): Int = ((key shr (BITS * block)) and POSITION_MASK).toInt()

    fun isSolved(key: Long) = position(key, 0) + lengths[0] == SIZE

    inline fun forEachNeighbour(
        key: Long,
        action: (Long) -> Unit,
    ) {
        var occupied = 0L
        for (i in 0 until count) {
            occupied = occupied or masks[i][position(key, i)]
        }
        for (i in 0 until count) {
            val position = position(key, i)
            val others = occupied and masks[i][position].inv()
            val cleared = key and (POSITION_MASK shl (BITS * i)).inv()
            var lower = position - 1
            while (lower >= 0 && masks[i][lower] and others == 0L) {
                action(cleared or (lower.toLong() shl (BITS * i)))
                lower--
            }
            var higher = position + 1
            while (higher + lengths[i] <= SIZE && masks[i][higher] and others == 0L) {
                action(cleared or (higher.toLong() shl (BITS * i)))
                higher++
            }
        }
    }

    fun board(key: Long) = Board(blocks.mapIndexed { i, block -> block.movedTo(position(key, i)) })

    fun path(
        parent: LongLongMap,
        start: Long,
        end: Long,
    ): List<Move> {
        val moves = mutableListOf<Move>()
        var key = end
        while (key != start) {
            val before = parent.get(key)
            val moved = (0 until count).first { position(key, it) != position(before, it) }
            moves += Move(moved, position(key, moved))
            key = before
        }
        return moves.reversed()
    }
}

private const val BITS = 3
private const val POSITION_MASK = 7L

private class LongLongMap {
    private var keys = LongArray(INITIAL) { EMPTY }
    private var values = LongArray(INITIAL)
    private var size = 0

    fun get(key: Long): Long = values[slot(key)]

    fun put(
        key: Long,
        value: Long,
    ) {
        putIfAbsent(key, value)
        values[slot(key)] = value
    }

    fun putIfAbsent(
        key: Long,
        value: Long,
    ): Boolean {
        val index = slot(key)
        if (keys[index] != EMPTY) {
            return false
        }
        keys[index] = key
        values[index] = value
        size++
        if (size * 2 > keys.size) {
            grow()
        }
        return true
    }

    private fun slot(key: Long): Int {
        var index = (mix(key) and (keys.size - 1).toLong()).toInt()
        while (keys[index] != EMPTY && keys[index] != key) {
            index = (index + 1) and (keys.size - 1)
        }
        return index
    }

    private fun grow() {
        val oldKeys = keys
        val oldValues = values
        keys = LongArray(oldKeys.size * 2) { EMPTY }
        values = LongArray(oldKeys.size * 2)
        size = 0
        for (i in oldKeys.indices) {
            if (oldKeys[i] != EMPTY) {
                putIfAbsent(oldKeys[i], oldValues[i])
            }
        }
    }

    private fun mix(key: Long): Long {
        var h = key * -0x61c8864680b583ebL
        h = h xor (h ushr 29)
        return h
    }

    private companion object {
        const val INITIAL = 1024
        const val EMPTY = -1L
    }
}

private class LongQueue {
    private var items = LongArray(INITIAL)
    private var head = 0
    var size = 0
        private set

    operator fun get(index: Int) = items[index]

    fun add(value: Long) {
        if (size == items.size) {
            items = items.copyOf(items.size * 2)
        }
        items[size++] = value
    }

    fun remove(): Long = items[head++]

    fun isNotEmpty() = head < size

    private companion object {
        const val INITIAL = 1024
    }
}

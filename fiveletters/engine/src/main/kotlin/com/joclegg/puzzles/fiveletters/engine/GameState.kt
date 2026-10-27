package com.joclegg.puzzles.fiveletters.engine

enum class Mark(
    val emoji: String,
) {
    CORRECT("🟩"),
    PRESENT("🟨"),
    ABSENT("⬜"),
}

enum class Rejection {
    TOO_SHORT,
    NOT_A_WORD,
    MUST_USE_HINTS,
}

fun score(
    guess: String,
    answer: String,
): List<Mark> {
    val marks = MutableList(WORD_LENGTH) { Mark.ABSENT }
    val unmatched = mutableMapOf<Char, Int>()
    for (i in 0 until WORD_LENGTH) {
        if (guess[i] == answer[i]) {
            marks[i] = Mark.CORRECT
        } else {
            unmatched[answer[i]] = (unmatched[answer[i]] ?: 0) + 1
        }
    }
    for (i in 0 until WORD_LENGTH) {
        val left = unmatched[guess[i]] ?: 0
        if (marks[i] != Mark.CORRECT && left > 0) {
            marks[i] = Mark.PRESENT
            unmatched[guess[i]] = left - 1
        }
    }
    return marks
}

data class GameState(
    val answer: String,
    val guesses: List<String> = emptyList(),
    val typing: String = "",
    val hardMode: Boolean = false,
    val rejection: Rejection? = null,
    val revealed: Boolean = false,
) {
    val marks: List<List<Mark>> get() = guesses.map { score(it, answer) }

    val isWon: Boolean get() = answer in guesses

    val isLost: Boolean get() = !isWon && (revealed || guesses.size == MAX_GUESSES)

    val isOver: Boolean get() = isWon || isLost

    val letterMarks: Map<Char, Mark>
        get() =
            guesses
                .zip(marks)
                .flatMap { (guess, marks) -> guess.toList().zip(marks) }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, marks) -> marks.minBy { it.ordinal } }

    fun hintLetter(): Pair<Int, Char>? {
        val index = answer.indices.firstOrNull { i -> guesses.none { it[i] == answer[i] } } ?: return null
        return index to answer[index]
    }

    fun revealWord(): GameState = copy(typing = "", rejection = null, revealed = true)

    fun type(letter: Char): GameState {
        if (isOver || typing.length == WORD_LENGTH || !letter.isLetter()) {
            return this
        }
        return copy(typing = typing + letter.uppercaseChar(), rejection = null)
    }

    fun delete(): GameState {
        if (isOver || typing.isEmpty()) {
            return this
        }
        return copy(typing = typing.dropLast(1), rejection = null)
    }

    fun submit(words: Words): GameState {
        if (isOver) {
            return this
        }
        val rejected =
            when {
                typing.length < WORD_LENGTH -> Rejection.TOO_SHORT
                typing != answer && !words.isAllowed(typing) -> Rejection.NOT_A_WORD
                hardMode && !usesHints(typing) -> Rejection.MUST_USE_HINTS
                else -> null
            }
        if (rejected != null) {
            return copy(rejection = rejected)
        }
        return copy(guesses = guesses + typing, typing = "", rejection = null)
    }

    fun shareText(title: String): String {
        val result =
            if (isWon) {
                guesses.size.toString()
            } else {
                "X"
            }
        val grid = marks.joinToString("\n") { row -> row.joinToString("") { it.emoji } }
        val mode =
            if (hardMode) {
                "*"
            } else {
                ""
            }
        return "$title $result/$MAX_GUESSES$mode\n\n$grid"
    }

    private fun usesHints(word: String): Boolean =
        guesses.zip(marks).all { (guess, marks) ->
            marks.indices.all { i ->
                when (marks[i]) {
                    Mark.CORRECT -> {
                        word[i] == guess[i]
                    }

                    Mark.PRESENT -> {
                        word.count { it == guess[i] } >=
                            guess.indices.count { guess[it] == guess[i] && marks[it] != Mark.ABSENT }
                    }

                    Mark.ABSENT -> {
                        true
                    }
                }
            }
        }
}

package com.joclegg.puzzles.wordwheel.engine

const val MIN_WORD = 3

enum class Level(
    val letters: Int,
    val words: IntRange,
) {
    FIVE(5, 4..6),
    SIX(6, 5..8),
    SEVEN(7, 6..10),
}

class WordList(
    valid: Collection<String>,
    common: Collection<String>,
) {
    val valid: Set<String> = valid.map { it.uppercase() }.toSet()
    val common: Set<String> = common.map { it.uppercase() }.filter { it in this.valid && !isAmericanOnly(it) }.toSet()

    fun spellable(
        letters: String,
        from: Set<String>,
    ): List<String> = from.filter { it.length >= MIN_WORD && fits(it, letters) }

    private fun isAmericanOnly(word: String): Boolean {
        val british =
            listOf(
                Regex("OR$") to "OUR",
                Regex("ER$") to "RE",
                Regex("IZE$") to "ISE",
                Regex("IZED$") to "ISED",
                Regex("ENSE$") to "ENCE",
                Regex("^GRAY") to "GREY",
            )
        return british.any { (pattern, replacement) -> pattern.containsMatchIn(word) && word.replace(pattern, replacement) in valid }
    }

    companion object {
        fun fits(
            word: String,
            letters: String,
        ): Boolean {
            val left = letters.groupingBy { it }.eachCount().toMutableMap()
            return word.all { letter ->
                val count = left[letter] ?: 0
                left[letter] = count - 1
                count > 0
            }
        }
    }
}

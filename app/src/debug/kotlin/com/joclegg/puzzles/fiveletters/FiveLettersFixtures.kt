package com.joclegg.puzzles.fiveletters

import com.joclegg.puzzles.fiveletters.core.data.WordRef

object FiveLettersFixtures {
    fun word(name: String?): WordRef? =
        when (name) {
            "five_letters_crane" -> WordRef("practice_fixture", "CRANE")
            else -> null
        }
}

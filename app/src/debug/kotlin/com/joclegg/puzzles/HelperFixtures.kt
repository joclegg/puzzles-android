package com.joclegg.puzzles

object HelperFixtures {
    const val EXTRA = "helpers"

    suspend fun apply(
        app: PuzzlesApplication,
        value: String?,
    ) {
        if (value == "on") {
            app.keepLegacyHelperDefaults()
        }
    }
}

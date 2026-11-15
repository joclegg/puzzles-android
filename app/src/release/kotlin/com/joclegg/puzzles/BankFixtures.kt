package com.joclegg.puzzles

object BankFixtures {
    const val EXTRA = "banks"

    @Suppress("UNUSED_PARAMETER")
    fun install(
        app: PuzzlesApplication,
        names: String?,
    ) = Unit

    @Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
    fun offline(app: PuzzlesApplication): Boolean = false
}

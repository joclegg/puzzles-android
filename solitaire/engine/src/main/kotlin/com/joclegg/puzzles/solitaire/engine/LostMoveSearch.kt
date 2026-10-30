package com.joclegg.puzzles.solitaire.engine

data class LostMoveSearch(
    val winnable: Int,
    val lost: Int,
) {
    val probe: Int?
        get() =
            if (lost - winnable > 1) {
                (winnable + lost) / 2
            } else {
                null
            }

    fun narrow(
        probe: Int,
        verdict: Verdict,
    ): LostMoveSearch =
        if (verdict == Verdict.WINNABLE) {
            copy(winnable = probe)
        } else {
            copy(lost = probe)
        }
}

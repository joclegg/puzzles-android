package com.joclegg.puzzles.core.data

data class DailyRecord(
    val id: Long,
    val puzzleId: String,
    val status: String,
    val finishedAt: Long?,
)

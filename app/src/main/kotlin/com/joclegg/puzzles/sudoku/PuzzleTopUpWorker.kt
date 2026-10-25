package com.joclegg.puzzles.sudoku

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.joclegg.puzzles.PuzzlesApplication

class PuzzleTopUpWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as PuzzlesApplication).sudoku.supply.topUp(shouldStop = { isStopped })
        return Result.success()
    }

    companion object {
        private const val NAME = "puzzle-top-up"

        fun enqueue(context: Context) {
            val request =
                OneTimeWorkRequestBuilder<PuzzleTopUpWorker>()
                    .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}

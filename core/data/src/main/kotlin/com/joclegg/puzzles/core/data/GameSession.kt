package com.joclegg.puzzles.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameSession(
    private val scope: CoroutineScope,
    private val monotonicNowMs: () -> Long,
    private val canRun: () -> Boolean,
    private val onTick: () -> Unit,
    private val onSave: () -> Unit,
    private val tickMs: Long = TICK_MS,
    private val saveAfterIdleMs: Long = SAVE_AFTER_IDLE_MS,
) {
    private var timer = GameTimer()
    private var visible = false
    private var ticker: Job? = null
    private var pendingSave: Job? = null

    fun elapsedMs() = timer.elapsedMs(monotonicNowMs())

    fun restore(elapsedMs: Long) {
        timer = GameTimer(accumulatedMs = elapsedMs)
        if (visible) {
            resume()
        }
    }

    fun restart() = restore(0)

    fun onVisible() {
        visible = true
        resume()
    }

    fun onHidden() {
        visible = false
        pause()
    }

    fun resume() {
        if (!canRun()) {
            return
        }
        timer = timer.start(monotonicNowMs())
        ticker?.cancel()
        ticker =
            scope.launch {
                while (isActive) {
                    onTick()
                    delay(tickMs)
                }
            }
    }

    fun pause() {
        timer = timer.pause(monotonicNowMs())
        ticker?.cancel()
    }

    fun stop() {
        pause()
        cancelPendingSave()
    }

    fun saveSoon() {
        pendingSave?.cancel()
        pendingSave =
            scope.launch {
                delay(saveAfterIdleMs)
                onSave()
            }
    }

    fun cancelPendingSave() {
        pendingSave?.cancel()
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_AFTER_IDLE_MS = 400L
    }
}

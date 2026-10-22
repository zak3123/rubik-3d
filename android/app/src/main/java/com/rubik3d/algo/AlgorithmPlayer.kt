package com.rubik3d.algo

import android.os.Handler
import android.os.Looper
import com.rubik3d.gl.RotationManager
import com.rubik3d.model.CubeState
import com.rubik3d.model.Move

interface StepScheduler {
    fun postDelayed(delayMs: Long, action: () -> Unit)
    fun cancelAll()
}

class AndroidStepScheduler : StepScheduler {
    private val handler = Handler(Looper.getMainLooper())
    override fun postDelayed(delayMs: Long, action: () -> Unit) {
        handler.postDelayed(action, delayMs)
    }
    override fun cancelAll() {
        handler.removeCallbacksAndMessages(null)
    }
}

class AlgorithmPlayer(
    private val cubeState: CubeState,
    private val rotationManager: RotationManager,
    private val scheduler: StepScheduler = AndroidStepScheduler()
) {
    enum class PlayState { IDLE, PLAYING, PAUSED, FINISHED }

    var moves: List<Move> = emptyList()
        private set
    var currentIndex: Int = -1 // -1 means start position; 0..moves.size - 1
        private set

    var state: PlayState = PlayState.IDLE
        private set

    var onStateChangedListener: ((PlayState, Int, List<Move>) -> Unit)? = null
    var stepDelayMs: Long = 120L

    fun loadAlgorithm(sequence: String): Boolean {
        pause()
        val parsed = Move.parseSequence(sequence)
        moves = parsed
        currentIndex = -1
        state = PlayState.IDLE
        notifyState()
        return moves.isNotEmpty()
    }

    fun play() {
        if (moves.isEmpty()) return
        if (currentIndex >= moves.size - 1) {
            state = PlayState.FINISHED
            notifyState()
            return
        }
        state = PlayState.PLAYING
        notifyState()
        playNextStep()
    }

    fun pause() {
        if (state == PlayState.PLAYING) {
            state = PlayState.PAUSED
            scheduler.cancelAll()
            notifyState()
        }
    }

    fun nextStep(): Boolean {
        if (rotationManager.isBusy() || moves.isEmpty()) return false
        if (currentIndex >= moves.size - 1) return false

        currentIndex++
        rotationManager.queueMove(moves[currentIndex])
        if (state != PlayState.PLAYING) {
            state = if (currentIndex >= moves.size - 1) PlayState.FINISHED else PlayState.PAUSED
        }
        notifyState()
        return true
    }

    fun prevStep(): Boolean {
        if (rotationManager.isBusy() || moves.isEmpty()) return false
        if (currentIndex < 0) return false

        val move = moves[currentIndex]
        currentIndex--
        rotationManager.queueMove(move.inverse())
        state = PlayState.PAUSED
        notifyState()
        return true
    }

    fun reset() {
        pause()
        while (currentIndex >= 0) {
            val move = moves[currentIndex]
            cubeState.applyMove(move.inverse())
            currentIndex--
        }
        state = PlayState.IDLE
        notifyState()
    }

    fun onMoveCompleted(move: Move) {
        if (state == PlayState.PLAYING) {
            if (currentIndex >= moves.size - 1) {
                state = PlayState.FINISHED
                notifyState()
            } else {
                scheduler.postDelayed(stepDelayMs) {
                    if (state == PlayState.PLAYING) {
                        playNextStep()
                    }
                }
            }
        }
    }

    private fun playNextStep() {
        if (currentIndex < moves.size - 1) {
            currentIndex++
            rotationManager.queueMove(moves[currentIndex])
            notifyState()
        } else {
            state = PlayState.FINISHED
            notifyState()
        }
    }

    private fun notifyState() {
        onStateChangedListener?.invoke(state, currentIndex, moves)
    }
}

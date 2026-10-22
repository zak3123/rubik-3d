package com.rubik3d.gl

import android.os.SystemClock
import com.rubik3d.model.CubeState
import com.rubik3d.model.Move
import java.util.ArrayDeque

class RotationManager(private val cubeState: CubeState) {

    var onMoveFinishedListener: ((Move) -> Unit)? = null
    var onMoveStartedListener: ((Move) -> Unit)? = null
    var onDragStartedListener: (() -> Unit)? = null
    // Queue for animated moves
    private val moveQueue = ArrayDeque<Move>()

    // Currently animating move
    var activeMove: Move? = null
        private set
    var currentAngle: Float = 0f
        private set

    private var animStartTime: Long = 0
    var animDuration: Long = 180 // ms per 90 deg turn
        private set
    // Interactive touch drag state
    var isDragging: Boolean = false
        private set
    var dragAxis: Int = 0
        private set
    var dragLayer: Int = 0
        private set
    var dragAngle: Float = 0f
        private set

    fun setSpeedMs(durationMs: Long) {
        animDuration = durationMs.coerceIn(50, 600)
    }

    fun queueMove(move: Move) {
        synchronized(moveQueue) {
            moveQueue.add(move)
        }
    }

    fun queueMoves(moves: Collection<Move>) {
        synchronized(moveQueue) {
            moveQueue.addAll(moves)
        }
    }

    fun clearQueue() {
        synchronized(moveQueue) {
            moveQueue.clear()
        }
    }

    fun isBusy(): Boolean = activeMove != null || isDragging || synchronized(moveQueue) { moveQueue.isNotEmpty() }

    // Start manual drag
    fun startDrag(axis: Int, layer: Int) {
        if (activeMove != null) return
        isDragging = true
        dragAxis = axis
        dragLayer = layer
        dragAngle = 0f
        onDragStartedListener?.invoke()
    }

    fun updateDrag(deltaAngle: Float) {
        if (!isDragging) return
        dragAngle += deltaAngle
    }

    /**
     * Finish manual drag: snap to nearest 90 degrees or revert
     */
    fun endDrag() {
        if (!isDragging) return
        val angle = dragAngle
        val axis = dragAxis
        val layer = dragLayer
        isDragging = false

        // Snap threshold: 25 degrees
        val absAngle = Math.abs(angle)
        if (absAngle >= 25f) {
            val quarterTurns = Math.round(angle / 90f).coerceIn(-2, 2)
            if (quarterTurns != 0) {
                val targetAngle = quarterTurns * 90f
                // Find matching move or construct programmatic move
                val move = findMoveForAxisLayerAngle(axis, layer, targetAngle)
                if (move != null) {
                    // Start snap animation from dragAngle to targetAngle
                    startSnapAnimation(move, angle, targetAngle)
                    return
                }
            }
        }
        // Snap back to 0
        if (absAngle > 1f) {
            startSnapAnimation(null, angle, 0f)
        }
    }

    private var snapTargetAngle = 0f
    private var snapStartAngle = 0f
    private var isSnapping = false

    private fun startSnapAnimation(move: Move?, fromAngle: Float, toAngle: Float) {
        isSnapping = true
        activeMove = move
        snapStartAngle = fromAngle
        snapTargetAngle = toAngle
        currentAngle = fromAngle
        animStartTime = SystemClock.uptimeMillis()
    }

    private fun findMoveForAxisLayerAngle(axis: Int, layer: Int, angle: Float): Move? {
        val candidates = listOf(
            Move.R, Move.R_PRIME, Move.R2,
            Move.L, Move.L_PRIME, Move.L2,
            Move.U, Move.U_PRIME, Move.U2,
            Move.D, Move.D_PRIME, Move.D2,
            Move.F, Move.F_PRIME, Move.F2,
            Move.B, Move.B_PRIME, Move.B2
        )
        return candidates.firstOrNull {
            it.axis == axis && it.layer == layer && Math.abs(it.angleDegrees - angle) < 1f
        }
    }

    /**
     * Called on each frame before rendering
     */
    fun update() {
        if (isDragging) {
            currentAngle = dragAngle
            return
        }

        if (isSnapping) {
            val now = SystemClock.uptimeMillis()
            val elapsed = now - animStartTime
            val duration = 120L // quick snap ms
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            // Ease-out quad
            val t = progress * (2f - progress)
            currentAngle = snapStartAngle + (snapTargetAngle - snapStartAngle) * t

            if (progress >= 1f) {
                isSnapping = false
                val move = activeMove
                activeMove = null
                currentAngle = 0f
                if (move != null) {
                    cubeState.applyMove(move)
                    onMoveFinishedListener?.invoke(move)
                }
            }
            return
        }

        if (activeMove == null) {
            // Check queue
            val nextMove = synchronized(moveQueue) {
                if (moveQueue.isNotEmpty()) moveQueue.poll() else null
            }
            if (nextMove != null) {
                activeMove = nextMove
                currentAngle = 0f
                animStartTime = SystemClock.uptimeMillis()
                animStartTime = SystemClock.uptimeMillis()
                onMoveStartedListener?.invoke(nextMove)
        } else {
            val move = activeMove ?: return
            val now = SystemClock.uptimeMillis()
            val elapsed = now - animStartTime
            val totalTime = if (Math.abs(move.angleDegrees) > 100f) animDuration * 1.5f else animDuration.toFloat()
            val progress = (elapsed / totalTime).coerceIn(0f, 1f)
            // Ease-out quad
            val t = progress * (2f - progress)
            currentAngle = move.angleDegrees * t

            if (progress >= 1f) {
                cubeState.applyMove(move)
                activeMove = null
                currentAngle = 0f
                onMoveFinishedListener?.invoke(move)
            }
        }
    }
}

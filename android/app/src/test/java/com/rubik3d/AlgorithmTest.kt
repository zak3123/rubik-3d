package com.rubik3d

import com.rubik3d.algo.AlgorithmPlayer
import com.rubik3d.algo.AlgorithmPresets
import com.rubik3d.algo.StepScheduler
import com.rubik3d.gl.RotationManager
import com.rubik3d.model.CubeState
import com.rubik3d.model.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlgorithmTest {

    private lateinit var cube: CubeState
    private lateinit var rotationManager: RotationManager
    private lateinit var player: AlgorithmPlayer

    private class ManualScheduler : StepScheduler {
        val pendingActions = mutableListOf<() -> Unit>()
        override fun postDelayed(delayMs: Long, action: () -> Unit) {
            pendingActions.add(action)
        }
        override fun cancelAll() {
            pendingActions.clear()
        }
        fun runNext() {
            if (pendingActions.isNotEmpty()) {
                val act = pendingActions.removeAt(0)
                act.invoke()
            }
        }
    }

    @Before
    fun setUp() {
        cube = CubeState()
        rotationManager = RotationManager(cube)
        player = AlgorithmPlayer(cube, rotationManager, ManualScheduler())
    }

    @Test
    fun parseSequenceFiltersWhitespaceAndInvalidTokens() {
        val seq = "  R   U  R'   U'   INVALID_TOKEN  F2  "
        val moves = Move.parseSequence(seq)
        assertEquals(5, moves.size)
        assertEquals("R", moves[0].notation)
        assertEquals("U", moves[1].notation)
        assertEquals("R'", moves[2].notation)
        assertEquals("U'", moves[3].notation)
        assertEquals("F2", moves[4].notation)
    }

    @Test
    fun presetsAreValidAndParseProperly() {
        for (item in AlgorithmPresets.ITEMS) {
            val moves = Move.parseSequence(item.notation)
            assertTrue("Preset ${item.name} should parse at least 1 move", moves.isNotEmpty())
        }
    }

    @Test
    fun forwardAndBackwardStepConservation() {
        player.loadAlgorithm("R U R' U' F2")
        assertEquals(5, player.moves.size)
        assertEquals(-1, player.currentIndex)

        // Step forward 3 times
        repeat(3) {
            assertTrue(player.nextStep())
            // Simulate completion in RotationManager
            rotationManager.update()
        }
        assertEquals(2, player.currentIndex)
        assertFalse(cube.isSolved())

        // Step backward 3 times
        repeat(3) {
            assertTrue(player.prevStep())
            rotationManager.update()
        }
        assertEquals(-1, player.currentIndex)
        assertTrue("Rewinding all steps must restore solved state", cube.isSolved())
    }

    @Test
    fun algorithmResetRestoresInitialState() {
        player.loadAlgorithm("R U R' U'")
        repeat(4) {
            player.nextStep()
            rotationManager.update()
        }
        assertEquals(3, player.currentIndex)
        assertFalse(cube.isSolved())

        player.reset()
        assertEquals(-1, player.currentIndex)
        assertEquals(AlgorithmPlayer.PlayState.IDLE, player.state)
        assertTrue(cube.isSolved())
    }
}

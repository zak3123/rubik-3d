package com.rubik3d

import com.rubik3d.model.CubeState
import com.rubik3d.model.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CubeStateTest {

    private lateinit var cube: CubeState

    @Before
    fun setUp() {
        cube = CubeState()
    }

    @Test
    fun initialStateIsSolved() {
        assertTrue(cube.isSolved())
        assertEquals(26, cube.cubies.size)
    }

    @Test
    fun singleMoveChangesState() {
        cube.applyMove(Move.R)
        assertFalse(cube.isSolved())
    }

    @Test
    fun fourTurnsReturnsToSolved() {
        val testMoves = listOf(Move.U, Move.R, Move.F, Move.D, Move.L, Move.B)
        for (m in testMoves) {
            cube.reset()
            assertTrue(cube.isSolved())
            repeat(4) { cube.applyMove(m) }
            assertTrue("Failed identity for 4x ${m.notation}", cube.isSolved())
        }
    }

    @Test
    fun moveAndInverseReturnsToSolved() {
        val pairs = listOf(
            Move.R to Move.R_PRIME,
            Move.L to Move.L_PRIME,
            Move.U to Move.U_PRIME,
            Move.D to Move.D_PRIME,
            Move.F to Move.F_PRIME,
            Move.B to Move.B_PRIME
        )
        for ((m, inv) in pairs) {
            cube.reset()
            cube.applyMove(m)
            assertFalse(cube.isSolved())
            cube.applyMove(inv)
            assertTrue("Failed inverse for ${m.notation} + ${inv.notation}", cube.isSolved())
        }
    }

    @Test
    fun sexyMoveSixTimesReturnsToSolved() {
        // (R U R' U') x 6 = solved
        repeat(6) {
            cube.applyMove(Move.R)
            cube.applyMove(Move.U)
            cube.applyMove(Move.R_PRIME)
            cube.applyMove(Move.U_PRIME)
        }
        assertTrue("Sexy move x 6 must return cube to solved state", cube.isSolved())
    }

    @Test
    fun scrambleConservationInvariant() {
        val scramble = cube.generateScramble(25)
        assertEquals(25, scramble.size)

        for (m in scramble) {
            cube.applyMove(m)
        }

        // 26 cubies conserved
        assertEquals(26, cube.cubies.size)

        // All cubies within valid discrete bounds [-1, 1]
        for (c in cube.cubies) {
            assertTrue(c.cx in -1..1)
            assertTrue(c.cy in -1..1)
            assertTrue(c.cz in -1..1)
            assertFalse(c.cx == 0 && c.cy == 0 && c.cz == 0)
        }

        // No two cubies share the exact same grid coordinate
        val occupied = cube.cubies.map { "${it.cx},${it.cy},${it.cz}" }.toSet()
        assertEquals(26, occupied.size)
    }

    @Test
    fun moveParserParsesStandardMoves() {
        val notations = listOf("R", "R'", "R2", "L", "L'", "L2", "U", "U'", "U2", "D", "D'", "D2", "F", "F'", "F2", "B", "B'", "B2")
        for (n in notations) {
            val m = Move.parse(n)
            assertNotNull("Move.parse failed for $n", m)
            assertEquals(n, m?.notation)
        }
    }
}

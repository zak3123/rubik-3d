package com.rubik3d

import com.rubik3d.model.CubeState
import com.rubik3d.model.Move
import com.rubik3d.solver.KociembaSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class SolverTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initSolver() {
            KociembaSolver.warmup()
        }
    }

    @Test
    fun faceletExtractionSolvedCube() {
        val cube = CubeState()
        val faceletStr = cube.toFaceletString()
        assertEquals("UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB", faceletStr)
    }

    @Test
    fun solvedCubeReturnsEmptySolution() {
        val cube = CubeState()
        val solution = KociembaSolver.solve(cube)
        assertNotNull(solution)
        assertTrue(solution!!.isEmpty())
    }

    @Test
    fun singleMoveSolve() {
        val cube = CubeState()
        cube.applyMove(Move.R)
        val solution = KociembaSolver.solve(cube)
        assertNotNull(solution)
        assertEquals(1, solution!!.size)
        assertEquals(Move.R_PRIME, solution[0])

        for (m in solution) {
            cube.applyMove(m)
        }
        assertTrue(cube.isSolved())
    }

    @Test
    fun sexyMoveSequenceSolve() {
        val cube = CubeState()
        val sexyMoves = listOf(Move.R, Move.U, Move.R_PRIME, Move.U_PRIME)
        for (m in sexyMoves) {
            cube.applyMove(m)
        }

        val solution = KociembaSolver.solve(cube)
        assertNotNull(solution)
        assertTrue(solution!!.isNotEmpty())
        assertTrue(solution.size <= 6)

        for (m in solution) {
            cube.applyMove(m)
        }
        assertTrue(cube.isSolved())
    }

    @Test
    fun randomScrambleSolvesUnder25Moves() {
        val cube = CubeState()
        repeat(5) { attempt ->
            cube.reset()
            val scramble = cube.generateScramble(20)
            for (m in scramble) {
                cube.applyMove(m)
            }

            val solution = KociembaSolver.solve(cube)
            assertNotNull("Solver should find solution for attempt $attempt", solution)
            assertTrue("Solution length must be <= 25 moves, got ${solution!!.size}", solution.size <= 25)

            for (m in solution) {
                cube.applyMove(m)
            }
            assertTrue("Cube must be solved after solution for attempt $attempt", cube.isSolved())
        }
    }
}

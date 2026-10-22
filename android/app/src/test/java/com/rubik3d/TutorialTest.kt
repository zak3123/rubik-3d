package com.rubik3d

import com.rubik3d.algo.AlgorithmPlayer
import com.rubik3d.algo.StepScheduler
import com.rubik3d.gl.RotationManager
import com.rubik3d.model.CubeState
import com.rubik3d.model.Move
import com.rubik3d.tutorial.TutorialCategory
import com.rubik3d.tutorial.TutorialLesson
import com.rubik3d.tutorial.TutorialRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TutorialTest {

    private lateinit var cube: CubeState
    private lateinit var rotationManager: RotationManager
    private lateinit var player: AlgorithmPlayer

    private class InstantScheduler : StepScheduler {
        override fun postDelayed(delayMs: Long, action: () -> Unit) {
            action.invoke()
        }
        override fun cancelAll() {}
    }

    @Before
    fun setUp() {
        cube = CubeState()
        rotationManager = RotationManager(cube)
        player = AlgorithmPlayer(cube, rotationManager, InstantScheduler())
    }

    @Test
    fun lessonRepositoryIsNotEmpty() {
        assertTrue("TutorialRepository must contain lessons", TutorialRepository.LESSONS.isNotEmpty())
    }

    @Test
    fun allLessonsHaveUniqueIds() {
        val ids = TutorialRepository.LESSONS.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("Each lesson must have a unique ID", ids.size, uniqueIds.size)
    }

    @Test
    fun allLessonsHaveMandatoryMetadata() {
        for (lesson in TutorialRepository.LESSONS) {
            assertTrue("ID must not be blank for ${lesson.id}", lesson.id.isNotBlank())
            assertTrue("Title must not be blank for ${lesson.id}", lesson.title.isNotBlank())
            assertTrue("Stage must not be blank for ${lesson.id}", lesson.stage.isNotBlank())
            assertTrue("Description must not be blank for ${lesson.id}", lesson.description.isNotBlank())
            assertTrue("Notation must not be blank for ${lesson.id}", lesson.notation.isNotBlank())
        }
    }

    @Test
    fun bothBeginnerAndAdvancedCategoriesExist() {
        val categories = TutorialRepository.LESSONS.map { it.category }.toSet()
        assertTrue("Must include BEGINNER category", categories.contains(TutorialCategory.BEGINNER))
        assertTrue("Must include ADVANCED category", categories.contains(TutorialCategory.ADVANCED))
    }

    @Test
    fun allLessonNotationsParseValidMoves() {
        for (lesson in TutorialRepository.LESSONS) {
            val moves = Move.parseSequence(lesson.notation)
            assertTrue("Lesson ${lesson.id} notation must contain valid moves", moves.isNotEmpty())
            for (m in moves) {
                assertNotEquals("Move notation should not be empty", "", m.notation)
            }
        }
    }

    @Test
    fun allSetupScramblesParseValidMoves() {
        for (lesson in TutorialRepository.LESSONS) {
            if (lesson.setupScramble.isNotBlank()) {
                val moves = Move.parseSequence(lesson.setupScramble)
                assertTrue("Setup scramble for ${lesson.id} must parse into moves", moves.isNotEmpty())
            }
        }
    }

    @Test
    fun inverseSetupRestoresSolvedState() {
        for (lesson in TutorialRepository.LESSONS) {
            if (lesson.setupScramble.isNotBlank()) {
                cube.reset()
                assertTrue(cube.isSolved())

                val setupMoves = Move.parseSequence(lesson.setupScramble)
                for (m in setupMoves) {
                    cube.applyMove(m)
                }

                // Invert setup moves
                val inverseMoves = setupMoves.asReversed().map { it.inverse() }
                for (inv in inverseMoves) {
                    cube.applyMove(inv)
                }

                assertTrue("Inverting setup scramble for ${lesson.id} must restore solved state", cube.isSolved())
            }
        }
    }

    @Test
    fun tutorialPlaybackStepByStep() {
        val lesson = TutorialRepository.LESSONS.first { it.id == "beg_corners" }
        player.loadAlgorithm(lesson.notation)

        val totalMoves = player.moves.size
        assertEquals(4, totalMoves) // R U R' U'

        repeat(totalMoves) {
            player.nextStep()
            rotationManager.update()
        }

        assertEquals(totalMoves - 1, player.currentIndex)
        assertFalse(cube.isSolved())

        // Rewind all
        repeat(totalMoves) {
            player.prevStep()
            rotationManager.update()
        }

        assertEquals(-1, player.currentIndex)
        assertTrue(cube.isSolved())
    }
}

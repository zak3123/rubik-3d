package com.rubik3d.model

import java.util.Random

class CubeState {
    val cubies: List<Cubie>

    init {
        val list = ArrayList<Cubie>(26)
        var id = 0
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    if (x == 0 && y == 0 && z == 0) continue
                    list.add(Cubie(id++, x, y, z))
                }
            }
        }
        cubies = list
    }

    fun reset() {
        cubies.forEach { it.reset() }
    }

    /**
     * Apply move to the affected layer of cubies
     */
    fun applyMove(move: Move) {
        val quarterTurns = Math.round(move.angleDegrees / 90f)
        val affected = getCubiesInLayer(move.axis, move.layer)
        for (cubie in affected) {
            cubie.rotate(move.axis, quarterTurns, move.angleDegrees)
        }
    }

    fun getCubiesInLayer(axis: Int, layer: Int): List<Cubie> {
        return cubies.filter {
            when (axis) {
                0 -> it.cx == layer
                1 -> it.cy == layer
                2 -> it.cz == layer
                else -> false
            }
        }
    }

    /**
     * Check if all cubies are at their original positions and orientations
     */
    fun isSolved(): Boolean {
        for (cubie in cubies) {
            if (cubie.cx != cubie.ix || cubie.cy != cubie.iy || cubie.cz != cubie.iz) {
                return false
            }
            val nonZero = (if (cubie.ix != 0) 1 else 0) + (if (cubie.iy != 0) 1 else 0) + (if (cubie.iz != 0) 1 else 0)
            if (nonZero > 1) {
                if (cubie.ux[0] != 1 || cubie.uy[1] != 1 || cubie.uz[2] != 1) return false
            } else {
                if (cubie.ix != 0 && cubie.ux[0] != 1) return false
                if (cubie.iy != 0 && cubie.uy[1] != 1) return false
                if (cubie.iz != 0 && cubie.uz[2] != 1) return false
            }
        return true
    }

    /**
     * Generate random standard WCA scramble sequence
     */
    fun generateScramble(length: Int = 20): List<Move> {
        val faces = arrayOf(Face.U, Face.D, Face.F, Face.B, Face.L, Face.R)
        val modifiers = arrayOf("", "'", "2")
        val random = Random()
        val moves = ArrayList<Move>(length)
        var lastFace: Face? = null

        for (i in 0 until length) {
            var face: Face
            do {
                face = faces[random.nextInt(faces.size)]
            } while (face == lastFace)
            lastFace = face

            val mod = modifiers[random.nextInt(modifiers.size)]
            val notation = face.name + mod
            Move.parse(notation)?.let { moves.add(it) }
        }
        return moves
    }

    /**
     * Generate 54-facelet representation string:
     * Order: 9 U, 9 R, 9 F, 9 D, 9 L, 9 B
     */
    fun toFaceletString(): String {
        val faces = arrayOf(Face.U, Face.D, Face.F, Face.B, Face.L, Face.R)
        fun getStickerAt(x: Int, y: Int, z: Int, worldFace: Face): Face {
            val nonZero = (if (x != 0) 1 else 0) + (if (y != 0) 1 else 0) + (if (z != 0) 1 else 0)
            if (nonZero <= 1) return worldFace

            val cubie = cubies.firstOrNull { it.cx == x && it.cy == y && it.cz == z } ?: return Face.NONE
            for (lf in faces) {
                if (cubie.getStickerDirection(lf) == worldFace) {
                    if (lf == Face.U && cubie.iy == 1) return Face.U
                    if (lf == Face.D && cubie.iy == -1) return Face.D
                    if (lf == Face.F && cubie.iz == 1) return Face.F
                    if (lf == Face.B && cubie.iz == -1) return Face.B
                    if (lf == Face.L && cubie.ix == -1) return Face.L
                    if (lf == Face.R && cubie.ix == 1) return Face.R
                }
            }
            return Face.NONE
        }

        val sb = StringBuilder(54)
        for (z in -1..1) {
            for (x in -1..1) sb.append(getStickerAt(x, 1, z, Face.U).name)
        }
        for (y in 1 downTo -1) {
            for (z in 1 downTo -1) sb.append(getStickerAt(1, y, z, Face.R).name)
        }
        for (y in 1 downTo -1) {
            for (x in -1..1) sb.append(getStickerAt(x, y, 1, Face.F).name)
        }
        for (z in 1 downTo -1) {
            for (x in -1..1) sb.append(getStickerAt(x, -1, z, Face.D).name)
        }
        for (y in 1 downTo -1) {
            for (z in -1..1) sb.append(getStickerAt(-1, y, z, Face.L).name)
        }
        for (y in 1 downTo -1) {
            for (x in 1 downTo -1) sb.append(getStickerAt(x, y, -1, Face.B).name)
        }
        return sb.toString()
    }
}

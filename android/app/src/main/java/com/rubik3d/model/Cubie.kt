package com.rubik3d.model

import android.opengl.Matrix

class Cubie(
    val id: Int,
    val ix: Int,
    val iy: Int,
    val iz: Int
) {
    var cx: Int = ix
        private set
    var cy: Int = iy
        private set
    var cz: Int = iz
        private set

    val ux = intArrayOf(1, 0, 0)
    val uy = intArrayOf(0, 1, 0)
    val uz = intArrayOf(0, 0, 1)
    // Sticker colors on local faces (+X=R, -X=L, +Y=U, -Y=D, +Z=F, -Z=B)
    val localStickers = mapOf(
        Face.R to (if (ix == 1) Face.R else Face.NONE),
        Face.L to (if (ix == -1) Face.L else Face.NONE),
        Face.U to (if (iy == 1) Face.U else Face.NONE),
        Face.D to (if (iy == -1) Face.D else Face.NONE),
        Face.F to (if (iz == 1) Face.F else Face.NONE),
        Face.B to (if (iz == -1) Face.B else Face.NONE)
    )

    // 4x4 model rotation matrix
    val rotationMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val rotStepMatrix = FloatArray(16)

    init {
        Matrix.setIdentityM(rotationMatrix, 0)
    }

    fun reset() {
        cx = ix
        cy = iy
        cz = iz
        ux[0] = 1; ux[1] = 0; ux[2] = 0
        uy[0] = 0; uy[1] = 1; uy[2] = 0
        uz[0] = 0; uz[1] = 0; uz[2] = 1
        Matrix.setIdentityM(rotationMatrix, 0)
    }

    /**
     * Rotate discrete integer coordinates and cumulative orientation matrix
     */
    fun rotate(axis: Int, quarterTurns: Int, angleDegrees: Float) {
        // 1. Update discrete grid position
        val q = ((quarterTurns % 4) + 4) % 4
        when (axis) {
            0 -> { // X axis
                for (i in 0 until q) {
                    val ny = -cz
                    val nz = cy
                    cy = ny
                    cz = nz
                }
            }
            1 -> { // Y axis
                for (i in 0 until q) {
                    val nx = cz
                    val nz = -cx
                    cx = nx
                    cz = nz
                }
            }
            2 -> { // Z axis
                for (i in 0 until q) {
                    val nx = -cy
                    val ny = cx
                    cx = nx
                    cy = ny
                }
            }
        }
        // 2. Update direction vectors
        val vectors = arrayOf(ux, uy, uz)
        when (axis) {
            0 -> {
                for (v in vectors) {
                    for (i in 0 until q) {
                        val ny = -v[2]
                        val nz = v[1]
                        v[1] = ny
                        v[2] = nz
                    }
                }
            }
            1 -> {
                for (v in vectors) {
                    for (i in 0 until q) {
                        val nx = v[2]
                        val nz = -v[0]
                        v[0] = nx
                        v[2] = nz
                    }
                }
            }
            2 -> {
                for (v in vectors) {
                    for (i in 0 until q) {
                        val nx = -v[1]
                        val ny = v[0]
                        v[0] = nx
                        v[1] = ny
                    }
                }
            }
        }

        Matrix.setIdentityM(rotStepMatrix, 0)
        when (axis) {
            0 -> Matrix.setRotateM(rotStepMatrix, 0, angleDegrees, 1f, 0f, 0f)
            1 -> Matrix.setRotateM(rotStepMatrix, 0, angleDegrees, 0f, 1f, 0f)
            2 -> Matrix.setRotateM(rotStepMatrix, 0, angleDegrees, 0f, 0f, 1f)
        }
        Matrix.multiplyMM(tempMatrix, 0, rotStepMatrix, 0, rotationMatrix, 0)
        System.arraycopy(tempMatrix, 0, rotationMatrix, 0, 16)
    }

    fun getStickerDirection(localFace: Face): Face {
        val v = when (localFace) {
            Face.R -> ux
            Face.L -> intArrayOf(-ux[0], -ux[1], -ux[2])
            Face.U -> uy
            Face.D -> intArrayOf(-uy[0], -uy[1], -uy[2])
            Face.F -> uz
            Face.B -> intArrayOf(-uz[0], -uz[1], -uz[2])
            Face.NONE -> return Face.NONE
        }
        return when {
            v[0] == 1 -> Face.R
            v[0] == -1 -> Face.L
            v[1] == 1 -> Face.U
            v[1] == -1 -> Face.D
            v[2] == 1 -> Face.F
            v[2] == -1 -> Face.B
            else -> Face.NONE
        }
    }
}

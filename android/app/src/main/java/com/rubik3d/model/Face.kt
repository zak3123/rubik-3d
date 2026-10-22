package com.rubik3d.model

enum class Face(
    val nx: Float,
    val ny: Float,
    val nz: Float,
    val colorRgba: FloatArray
) {
    U(0f, 1f, 0f, floatArrayOf(0.96f, 0.96f, 0.97f, 1f)),     // White
    D(0f, -1f, 0f, floatArrayOf(1.0f, 0.83f, 0.0f, 1f)),     // Yellow
    F(0f, 0f, 1f, floatArrayOf(0.0f, 0.62f, 0.26f, 1f)),     // Green
    B(0f, 0f, -1f, floatArrayOf(0.0f, 0.32f, 0.73f, 1f)),    // Blue
    L(-1f, 0f, 0f, floatArrayOf(1.0f, 0.35f, 0.0f, 1f)),    // Orange
    R(1f, 0f, 0f, floatArrayOf(0.77f, 0.12f, 0.23f, 1f)),    // Red
    NONE(0f, 0f, 0f, floatArrayOf(0.07f, 0.07f, 0.08f, 1f)); // Plastic body

    companion object {
        fun fromNormal(nx: Float, ny: Float, nz: Float): Face {
            val ax = Math.abs(nx)
            val ay = Math.abs(ny)
            val az = Math.abs(nz)
            return when {
                ax > ay && ax > az -> if (nx > 0) R else L
                ay > ax && ay > az -> if (ny > 0) U else D
                az > ax && az > ay -> if (nz > 0) F else B
                else -> NONE
            }
        }
    }
}

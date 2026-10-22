package com.rubik3d.model

data class Move(
    val notation: String,
    val face: Face,
    val axis: Int, // 0 = X, 1 = Y, 2 = Z
    val layer: Int, // -1 or 1
    val angleDegrees: Float // signed angle in degrees
) {
    fun inverse(): Move {
        val invNotation = when {
            notation.endsWith("2") -> notation
            notation.endsWith("'") -> notation.dropLast(1)
            else -> "$notation'"
        }
        return Move(
            notation = invNotation,
            face = face,
            axis = axis,
            layer = layer,
            angleDegrees = -angleDegrees
        )
    }

    companion object {
        val R = Move("R", Face.R, 0, 1, -90f)
        val R_PRIME = Move("R'", Face.R, 0, 1, 90f)
        val R2 = Move("R2", Face.R, 0, 1, -180f)

        val L = Move("L", Face.L, 0, -1, 90f)
        val L_PRIME = Move("L'", Face.L, 0, -1, -90f)
        val L2 = Move("L2", Face.L, 0, -1, 180f)

        val U = Move("U", Face.U, 1, 1, -90f)
        val U_PRIME = Move("U'", Face.U, 1, 1, 90f)
        val U2 = Move("U2", Face.U, 1, 1, -180f)

        val D = Move("D", Face.D, 1, -1, 90f)
        val D_PRIME = Move("D'", Face.D, 1, -1, -90f)
        val D2 = Move("D2", Face.D, 1, -1, 180f)

        val F = Move("F", Face.F, 2, 1, -90f)
        val F_PRIME = Move("F'", Face.F, 2, 1, 90f)
        val F2 = Move("F2", Face.F, 2, 1, -180f)

        val B = Move("B", Face.B, 2, -1, 90f)
        val B_PRIME = Move("B'", Face.B, 2, -1, -90f)
        val B2 = Move("B2", Face.B, 2, -1, 180f)

        private val ALL_MOVES = listOf(
            R, R_PRIME, R2,
            L, L_PRIME, L2,
            U, U_PRIME, U2,
            D, D_PRIME, D2,
            F, F_PRIME, F2,
            B, B_PRIME, B2
        ).associateBy { it.notation }

        fun parse(token: String): Move? = ALL_MOVES[token.trim()]

        fun parseSequence(sequence: String): List<Move> {
            return sequence.trim().split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .mapNotNull { parse(it) }
        }
    }
}

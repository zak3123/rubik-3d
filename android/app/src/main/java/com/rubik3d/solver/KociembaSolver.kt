package com.rubik3d.solver

import com.rubik3d.model.CubeState
import com.rubik3d.model.Move

/**
 * Kociemba Two-Phase Algorithm Solver for Rubik's Cube.
 * Computes optimal/sub-optimal solutions (<= 23-25 moves) offline.
 */
object KociembaSolver {

    // 8 Corners
    const val URF = 0
    const val UFL = 1
    const val ULB = 2
    const val UBR = 3
    const val DFR = 4
    const val DLF = 5
    const val DBL = 6
    const val DRB = 7

    // 12 Edges
    const val UR = 0
    const val UF = 1
    const val UL = 2
    const val UB = 3
    const val DR = 4
    const val DF = 5
    const val DL = 6
    const val DB = 7
    const val FR = 8
    const val FL = 9
    const val BL = 10
    const val BR = 11

    // Binomial coefficient table C(n, k)
    private val C_NK = Array(13) { n ->
        IntArray(13) { k ->
            if (k > n || k < 0) 0
            else if (k == 0 || k == n) 1
            else {
                var c = 1
                for (i in 1..k) {
                    c = c * (n - i + 1) / i
                }
                c
            }
        }
    }

    class CubieCube {
        var cp = intArrayOf(URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB)
        var co = IntArray(8)
        var ep = intArrayOf(UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR)
        var eo = IntArray(12)

        fun copy(): CubieCube {
            val c = CubieCube()
            System.arraycopy(cp, 0, c.cp, 0, 8)
            System.arraycopy(co, 0, c.co, 0, 8)
            System.arraycopy(ep, 0, c.ep, 0, 12)
            System.arraycopy(eo, 0, c.eo, 0, 12)
            return c
        }

        fun multiply(b: CubieCube): CubieCube {
            val res = CubieCube()
            for (c in 0 until 8) {
                res.cp[c] = this.cp[b.cp[c]]
                res.co[c] = (this.co[b.cp[c]] + b.co[c]) % 3
            }
            for (e in 0 until 12) {
                res.ep[e] = this.ep[b.ep[e]]
                res.eo[e] = (this.eo[b.ep[e]] + b.eo[e]) % 2
            }
            return res
        }

        fun getTwist(): Int {
            var ret = 0
            for (i in 0 until 7) {
                ret = 3 * ret + co[i]
            }
            return ret
        }

        fun setTwist(twist: Int) {
            var t = twist
            var twistSum = 0
            for (i in 6 downTo 0) {
                co[i] = t % 3
                twistSum += co[i]
                t /= 3
            }
            co[7] = (3 - (twistSum % 3)) % 3
        }

        fun getFlip(): Int {
            var ret = 0
            for (i in 0 until 11) {
                ret = 2 * ret + eo[i]
            }
            return ret
        }

        fun setFlip(flip: Int) {
            var f = flip
            var flipSum = 0
            for (i in 10 downTo 0) {
                eo[i] = f % 2
                flipSum += eo[i]
                f /= 2
            }
            eo[11] = (2 - (flipSum % 2)) % 2
        }

        fun getSlice(): Int {
            var a = 0
            var x = 0
            for (j in 11 downTo 0) {
                if (ep[j] in FR..BR) {
                    a += C_NK[11 - j][x + 1]
                    x++
                }
            }
            return a
        }

        fun setSlice(slice: Int) {
            val sliceEdge = intArrayOf(FR, FL, BL, BR)
            val otherEdge = intArrayOf(UR, UF, UL, UB, DR, DF, DL, DB)
            var a = slice
            var x = 4
            for (j in 0 until 12) ep[j] = -1
            for (j in 0 until 12) {
                if (a >= C_NK[11 - j][x]) {
                    ep[j] = sliceEdge[4 - x]
                    a -= C_NK[11 - j][x]
                    x--
                }
            }
            var y = 0
            for (j in 0 until 12) {
                if (ep[j] == -1) {
                    ep[j] = otherEdge[y++]
                }
            }
        }

        fun getCornerPerm(): Int {
            var a = 0
            for (j in 7 downTo 1) {
                var s = 0
                for (k in j - 1 downTo 0) {
                    if (cp[k] > cp[j]) s++
                }
                a = (a + s) * j
            }
            return a
        }

        fun setCornerPerm(cpVal: Int) {
            val p = IntArray(8)
            var a = cpVal
            for (j in 1..8) {
                p[j - 1] = a % j
                a /= j
            }
            val avail = arrayListOf(0, 1, 2, 3, 4, 5, 6, 7)
            for (j in 7 downTo 0) {
                cp[j] = avail.removeAt(avail.size - 1 - p[j])
            }
        }

        fun getEdgePerm(): Int {
            var a = 0
            for (j in 7 downTo 1) {
                var s = 0
                for (k in j - 1 downTo 0) {
                    if (ep[k] > ep[j]) s++
                }
                a = (a + s) * j
            }
            return a
        }

        fun getSlicePerm(): Int {
            val perm = IntArray(4) { ep[8 + it] - 8 }
            var a = 0
            for (j in 3 downTo 1) {
                var s = 0
                for (k in j - 1 downTo 0) {
                    if (perm[k] > perm[j]) s++
                }
                a = (a + s) * j
            }
            return a
        }
    }

    val moveNames = arrayOf(
        "U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'",
        "D", "D2", "D'", "L", "L2", "L'", "B", "B2", "B'"
    )

    val P2_MOVE_INDICES = intArrayOf(0, 1, 2, 9, 10, 11, 4, 13, 7, 16)

    val moveCube = ArrayList<CubieCube>(18)

    // Tables
    private val twistMove = Array(2187) { ShortArray(18) }
    private val flipMove = Array(2048) { ShortArray(18) }
    private val sliceMove = Array(495) { ShortArray(18) }

    private val cornerPermMove = Array(40320) { IntArray(10) }
    private val edgePermMove = Array(40320) { IntArray(10) }
    private val slicePermMove = Array(24) { ByteArray(10) }

    private val sliceFlipPrun = ByteArray(495 * 2048) { -1 }
    private val sliceTwistPrun = ByteArray(495 * 2187) { -1 }
    private val sliceCornerPrun = ByteArray(24 * 40320) { -1 }
    private val sliceEdgePrun = ByteArray(24 * 40320) { -1 }

    private val cornerFacelet = arrayOf(
        intArrayOf(8, 9, 20),   // URF: U, R, F
        intArrayOf(6, 18, 38),  // UFL: U, F, L
        intArrayOf(0, 36, 47),  // ULB: U, L, B
        intArrayOf(2, 45, 11),  // UBR: U, B, R
        intArrayOf(29, 26, 15), // DFR: D, F, R
        intArrayOf(27, 44, 24), // DLF: D, L, F
        intArrayOf(33, 53, 42), // DBL: D, B, L
        intArrayOf(35, 17, 51)  // DRB: D, R, B
    )

    private val cornerColor = arrayOf(
        charArrayOf('U', 'R', 'F'),
        charArrayOf('U', 'F', 'L'),
        charArrayOf('U', 'L', 'B'),
        charArrayOf('U', 'B', 'R'),
        charArrayOf('D', 'F', 'R'),
        charArrayOf('D', 'L', 'F'),
        charArrayOf('D', 'B', 'L'),
        charArrayOf('D', 'R', 'B')
    )

    private val edgeFacelet = arrayOf(
        intArrayOf(5, 10),  // UR
        intArrayOf(7, 19),  // UF
        intArrayOf(3, 37),  // UL
        intArrayOf(1, 46),  // UB
        intArrayOf(32, 16), // DR
        intArrayOf(28, 25), // DF
        intArrayOf(30, 43), // DL
        intArrayOf(34, 52), // DB
        intArrayOf(23, 12), // FR
        intArrayOf(21, 41), // FL
        intArrayOf(50, 39), // BL
        intArrayOf(48, 14)  // BR
    )

    private val edgeColor = arrayOf(
        charArrayOf('U', 'R'),
        charArrayOf('U', 'F'),
        charArrayOf('U', 'L'),
        charArrayOf('U', 'B'),
        charArrayOf('D', 'R'),
        charArrayOf('D', 'F'),
        charArrayOf('D', 'L'),
        charArrayOf('D', 'B'),
        charArrayOf('F', 'R'),
        charArrayOf('F', 'L'),
        charArrayOf('B', 'L'),
        charArrayOf('B', 'R')
    )

    init {
        initBasicMoves()
        initMoveTables()
        initPrunTables()
    }

    /**
     * Trigger initialization in background thread
     */
    fun warmup() {
        // Accessing moveCube or calling warmup forces `init` to complete
    }

    private fun initBasicMoves() {
        val basicMoves = Array(6) { CubieCube() }

        // U
        basicMoves[0].cp = intArrayOf(UBR, URF, UFL, ULB, DFR, DLF, DBL, DRB)
        basicMoves[0].co = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        basicMoves[0].ep = intArrayOf(UB, UR, UF, UL, DR, DF, DL, DB, FR, FL, BL, BR)
        basicMoves[0].eo = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        // R
        basicMoves[1].cp = intArrayOf(DFR, UFL, ULB, URF, DRB, DLF, DBL, UBR)
        basicMoves[1].co = intArrayOf(2, 0, 0, 1, 1, 0, 0, 2)
        basicMoves[1].ep = intArrayOf(FR, UF, UL, UB, BR, DF, DL, DB, DR, FL, BL, UR)
        basicMoves[1].eo = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        // F
        basicMoves[2].cp = intArrayOf(UFL, DLF, ULB, UBR, URF, DFR, DBL, DRB)
        basicMoves[2].co = intArrayOf(1, 2, 0, 0, 2, 1, 0, 0)
        basicMoves[2].ep = intArrayOf(UR, FL, UL, UB, DR, FR, DL, DB, UF, DF, BL, BR)
        basicMoves[2].eo = intArrayOf(0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0)

        // D
        basicMoves[3].cp = intArrayOf(URF, UFL, ULB, UBR, DLF, DBL, DRB, DFR)
        basicMoves[3].co = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        basicMoves[3].ep = intArrayOf(UR, UF, UL, UB, DF, DL, DB, DR, FR, FL, BL, BR)
        basicMoves[3].eo = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        // L
        basicMoves[4].cp = intArrayOf(URF, ULB, DBL, UBR, DFR, UFL, DLF, DRB)
        basicMoves[4].co = intArrayOf(0, 1, 2, 0, 0, 2, 1, 0)
        basicMoves[4].ep = intArrayOf(UR, UF, BL, UB, DR, DF, FL, DB, FR, UL, DL, BR)
        basicMoves[4].eo = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        // B
        basicMoves[5].cp = intArrayOf(URF, UFL, UBR, DRB, DFR, DLF, ULB, DBL)
        basicMoves[5].co = intArrayOf(0, 0, 1, 2, 0, 0, 2, 1)
        basicMoves[5].ep = intArrayOf(UR, UF, UL, BR, DR, DF, DL, BL, FR, FL, UB, DB)
        basicMoves[5].eo = intArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1)

        moveCube.clear()
        for (f in 0 until 6) {
            var cur = CubieCube()
            for (p in 0 until 3) {
                cur = cur.multiply(basicMoves[f])
                moveCube.add(cur)
            }
        }
    }

    private fun setSlicePerm(c: CubieCube, idx: Int) {
        var a = idx
        val p = IntArray(4)
        for (j in 1..4) {
            p[j - 1] = a % j
            a /= j
        }
        val avail = arrayListOf(FR, FL, BL, BR)
        for (j in 3 downTo 0) {
            c.ep[8 + j] = avail.removeAt(avail.size - 1 - p[j])
        }
    }

    private fun setUDEdgePerm(c: CubieCube, idx: Int) {
        var a = idx
        val p = IntArray(8)
        for (j in 1..8) {
            p[j - 1] = a % j
            a /= j
        }
        val avail = arrayListOf(UR, UF, UL, UB, DR, DF, DL, DB)
        for (j in 7 downTo 0) {
            c.ep[j] = avail.removeAt(avail.size - 1 - p[j])
        }
    }

    private fun initMoveTables() {
        val a = CubieCube()
        for (u in 0 until 2187) {
            a.setTwist(u)
            for (m in 0 until 18) twistMove[u][m] = a.multiply(moveCube[m]).getTwist().toShort()
        }
        for (u in 0 until 2048) {
            a.setFlip(u)
            for (m in 0 until 18) flipMove[u][m] = a.multiply(moveCube[m]).getFlip().toShort()
        }
        for (u in 0 until 495) {
            a.setSlice(u)
            for (m in 0 until 18) sliceMove[u][m] = a.multiply(moveCube[m]).getSlice().toShort()
        }

        val cCube = CubieCube()
        for (u in 0 until 40320) {
            cCube.setCornerPerm(u)
            for (m in 0 until 10) {
                cornerPermMove[u][m] = cCube.multiply(moveCube[P2_MOVE_INDICES[m]]).getCornerPerm()
            }
        }

        for (u in 0 until 24) {
            val c = CubieCube()
            setSlicePerm(c, u)
            for (m in 0 until 10) {
                slicePermMove[u][m] = c.multiply(moveCube[P2_MOVE_INDICES[m]]).getSlicePerm().toByte()
            }
        }

        for (u in 0 until 40320) {
            val c = CubieCube()
            setUDEdgePerm(c, u)
            for (m in 0 until 10) {
                edgePermMove[u][m] = c.multiply(moveCube[P2_MOVE_INDICES[m]]).getEdgePerm()
            }
        }
    }

    private fun initPrunTables() {
        // Phase 1: Slice x Flip
        sliceFlipPrun[0] = 0
        var filled = 1
        var d = 0
        while (d < 10 && filled < 495 * 2048) {
            for (i in 0 until 495 * 2048) {
                if (sliceFlipPrun[i].toInt() == d) {
                    val s = i / 2048
                    val f = i % 2048
                    for (m in 0 until 18) {
                        val ns = sliceMove[s][m].toInt()
                        val nf = flipMove[f][m].toInt()
                        val ni = ns * 2048 + nf
                        if (sliceFlipPrun[ni] == (-1).toByte()) {
                            sliceFlipPrun[ni] = (d + 1).toByte()
                            filled++
                        }
                    }
                }
            }
            d++
        }

        // Phase 1: Slice x Twist
        sliceTwistPrun[0] = 0
        filled = 1
        d = 0
        while (d < 10 && filled < 495 * 2187) {
            for (i in 0 until 495 * 2187) {
                if (sliceTwistPrun[i].toInt() == d) {
                    val s = i / 2187
                    val t = i % 2187
                    for (m in 0 until 18) {
                        val ns = sliceMove[s][m].toInt()
                        val nt = twistMove[t][m].toInt()
                        val ni = ns * 2187 + nt
                        if (sliceTwistPrun[ni] == (-1).toByte()) {
                            sliceTwistPrun[ni] = (d + 1).toByte()
                            filled++
                        }
                    }
                }
            }
            d++
        }

        // Phase 2: SlicePerm x CornerPerm
        sliceCornerPrun[0] = 0
        filled = 1
        d = 0
        while (d < 14 && filled < 24 * 40320) {
            for (i in 0 until 24 * 40320) {
                if (sliceCornerPrun[i].toInt() == d) {
                    val sp = i / 40320
                    val cp = i % 40320
                    for (m in 0 until 10) {
                        val nsp = slicePermMove[sp][m].toInt()
                        val ncp = cornerPermMove[cp][m]
                        val ni = nsp * 40320 + ncp
                        if (sliceCornerPrun[ni] == (-1).toByte()) {
                            sliceCornerPrun[ni] = (d + 1).toByte()
                            filled++
                        }
                    }
                }
            }
            d++
        }

        // Phase 2: SlicePerm x EdgePerm
        sliceEdgePrun[0] = 0
        filled = 1
        d = 0
        while (d < 14 && filled < 24 * 40320) {
            for (i in 0 until 24 * 40320) {
                if (sliceEdgePrun[i].toInt() == d) {
                    val sp = i / 40320
                    val ep = i % 40320
                    for (m in 0 until 10) {
                        val nsp = slicePermMove[sp][m].toInt()
                        val nep = edgePermMove[ep][m]
                        val ni = nsp * 40320 + nep
                        if (sliceEdgePrun[ni] == (-1).toByte()) {
                            sliceEdgePrun[ni] = (d + 1).toByte()
                            filled++
                        }
                    }
                }
            }
            d++
        }
    }

    /**
     * Convert 54-facelet representation string to CubieCube
     */
    fun faceletToCubie(fStr: String): CubieCube? {
        if (fStr.length != 54) return null
        val cc = CubieCube()

        for (i in 0 until 8) {
            val f = cornerFacelet[i]
            val col0 = fStr[f[0]]
            val col1 = fStr[f[1]]
            val col2 = fStr[f[2]]
            val ori = when {
                col0 == 'U' || col0 == 'D' -> 0
                col1 == 'U' || col1 == 'D' -> 1
                col2 == 'U' || col2 == 'D' -> 2
                else -> return null
            }
            cc.co[i] = ori

            val sorted = charArrayOf(col0, col1, col2).sorted()
            var found = false
            for (c in 0 until 8) {
                val expSorted = cornerColor[c].sorted()
                if (sorted == expSorted) {
                    cc.cp[i] = c
                    found = true
                    break
                }
            }
            if (!found) return null
        }

        for (i in 0 until 12) {
            val f = edgeFacelet[i]
            val col0 = fStr[f[0]]
            val col1 = fStr[f[1]]
            val sorted = charArrayOf(col0, col1).sorted()
            var found = false
            for (e in 0 until 12) {
                val expSorted = edgeColor[e].sorted()
                if (sorted == expSorted) {
                    cc.ep[i] = e
                    cc.eo[i] = if (col0 == edgeColor[e][0]) 0 else 1
                    found = true
                    break
                }
            }
            if (!found) return null
        }
        return cc
    }

    private fun searchPhase2(cube: CubieCube, phase1Moves: List<Int>): List<Int>? {
        var cur = cube.copy()
        for (m in phase1Moves) {
            cur = cur.multiply(moveCube[m])
        }
        val cp0 = cur.getCornerPerm()
        val ep0 = cur.getEdgePerm()
        val sp0 = cur.getSlicePerm()

        val p2Moves = ArrayList<Int>(15)

        fun search2(cp: Int, ep: Int, sp: Int, maxDepth: Int, lastFace: Int): Boolean {
            if (maxDepth == 0) {
                return cp == 0 && ep == 0 && sp == 0
            }
            val h1 = sliceCornerPrun[sp * 40320 + cp].toInt()
            val h2 = sliceEdgePrun[sp * 40320 + ep].toInt()
            val h = if (h1 > h2) h1 else h2
            if (h > maxDepth) return false

            for (m in 0 until 10) {
                val moveIdx18 = P2_MOVE_INDICES[m]
                val face = moveIdx18 / 3
                if (face == lastFace) continue
                if (face == 3 && lastFace == 0) continue
                if (face == 4 && lastFace == 1) continue
                if (face == 5 && lastFace == 2) continue

                val ncp = cornerPermMove[cp][m]
                val nep = edgePermMove[ep][m]
                val nsp = slicePermMove[sp][m].toInt()

                p2Moves.add(moveIdx18)
                if (search2(ncp, nep, nsp, maxDepth - 1, face)) return true
                p2Moves.removeAt(p2Moves.size - 1)
            }
            return false
        }

        for (d2 in 0..14) {
            if (search2(cp0, ep0, sp0, d2, -1)) {
                return p2Moves
            }
        }
        return null
    }

    private fun search1(
        twist: Int,
        flip: Int,
        slice: Int,
        maxDepth: Int,
        lastFace: Int,
        cube: CubieCube,
        p1Moves: ArrayList<Int>
    ): List<Int>? {
        if (maxDepth == 0) {
            if (twist == 0 && flip == 0 && slice == 0) {
                val p2 = searchPhase2(cube, p1Moves)
                if (p2 != null) {
                    val full = ArrayList<Int>(p1Moves.size + p2.size)
                    full.addAll(p1Moves)
                    full.addAll(p2)
                    return full
                }
            }
            return null
        }

        val h1 = sliceFlipPrun[slice * 2048 + flip].toInt()
        val h2 = sliceTwistPrun[slice * 2187 + twist].toInt()
        val h = if (h1 > h2) h1 else h2
        if (h > maxDepth) return null

        for (m in 0 until 18) {
            val face = m / 3
            if (face == lastFace) continue
            if (face == 3 && lastFace == 0) continue
            if (face == 4 && lastFace == 1) continue
            if (face == 5 && lastFace == 2) continue

            val nt = twistMove[twist][m].toInt()
            val nf = flipMove[flip][m].toInt()
            val ns = sliceMove[slice][m].toInt()

            p1Moves.add(m)
            val res = search1(nt, nf, ns, maxDepth - 1, face, cube, p1Moves)
            if (res != null) return res
            p1Moves.removeAt(p1Moves.size - 1)
        }
        return null
    }

    /**
     * Solve from CubieCube
     */
    fun solve(cube: CubieCube): List<Move>? {
        val twist0 = cube.getTwist()
        val flip0 = cube.getFlip()
        val slice0 = cube.getSlice()
        if (twist0 == 0 && flip0 == 0 && slice0 == 0 &&
            cube.getCornerPerm() == 0 && cube.getEdgePerm() == 0 && cube.getSlicePerm() == 0) {
            return emptyList()
        }

        val p1Moves = ArrayList<Int>(15)
        for (depth1 in 0..12) {
            val sol = search1(twist0, flip0, slice0, depth1, -1, cube, p1Moves)
            if (sol != null) {
                return sol.mapNotNull { Move.parse(moveNames[it]) }
            }
        }
        return null
    }

    /**
     * Solve from 3D CubeState directly
     */
    fun solve(cubeState: CubeState): List<Move>? {
        if (cubeState.isSolved()) return emptyList()
        val fStr = cubeState.toFaceletString()
        val cc = faceletToCubie(fStr) ?: return null
        return solve(cc)
    }
}

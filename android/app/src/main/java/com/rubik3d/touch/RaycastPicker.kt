package com.rubik3d.touch

import android.opengl.Matrix
import com.rubik3d.gl.CubeRenderer
import com.rubik3d.model.Cubie
import com.rubik3d.model.Face

class RaycastPicker(private val renderer: CubeRenderer) {

    data class HitResult(
        val cubie: Cubie,
        val normalFace: Face,
        val distance: Float,
        val hitPoint: FloatArray
    )

    private val invVP = FloatArray(16)
    private val nearPoint = FloatArray(4)
    private val farPoint = FloatArray(4)
    private val tempVec = FloatArray(4)

    /**
     * Cast ray from screen coordinates (x, y) into 3D scene
     */
    fun pick(screenX: Float, screenY: Float): HitResult? {
        val vp = renderer.viewport
        val width = vp[2].toFloat()
        val height = vp[3].toFloat()
        if (width <= 0 || height <= 0) return null

        // Invert Y for OpenGL NDC
        val ndcX = (screenX / width) * 2f - 1f
        val ndcY = -((screenY / height) * 2f - 1f)

        if (!Matrix.invertM(invVP, 0, renderer.vpMatrix, 0)) {
            return null
        }

        // Near plane point (ndcZ = -1)
        tempVec[0] = ndcX; tempVec[1] = ndcY; tempVec[2] = -1f; tempVec[3] = 1f
        Matrix.multiplyMV(nearPoint, 0, invVP, 0, tempVec, 0)
        val nw = nearPoint[3]
        val rx0 = nearPoint[0] / nw
        val ry0 = nearPoint[1] / nw
        val rz0 = nearPoint[2] / nw

        // Far plane point (ndcZ = 1)
        tempVec[0] = ndcX; tempVec[1] = ndcY; tempVec[2] = 1f; tempVec[3] = 1f
        Matrix.multiplyMV(farPoint, 0, invVP, 0, tempVec, 0)
        val fw = farPoint[3]
        val rx1 = farPoint[0] / fw
        val ry1 = farPoint[1] / fw
        val rz1 = farPoint[2] / fw

        // Ray direction
        var dx = rx1 - rx0
        var dy = ry1 - ry0
        var dz = rz1 - rz0
        val len = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
        if (len <= 0.0001f) return null
        dx /= len; dy /= len; dz /= len

        var closestHit: HitResult? = null
        var minDistance = Float.MAX_VALUE

        val half = 0.5f

        for (cubie in renderer.cubeState.cubies) {
            val cx = cubie.cx.toFloat()
            val cy = cubie.cy.toFloat()
            val cz = cubie.cz.toFloat()

            // Ray - AABB intersection (Slab method)
            val t1x = (cx - half - rx0) / dx
            val t2x = (cx + half - rx0) / dx
            val tNearX = Math.min(t1x, t2x)
            val tFarX = Math.max(t1x, t2x)

            val t1y = (cy - half - ry0) / dy
            val t2y = (cy + half - ry0) / dy
            val tNearY = Math.min(t1y, t2y)
            val tFarY = Math.max(t1y, t2y)

            val t1z = (cz - half - rz0) / dz
            val t2z = (cz + half - rz0) / dz
            val tNearZ = Math.min(t1z, t2z)
            val tFarZ = Math.max(t1z, t2z)

            val tEnter = Math.max(Math.max(tNearX, tNearY), tNearZ)
            val tExit = Math.min(Math.min(tFarX, tFarY), tFarZ)

            if (tEnter <= tExit && tExit > 0) {
                val tHit = if (tEnter > 0) tEnter else tExit
                if (tHit < minDistance) {
                    minDistance = tHit
                    val hx = rx0 + dx * tHit
                    val hy = ry0 + dy * tHit
                    val hz = rz0 + dz * tHit

                    // Determine which of the 6 faces was hit
                    val normX = (hx - cx) / half
                    val normY = (hy - cy) / half
                    val normZ = (hz - cz) / half
                    val face = Face.fromNormal(normX, normY, normZ)

                    closestHit = HitResult(
                        cubie = cubie,
                        normalFace = face,
                        distance = tHit,
                        hitPoint = floatArrayOf(hx, hy, hz)
                    )
                }
            }
        }
        return closestHit
    }
}

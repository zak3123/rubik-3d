package com.rubik3d.touch

import android.content.Context
import android.opengl.Matrix
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import com.rubik3d.gl.CubeRenderer
import com.rubik3d.gl.RotationManager
import com.rubik3d.model.Cubie
import com.rubik3d.model.Face

class CubeTouchHandler(
    context: Context,
    private val renderer: CubeRenderer,
    private val rotationManager: RotationManager
) {
    private enum class TouchMode { NONE, ORBIT, PENDING_DRAG, LAYER_DRAG }

    private var mode = TouchMode.NONE
    private val picker = RaycastPicker(renderer)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f

    private var hitCubie: Cubie? = null
    private var hitFace: Face = Face.NONE
    private var hitPoint = FloatArray(3)

    private var dragAxis = 0
    private var dragLayer = 0
    private var dragDirX = 0f
    private var dragDirY = 0f

    private val tempVec4 = FloatArray(4)
    private val projVec4 = FloatArray(4)

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.zoomCamera(detector.scaleFactor)
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        if (event.pointerCount > 1) {
            if (mode == TouchMode.LAYER_DRAG) {
                rotationManager.endDrag()
            }
            mode = TouchMode.NONE
            return true
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                lastX = x
                lastY = y

                if (rotationManager.activeMove != null) {
                    mode = TouchMode.NONE
                    return true
                }

                val hit = picker.pick(x, y)
                if (hit != null && hit.normalFace != Face.NONE) {
                    hitCubie = hit.cubie
                    hitFace = hit.normalFace
                    System.arraycopy(hit.hitPoint, 0, hitPoint, 0, 3)
                    mode = TouchMode.PENDING_DRAG
                } else {
                    hitCubie = null
                    hitFace = Face.NONE
                    mode = TouchMode.ORBIT
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX
                val dy = y - lastY
                val totalDx = x - startX
                val totalDy = y - startY
                val totalDistSq = totalDx * totalDx + totalDy * totalDy

                when (mode) {
                    TouchMode.ORBIT -> {
                        renderer.rotateCamera(dx, dy)
                    }

                    TouchMode.PENDING_DRAG -> {
                        if (totalDistSq > touchSlop * touchSlop) {
                            val cubie = hitCubie
                            val face = hitFace
                            if (cubie != null && face != Face.NONE) {
                                initLayerDrag(cubie, face, totalDx, totalDy)
                            } else {
                                mode = TouchMode.ORBIT
                            }
                        }
                    }

                    TouchMode.LAYER_DRAG -> {
                        // Project (dx, dy) onto screen drag vector
                        val dot = dx * dragDirX + dy * dragDirY
                        val angleDelta = dot * 0.75f
                        rotationManager.updateDrag(angleDelta)
                    }

                    TouchMode.NONE -> {}
                }

                lastX = x
                lastY = y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mode == TouchMode.LAYER_DRAG) {
                    rotationManager.endDrag()
                }
                mode = TouchMode.NONE
                hitCubie = null
                hitFace = Face.NONE
            }
        }
        return true
    }

    private fun projectToScreen(wx: Float, wy: Float, wz: Float, out: FloatArray) {
        tempVec4[0] = wx; tempVec4[1] = wy; tempVec4[2] = wz; tempVec4[3] = 1f
        Matrix.multiplyMV(projVec4, 0, renderer.vpMatrix, 0, tempVec4, 0)
        val w = projVec4[3]
        val vp = renderer.viewport
        val sw = vp[2].toFloat()
        val sh = vp[3].toFloat()
        val ndcX = projVec4[0] / w
        val ndcY = projVec4[1] / w
        out[0] = (ndcX + 1f) * 0.5f * sw
        out[1] = (1f - ndcY) * 0.5f * sh
    }

    private fun initLayerDrag(cubie: Cubie, face: Face, totalDx: Float, totalDy: Float) {
        val p0 = FloatArray(2)
        val p1 = FloatArray(2)
        projectToScreen(hitPoint[0], hitPoint[1], hitPoint[2], p0)

        // Find candidate tangent vectors
        val tangents: Array<FloatArray>
        val candidateRotAxes: IntArray
        val candidateLayers: IntArray
        val signModifiers: FloatArray

        when (face) {
            Face.F -> { // Normal +Z (tangents X and Y)
                tangents = arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f))
                candidateRotAxes = intArrayOf(1, 0) // drag X -> rot Y; drag Y -> rot X
                candidateLayers = intArrayOf(cubie.cy, cubie.cx)
                signModifiers = floatArrayOf(1f, -1f)
            }
            Face.B -> { // Normal -Z
                tangents = arrayOf(floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 1f, 0f))
                candidateRotAxes = intArrayOf(1, 0)
                candidateLayers = intArrayOf(cubie.cy, cubie.cx)
                signModifiers = floatArrayOf(-1f, 1f)
            }
            Face.U -> { // Normal +Y (tangents X and Z)
                tangents = arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, -1f))
                candidateRotAxes = intArrayOf(2, 0) // drag X -> rot Z; drag Z -> rot X
                candidateLayers = intArrayOf(cubie.cz, cubie.cx)
                signModifiers = floatArrayOf(-1f, 1f)
            }
            Face.D -> { // Normal -Y
                tangents = arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, 1f))
                candidateRotAxes = intArrayOf(2, 0)
                candidateLayers = intArrayOf(cubie.cz, cubie.cx)
                signModifiers = floatArrayOf(1f, -1f)
            }
            Face.R -> { // Normal +X (tangents Z and Y)
                tangents = arrayOf(floatArrayOf(0f, 0f, -1f), floatArrayOf(0f, 1f, 0f))
                candidateRotAxes = intArrayOf(1, 2) // drag Z -> rot Y; drag Y -> rot Z
                candidateLayers = intArrayOf(cubie.cy, cubie.cz)
                signModifiers = floatArrayOf(1f, 1f)
            }
            Face.L -> { // Normal -X
                tangents = arrayOf(floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 1f, 0f))
                candidateRotAxes = intArrayOf(1, 2)
                candidateLayers = intArrayOf(cubie.cy, cubie.cz)
                signModifiers = floatArrayOf(-1f, -1f)
            }
            Face.NONE -> {
                mode = TouchMode.ORBIT
                return
            }
        }

        // Project candidate 0
        projectToScreen(hitPoint[0] + tangents[0][0], hitPoint[1] + tangents[0][1], hitPoint[2] + tangents[0][2], p1)
        val s0x = p1[0] - p0[0]
        val s0y = p1[1] - p0[1]
        val len0 = Math.hypot(s0x.toDouble(), s0y.toDouble()).toFloat()

        // Project candidate 1
        projectToScreen(hitPoint[0] + tangents[1][0], hitPoint[1] + tangents[1][1], hitPoint[2] + tangents[1][2], p1)
        val s1x = p1[0] - p0[0]
        val s1y = p1[1] - p0[1]
        val len1 = Math.hypot(s1x.toDouble(), s1y.toDouble()).toFloat()

        if (len0 < 0.001f || len1 < 0.001f) {
            mode = TouchMode.ORBIT
            return
        }

        val dot0 = (totalDx * (s0x / len0) + totalDy * (s0y / len0))
        val dot1 = (totalDx * (s1x / len1) + totalDy * (s1y / len1))

        val chooseIndex = if (Math.abs(dot0) >= Math.abs(dot1)) 0 else 1
        val chosenLen = if (chooseIndex == 0) len0 else len1
        val chosenDirX = (if (chooseIndex == 0) s0x else s1x) / chosenLen
        val chosenDirY = (if (chooseIndex == 0) s0y else s1y) / chosenLen
        val modifier = signModifiers[chooseIndex]

        dragAxis = candidateRotAxes[chooseIndex]
        dragLayer = candidateLayers[chooseIndex]
        dragDirX = chosenDirX * modifier
        dragDirY = chosenDirY * modifier

        rotationManager.startDrag(dragAxis, dragLayer)
        mode = TouchMode.LAYER_DRAG
    }
}

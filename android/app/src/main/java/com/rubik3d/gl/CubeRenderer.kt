package com.rubik3d.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.rubik3d.model.CubeState
import com.rubik3d.model.Face
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CubeRenderer(
    val cubeState: CubeState,
    val rotationManager: RotationManager
) : GLSurfaceView.Renderer {

    private var program = 0
    private var uMVPMatrixLoc = 0
    private var uModelMatrixLoc = 0
    private var uLightPosLoc = 0
    private var uColorLoc = 0
    private var aPositionLoc = 0
    private var aNormalLoc = 0

    private lateinit var mesh: CubieMesh

    // Viewport and matrices
    val viewport = IntArray(4)
    val projMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val layerRotMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // Camera orbit parameters
    var cameraAngleX = 22f // pitch in degrees
        private set
    var cameraAngleY = -38f // yaw in degrees
        private set
    var cameraDistance = 9.2f
        private set

    // Plastic color
    private val plasticColor = floatArrayOf(0.08f, 0.08f, 0.09f, 1.0f)
    private val lightPos = floatArrayOf(5.0f, 8.0f, 10.0f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.06f, 0.07f, 0.09f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        val vShader = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uLightPos;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vLightDir;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = normalize(vec3(uModelMatrix * vec4(aNormal, 0.0)));
                vec3 worldPos = vec3(uModelMatrix * aPosition);
                vLightDir = normalize(uLightPos - worldPos);
            }
        """.trimIndent()

        val fShader = """
            precision mediump float;
            uniform vec4 uColor;
            varying vec3 vNormal;
            varying vec3 vLightDir;
            void main() {
                float diff = max(dot(vNormal, vLightDir), 0.0);
                vec3 ambient = vec3(0.40, 0.40, 0.44);
                vec3 diffuse = diff * vec3(0.70, 0.70, 0.70);
                vec3 finalColor = (ambient + diffuse) * uColor.rgb;
                gl_FragColor = vec4(finalColor, uColor.a);
            }
        """.trimIndent()

        program = ShaderUtil.createProgram(vShader, fShader)
        uMVPMatrixLoc = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uModelMatrixLoc = GLES20.glGetUniformLocation(program, "uModelMatrix")
        uLightPosLoc = GLES20.glGetUniformLocation(program, "uLightPos")
        uColorLoc = GLES20.glGetUniformLocation(program, "uColor")
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(program, "aNormal")

        mesh = CubieMesh()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewport[0] = 0
        viewport[1] = 0
        viewport[2] = width
        viewport[3] = height

        val aspect = width.toFloat() / height.toFloat()
        // Responsive camera: expand distance or fov if narrow screen
        val fovY = if (aspect < 1.0f) 42f / aspect else 42f
        val clampedFov = fovY.coerceIn(40f, 75f)
        Matrix.perspectiveM(projMatrix, 0, clampedFov, aspect, 1.0f, 50.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        rotationManager.update()

        // Calculate View matrix from camera angles
        val eyeX = (cameraDistance * Math.sin(Math.toRadians(cameraAngleY.toDouble())) * Math.cos(Math.toRadians(cameraAngleX.toDouble()))).toFloat()
        val eyeY = (cameraDistance * Math.sin(Math.toRadians(cameraAngleX.toDouble()))).toFloat()
        val eyeZ = (cameraDistance * Math.cos(Math.toRadians(cameraAngleY.toDouble())) * Math.cos(Math.toRadians(cameraAngleX.toDouble()))).toFloat()

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniform3f(uLightPosLoc, lightPos[0], lightPos[1], lightPos[2])

        mesh.bind(aPositionLoc, aNormalLoc)

        val isRotating = rotationManager.activeMove != null || rotationManager.isDragging
        val rotAxis = if (rotationManager.activeMove != null) rotationManager.activeMove!!.axis else rotationManager.dragAxis
        val rotLayer = if (rotationManager.activeMove != null) rotationManager.activeMove!!.layer else rotationManager.dragLayer
        val rotAngle = rotationManager.currentAngle

        Matrix.setIdentityM(layerRotMatrix, 0)
        if (isRotating) {
            when (rotAxis) {
                0 -> Matrix.setRotateM(layerRotMatrix, 0, rotAngle, 1f, 0f, 0f)
                1 -> Matrix.setRotateM(layerRotMatrix, 0, rotAngle, 0f, 1f, 0f)
                2 -> Matrix.setRotateM(layerRotMatrix, 0, rotAngle, 0f, 0f, 1f)
            }
        }

        // Render each of the 26 cubies
        for (cubie in cubeState.cubies) {
            val inLayer = isRotating && when (rotAxis) {
                0 -> cubie.cx == rotLayer
                1 -> cubie.cy == rotLayer
                2 -> cubie.cz == rotLayer
                else -> false
            }

            // Model Matrix = (LayerRot if in active layer) * Translation * CubieRotation
            Matrix.setIdentityM(modelMatrix, 0)
            if (inLayer) {
                Matrix.multiplyMM(modelMatrix, 0, layerRotMatrix, 0, modelMatrix, 0)
            }
            Matrix.translateM(modelMatrix, 0, cubie.cx.toFloat(), cubie.cy.toFloat(), cubie.cz.toFloat())
            Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, cubie.rotationMatrix, 0)
            System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)

            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

            GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, modelMatrix, 0)

            // 1. Draw plastic core
            GLES20.glUniform4fv(uColorLoc, 1, plasticColor, 0)
            mesh.drawBody()

            // 2. Draw outer stickers
            for ((face, sticker) in cubie.localStickers) {
                if (sticker != Face.NONE) {
                    GLES20.glUniform4fv(uColorLoc, 1, sticker.colorRgba, 0)
                    mesh.drawSticker(face)
                }
            }
        }
    }

    fun rotateCamera(deltaX: Float, deltaY: Float) {
        cameraAngleY += deltaX * 0.45f
        cameraAngleX = (cameraAngleX - deltaY * 0.45f).coerceIn(-85f, 85f)
    }

    fun zoomCamera(factor: Float) {
        cameraDistance = (cameraDistance / factor).coerceIn(6.0f, 16.0f)
    }
}

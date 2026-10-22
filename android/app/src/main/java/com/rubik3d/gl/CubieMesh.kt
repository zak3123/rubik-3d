package com.rubik3d.gl

import android.opengl.GLES20
import com.rubik3d.model.Face
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class CubieMesh {
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    // Index offsets (in bytes) for draw calls
    val bodyIndexCount = 36
    val stickerIndexCount = 6

    val stickerOffsets = mapOf(
        Face.R to 36 * 2,
        Face.L to 42 * 2,
        Face.U to 48 * 2,
        Face.D to 54 * 2,
        Face.F to 60 * 2,
        Face.B to 66 * 2
    )

    init {
        val h = 0.485f // body half size
        val s = 0.425f // sticker half size
        val d = 0.492f // sticker offset distance

        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addQuad(
            v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray,
            nx: Float, ny: Float, nz: Float
        ) {
            val baseIndex = (vertices.size / 6).toShort()
            val quadVerts = arrayOf(v0, v1, v2, v3)
            for (v in quadVerts) {
                vertices.add(v[0]); vertices.add(v[1]); vertices.add(v[2])
                vertices.add(nx); vertices.add(ny); vertices.add(nz)
            }
            indices.add(baseIndex)
            indices.add((baseIndex + 1).toShort())
            indices.add((baseIndex + 2).toShort())
            indices.add(baseIndex)
            indices.add((baseIndex + 2).toShort())
            indices.add((baseIndex + 3).toShort())
        }

        // 1. Body faces (36 indices)
        // R (+X)
        addQuad(floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), 1f, 0f, 0f)
        // L (-X)
        addQuad(floatArrayOf(-h, -h, h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(-h, h, h), -1f, 0f, 0f)
        // U (+Y)
        addQuad(floatArrayOf(-h, h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), floatArrayOf(-h, h, -h), 0f, 1f, 0f)
        // D (-Y)
        addQuad(floatArrayOf(-h, -h, -h), floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(-h, -h, h), 0f, -1f, 0f)
        // F (+Z)
        addQuad(floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h), 0f, 0f, 1f)
        // B (-Z)
        addQuad(floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h), 0f, 0f, -1f)

        // 2. Sticker quads (6 indices each, ordered R, L, U, D, F, B)
        // R (+X)
        addQuad(floatArrayOf(d, -s, -s), floatArrayOf(d, -s, s), floatArrayOf(d, s, s), floatArrayOf(d, s, -s), 1f, 0f, 0f)
        // L (-X)
        addQuad(floatArrayOf(-d, -s, s), floatArrayOf(-d, -s, -s), floatArrayOf(-d, s, -s), floatArrayOf(-d, s, s), -1f, 0f, 0f)
        // U (+Y)
        addQuad(floatArrayOf(-s, d, s), floatArrayOf(s, d, s), floatArrayOf(s, d, -s), floatArrayOf(-s, d, -s), 0f, 1f, 0f)
        // D (-Y)
        addQuad(floatArrayOf(-s, -d, -s), floatArrayOf(s, -d, -s), floatArrayOf(s, -d, s), floatArrayOf(-s, -d, s), 0f, -1f, 0f)
        // F (+Z)
        addQuad(floatArrayOf(-s, -s, d), floatArrayOf(s, -s, d), floatArrayOf(s, s, d), floatArrayOf(-s, s, d), 0f, 0f, 1f)
        // B (-Z)
        addQuad(floatArrayOf(s, -s, -d), floatArrayOf(-s, -s, -d), floatArrayOf(-s, s, -d), floatArrayOf(s, s, -d), 0f, 0f, -1f)

        val vArray = FloatArray(vertices.size)
        for (i in vertices.indices) vArray[i] = vertices[i]

        val iArray = ShortArray(indices.size)
        for (i in indices.indices) iArray[i] = indices[i]

        vertexBuffer = ByteBuffer.allocateDirect(vArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vArray)
        vertexBuffer.position(0)

        indexBuffer = ByteBuffer.allocateDirect(iArray.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(iArray)
        indexBuffer.position(0)
    }

    fun bind(aPositionLocation: Int, aNormalLocation: Int) {
        val stride = 6 * 4 // 3 pos + 3 norm floats
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLocation)
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormalLocation)
        GLES20.glVertexAttribPointer(aNormalLocation, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
    }

    fun drawBody() {
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, bodyIndexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }

    fun drawSticker(face: Face) {
        val offset = stickerOffsets[face] ?: return
        val indexOffset = offset / 2
        indexBuffer.position(indexOffset)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, stickerIndexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
}

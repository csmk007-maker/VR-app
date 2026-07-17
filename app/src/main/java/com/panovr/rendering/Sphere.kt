package com.panovr.rendering

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class Sphere(radius: Float, rings: Int, sectors: Int) {
    private val vertexBuffer: FloatBuffer
    private val texCoordinateBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val numIndices: Int

    companion object {
        private const val POSITION_DATA_SIZE = 3
        private const val TEXTURE_COORDINATE_DATA_SIZE = 2
    }

    init {
        val PI = Math.PI.toFloat()
        val PI_2 = (Math.PI / 2).toFloat()

        val R = 1f / rings
        val S = 1f / sectors

        val numPoint = (rings + 1) * (sectors + 1)
        val vertices = FloatArray(numPoint * 3)
        val texcoords = FloatArray(numPoint * 2)
        val indices = ShortArray(rings * sectors * 6)

        var t = 0
        var v = 0

        for (r in 0..rings) {
            for (s in 0..sectors) {
                val x = (cos(2 * PI * s * S) * sin(PI * r * R)).toFloat()
                val y = sin(-PI_2 + PI * r * R).toFloat()
                val z = (sin(2 * PI * s * S) * sin(PI * r * R)).toFloat()

                texcoords[t++] = s * S
                texcoords[t++] = r * R

                vertices[v++] = x * radius
                vertices[v++] = y * radius
                vertices[v++] = z * radius
            }
        }

        var counter = 0
        val sectorsPlusOne = sectors + 1
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                indices[counter++] = (r * sectorsPlusOne + s).toShort()
                indices[counter++] = ((r + 1) * sectorsPlusOne + s).toShort()
                indices[counter++] = (r * sectorsPlusOne + s + 1).toShort()

                indices[counter++] = (r * sectorsPlusOne + s + 1).toShort()
                indices[counter++] = ((r + 1) * sectorsPlusOne + s).toShort()
                indices[counter++] = ((r + 1) * sectorsPlusOne + s + 1).toShort()
            }
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        texCoordinateBuffer = ByteBuffer.allocateDirect(texcoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texcoords)
        texCoordinateBuffer.position(0)

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
        indexBuffer.position(0)

        numIndices = indices.size
    }

    fun uploadVerticesBuffer(positionHandle: Int) {
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
    }

    fun uploadTexCoordinateBuffer(textureCoordinateHandle: Int) {
        texCoordinateBuffer.position(0)
        GLES20.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, texCoordinateBuffer)
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle)
    }

    fun draw() {
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
}

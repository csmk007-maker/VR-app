package com.panovr.rendering

import android.opengl.GLES20
import android.util.Log

object Shaders {
    private const val TAG = "Shaders"

    val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        attribute vec2 aTexCoordinate;
        varying vec2 vTexCoordinate;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTexCoordinate = aTexCoordinate;
        }
    """.trimIndent()

    val FRAGMENT_SHADER_2D = """
        precision mediump float;
        uniform sampler2D uTexture;
        varying vec2 vTexCoordinate;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoordinate);
        }
    """.trimIndent()

    val FRAGMENT_SHADER_OES = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        uniform samplerExternalOES uTexture;
        varying vec2 vTexCoordinate;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoordinate);
        }
    """.trimIndent()

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader $type:")
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0

        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) return 0

        val program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, pixelShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                return 0
            }
        }
        return program
    }
}

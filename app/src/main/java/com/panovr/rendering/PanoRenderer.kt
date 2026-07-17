package com.panovr.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.view.Surface
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PanoRenderer(private val context: Context) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private lateinit var sphere: Sphere
    private var program = 0

    private var mvpMatrixHandle = 0
    private var positionHandle = 0
    private var texCoordinateHandle = 0
    private var textureHandle = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val textures = IntArray(1)

    private var isVideo = false
    private var bitmapToLoad: Bitmap? = null
    private var videoUri: Uri? = null

    private var mediaPlayer: MediaPlayer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var updateSurface = false

    // Configurable state
    var isStereoMode = false
    var isGyroMode = false
    var currentFov = 90f

    // Interaction
    var rotationX = 0f
    var rotationY = 0f

    // Gyroscope tracking
    var gyroMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmapToLoad = bitmap
        this.isVideo = false
    }

    fun setVideo(uri: Uri) {
        this.videoUri = uri
        this.isVideo = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        sphere = Sphere(18f, 75, 150)

        if (isVideo) {
            program = Shaders.createProgram(Shaders.VERTEX_SHADER, Shaders.FRAGMENT_SHADER_OES)
        } else {
            program = Shaders.createProgram(Shaders.VERTEX_SHADER, Shaders.FRAGMENT_SHADER_2D)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordinateHandle = GLES20.glGetAttribLocation(program, "aTexCoordinate")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        GLES20.glGenTextures(1, textures, 0)
        val target = if (isVideo) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D

        GLES20.glBindTexture(target, textures[0])
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        if (isVideo) {
            surfaceTexture = SurfaceTexture(textures[0])
            surfaceTexture?.setOnFrameAvailableListener(this)
            val surface = Surface(surfaceTexture)

            mediaPlayer = MediaPlayer().apply {
                setSurface(surface)
                videoUri?.let { setDataSource(context, it) }
                prepare()
                isLooping = true
                start()
            }
            surface.release()
        }
    }

    private var width = 0
    private var height = 0

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        updateProjectionMatrix()
    }

    private fun updateProjectionMatrix() {
        if (width == 0 || height == 0) return

        if (isStereoMode) {
            val ratio = (width / 2.0f) / height.toFloat()
            Matrix.perspectiveM(projectionMatrix, 0, currentFov, ratio, 0.1f, 100.0f)
        } else {
            val ratio = width.toFloat() / height.toFloat()
            Matrix.perspectiveM(projectionMatrix, 0, currentFov, ratio, 0.1f, 100.0f)
        }
    }

    fun setFov(fov: Float) {
        currentFov = fov.coerceIn(10f, 120f)
        updateProjectionMatrix()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (isVideo) {
            synchronized(this) {
                if (updateSurface) {
                    surfaceTexture?.updateTexImage()
                    updateSurface = false
                }
            }
        } else {
            bitmapToLoad?.let { bmp ->
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

                // Handle max texture size
                val maxTextureSize = IntArray(1)
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)

                var scaledBmp = bmp
                if (bmp.width > maxTextureSize[0] || bmp.height > maxTextureSize[0]) {
                    val scale = Math.min(maxTextureSize[0].toFloat() / bmp.width, maxTextureSize[0].toFloat() / bmp.height)
                    scaledBmp = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                }

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, scaledBmp, 0)

                if (scaledBmp != bmp) {
                    scaledBmp.recycle()
                }

                bitmapToLoad = null
            }
        }

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        val target = if (isVideo) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D
        GLES20.glBindTexture(target, textures[0])
        GLES20.glUniform1i(textureHandle, 0)

        sphere.uploadVerticesBuffer(positionHandle)
        sphere.uploadTexCoordinateBuffer(texCoordinateHandle)

        if (isStereoMode) {
            drawEye(isLeft = true)
            drawEye(isLeft = false)
        } else {
            GLES20.glViewport(0, 0, width, height)
            drawScene()
        }
    }

    private fun drawEye(isLeft: Boolean) {
        if (isLeft) {
            GLES20.glViewport(0, 0, width / 2, height)
        } else {
            GLES20.glViewport(width / 2, 0, width / 2, height)
        }

        val eyeOffset = if (isLeft) -0.03f else 0.03f

        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.translateM(viewMatrix, 0, eyeOffset, 0f, 0f)

        drawSceneWithView(viewMatrix)
    }

    private fun drawScene() {
        Matrix.setIdentityM(viewMatrix, 0)
        drawSceneWithView(viewMatrix)
    }

    private fun drawSceneWithView(vMatrix: FloatArray) {
        Matrix.setIdentityM(modelMatrix, 0)

        if (isGyroMode) {
            val tempMatrix = FloatArray(16)
            Matrix.multiplyMM(tempMatrix, 0, vMatrix, 0, gyroMatrix, 0)
            System.arraycopy(tempMatrix, 0, vMatrix, 0, 16)
        } else {
            Matrix.rotateM(modelMatrix, 0, rotationY, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, rotationX, 0f, 1f, 0f)
        }

        Matrix.multiplyMM(mvpMatrix, 0, vMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        sphere.draw()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            updateSurface = true
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        surfaceTexture?.release()
        surfaceTexture = null
    }
}

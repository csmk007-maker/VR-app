package com.panovr.viewer

import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.panovr.rendering.PanoRenderer
import com.panovr.sensor.GyroscopeSensorManager
import kotlin.math.max
import kotlin.math.min

@Composable
fun ViewerScreen(
    bitmap: Bitmap?,
    videoUri: Uri?,
    mode: Int, // 0 = 360, 1 = Handheld, 2 = Cardboard
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
    var renderer by remember { mutableStateOf<PanoRenderer?>(null) }

    var currentFov by remember { mutableFloatStateOf(90f) }

    val sensorManager = remember { GyroscopeSensorManager(context) }

    BackHandler {
        onBack()
    }

    DisposableEffect(Unit) {
        if (mode == 1 || mode == 2) {
            sensorManager.onGyroChanged = { matrix ->
                renderer?.gyroMatrix = matrix
            }
            sensorManager.start()
        }

        onDispose {
            if (mode == 1 || mode == 2) {
                sensorManager.stop()
            }
            renderer?.release()
            glSurfaceView?.onPause()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    renderer?.let { r ->
                        if (mode == 0) { // 360 mode: allow touch rotation
                            r.rotationY += pan.x * 0.1f
                            r.rotationX += pan.y * 0.1f
                            // Clamp rotationX to avoid flipping upside down
                            r.rotationX = max(-90f, min(90f, r.rotationX))
                        }

                        // All modes: allow pinch to zoom
                        if (zoom != 1f) {
                            currentFov = max(20f, min(120f, currentFov / zoom))
                            r.setFov(currentFov)
                        }
                    }
                }
            },
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                val panoRenderer = PanoRenderer(ctx).apply {
                    if (bitmap != null) {
                        setBitmap(bitmap)
                    } else if (videoUri != null) {
                        setVideo(videoUri)
                    }
                    isGyroMode = (mode == 1 || mode == 2)
                    isStereoMode = (mode == 2)
                }
                setRenderer(panoRenderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                renderer = panoRenderer
                glSurfaceView = this
            }
        },
        update = { _ ->
            // Update logic here if needed
        }
    )
}

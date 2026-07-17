package com.panovr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.panovr.loader.MediaLoader
import com.panovr.ui.HomeScreen
import com.panovr.viewer.ViewerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var selectedMode by mutableIntStateOf(-1)
    private var selectedBitmap by mutableStateOf<Bitmap?>(null)
    private var selectedVideoUri by mutableStateOf<Uri?>(null)
    private var showErrorDialog by mutableStateOf(false)

    private val mediaLoader by lazy { MediaLoader(this) }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val mimeType = contentResolver.getType(uri)
                if (mimeType?.startsWith("image/") == true) {
                    setContentWithLoader {
                        val bitmap = mediaLoader.loadImage(uri)
                        if (bitmap != null) {
                            if (bitmap.width >= bitmap.height * 1.8 && bitmap.width <= bitmap.height * 2.2) {
                                selectedBitmap = bitmap
                                selectedVideoUri = null
                                setImmersiveMode(true)
                            } else {
                                showErrorDialog = true
                                selectedMode = -1
                            }
                        } else {
                            showErrorDialog = true
                            selectedMode = -1
                        }
                    }
                } else if (mimeType?.startsWith("video/") == true) {
                    selectedVideoUri = uri
                    selectedBitmap = null
                    setImmersiveMode(true)
                } else {
                    showErrorDialog = true
                    selectedMode = -1
                }
            } else {
                selectedMode = -1
            }
        } else {
            selectedMode = -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContent {
            val darkTheme = true
            val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showErrorDialog) {
                        AlertDialog(
                            onDismissRequest = { showErrorDialog = false },
                            title = { Text("Unsupported Format") },
                            text = { Text("Only valid equirectangular 360° panorama images and videos are currently supported.") },
                            confirmButton = {
                                TextButton(onClick = { showErrorDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    if (selectedMode == -1 || (selectedBitmap == null && selectedVideoUri == null)) {
                        setImmersiveMode(false)
                        HomeScreen { mode ->
                            selectedMode = mode
                            launchFilePicker()
                        }
                    } else {
                        ViewerScreen(
                            bitmap = selectedBitmap,
                            videoUri = selectedVideoUri,
                            mode = selectedMode,
                            onBack = {
                                selectedMode = -1
                                selectedBitmap = null
                                selectedVideoUri = null
                                setImmersiveMode(false)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setContentWithLoader(action: suspend () -> Unit) {
        lifecycleScope.launch {
            action()
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp", "video/mp4"))
        }
        filePickerLauncher.launch(intent)
    }

    private fun setImmersiveMode(enable: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (selectedMode != -1) {
            selectedMode = -1
            selectedBitmap = null
            selectedVideoUri = null
            setImmersiveMode(false)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}

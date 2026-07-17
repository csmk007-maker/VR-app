package com.panovr.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaLoader(private val context: Context) {

    suspend fun loadImage(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

package com.helal.accomplishmentplan.webview.webview

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUploadHelper {

    fun parseAcceptMimeTypes(acceptTypes: Array<String>?): Array<String> {
        if (acceptTypes.isNullOrEmpty()) return arrayOf("*/*")
        val mimeSet = linkedSetOf<String>()
        for (raw in acceptTypes) {
            raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { token ->
                    when {
                        token.contains('/') -> mimeSet.add(token)
                        token.startsWith(".") -> {
                            val ext = token.removePrefix(".").lowercase()
                            val mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                            if (!mapped.isNullOrBlank()) {
                                mimeSet.add(mapped)
                            }
                        }
                    }
                }
        }
        return if (mimeSet.isEmpty()) arrayOf("*/*") else mimeSet.toTypedArray()
    }

    fun shouldOfferCamera(acceptTypes: Array<String>?, isCaptureEnabled: Boolean): Boolean {
        if (isCaptureEnabled) return true
        if (acceptTypes.isNullOrEmpty()) return true
        val joined = acceptTypes.joinToString(",").lowercase()
        if (joined.isBlank() || joined.contains("*/*")) return true
        return joined.contains("image") ||
            joined.contains(".jpg") ||
            joined.contains(".jpeg") ||
            joined.contains(".png") ||
            joined.contains(".webp")
    }

    fun createCameraPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_uploads")
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(dir, "capture_$timestamp.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
}

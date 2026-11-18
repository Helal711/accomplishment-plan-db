package com.helal.accomplishmentplan.webview.webview

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import java.io.File

data class SavedFileResult(
    val fileName: String,
    val mimeType: String,
    val uri: Uri
)

object DownloadUtils {
    private val EXTENSION_MIME_MAP = mapOf(
        "pdf" to "application/pdf",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "xls" to "application/vnd.ms-excel",
        "csv" to "text/csv",
        "txt" to "text/plain"
    )

    fun sanitizeFilename(rawName: String?, fallbackMimeType: String?): String {
        val trimmed = rawName
            ?.trim()
            ?.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+"), "_")
            ?.trim('.')
            .orEmpty()

        val ext = extensionForMimeType(fallbackMimeType)
        if (trimmed.isEmpty()) {
            return "accomplishment_plan_export.${ext ?: "bin"}"
        }
        val hasExtension = trimmed.contains('.') && trimmed.substringAfterLast('.').length in 1..6
        return if (hasExtension || ext == null) trimmed else "$trimmed.$ext"
    }

    fun inferMimeType(fileName: String, declaredMimeType: String?): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mapped = EXTENSION_MIME_MAP[ext]
        if (mapped != null) return mapped

        val cleanedDeclared = declaredMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() && it != "application/octet-stream" }
        if (cleanedDeclared != null) return cleanedDeclared

        val systemMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        return systemMime ?: "application/octet-stream"
    }

    fun extensionForMimeType(mimeType: String?): String? {
        val clean = mimeType?.substringBefore(';')?.trim()?.lowercase() ?: return null
        return when (clean) {
            "application/pdf" -> "pdf"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            "application/vnd.ms-excel" -> "xls"
            "text/csv" -> "csv"
            "text/plain" -> "txt"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(clean)
        }
    }

    fun decodeDataUrl(dataUrl: String): Pair<String?, ByteArray>? {
        if (!dataUrl.startsWith("data:", ignoreCase = true)) return null
        val commaIdx = dataUrl.indexOf(',')
        if (commaIdx <= 5) return null
        val header = dataUrl.substring(5, commaIdx)
        val payload = dataUrl.substring(commaIdx + 1)
        val isBase64 = header.split(';').any { it.equals("base64", ignoreCase = true) }
        val mime = header.substringBefore(';').trim().takeIf { it.isNotEmpty() }

        val bytes = if (isBase64) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            Uri.decode(payload).toByteArray(Charsets.UTF_8)
        }
        return mime to bytes
    }

    fun buildOpenFileIntent(result: SavedFileResult): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(result.uri, result.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

class DownloadBridge(
    private val context: Context,
    private val onSaved: (SavedFileResult) -> Unit,
    private val onError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun saveDataUrl(dataUrl: String, suggestedFilename: String, fallbackMimeType: String) {
        try {
            val decoded = DownloadUtils.decodeDataUrl(dataUrl)
                ?: throw IllegalArgumentException("Unsupported data payload")
            val (dataMime, bytes) = decoded
            val resolvedMimeHint = dataMime ?: fallbackMimeType
            val safeName = DownloadUtils.sanitizeFilename(suggestedFilename, resolvedMimeHint)
            val finalMime = DownloadUtils.inferMimeType(safeName, resolvedMimeHint)

            val saved = saveBytesToDownloads(bytes, safeName, finalMime)
            mainHandler.post { onSaved(saved) }
        } catch (t: Throwable) {
            val fallbackName = suggestedFilename.ifBlank { "export_file" }
            mainHandler.post { onError(fallbackName) }
        }
    }

    fun handleStandardDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ) {
        if (url.startsWith("data:", ignoreCase = true)) {
            val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            saveDataUrl(url, guessedName, mimetype.orEmpty())
            return
        }

        if (!url.startsWith("https://", ignoreCase = true)) {
            mainHandler.post { onError(URLUtil.guessFileName(url, contentDisposition, mimetype)) }
            return
        }

        try {
            val fileName = DownloadUtils.sanitizeFilename(
                URLUtil.guessFileName(url, contentDisposition, mimetype),
                mimetype
            )
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(DownloadUtils.inferMimeType(fileName, mimetype))
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }
                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                setTitle(fileName)
                setDescription("Downloading from Accomplishment Plan DB")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        } catch (t: Throwable) {
            mainHandler.post { onError(URLUtil.guessFileName(url, contentDisposition, mimetype)) }
        }
    }

    private fun saveBytesToDownloads(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): SavedFileResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: throw IllegalStateException("Cannot open output stream")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
                return SavedFileResult(fileName = fileName, mimeType = mimeType, uri = uri)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.cacheDir, "downloads")
        if (!dir.exists()) dir.mkdirs()

        val target = resolveUniqueFile(dir, fileName)
        target.writeBytes(bytes)
        val providerUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            target
        )
        return SavedFileResult(fileName = target.name, mimeType = mimeType, uri = providerUri)
    }

    private fun resolveUniqueFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val base = desiredName.substringBeforeLast('.')
        val ext = desiredName.substringAfterLast('.', "")
        var idx = 1
        while (candidate.exists()) {
            val nextName = if (ext.isNotEmpty() && base != desiredName) {
                "$base ($idx).$ext"
            } else {
                "$desiredName ($idx)"
            }
            candidate = File(dir, nextName)
            idx++
        }
        return candidate
    }
}

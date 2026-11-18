package com.helal.accomplishmentplan.webview

import android.util.Base64
import com.helal.accomplishmentplan.webview.webview.DownloadUtils
import com.helal.accomplishmentplan.webview.webview.FileUploadHelper
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DownloadUtilsTest {

    @Test
    fun `sanitizeFilename preserves valid extensions and strips unsafe chars`() {
        assertEquals(
            "Accomplishment_Plan_Nov-2026.pdf",
            DownloadUtils.sanitizeFilename("Accomplishment_Plan_Nov-2026.pdf", "application/pdf")
        )
        assertEquals(
            "Tasks_Nov_2026.xlsx",
            DownloadUtils.sanitizeFilename("Tasks/Nov:2026", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        )
        assertEquals(
            "report.csv",
            DownloadUtils.sanitizeFilename("report", "text/csv")
        )
    }

    @Test
    fun `inferMimeType resolves pdf excel csv and txt accurately`() {
        assertEquals("application/pdf", DownloadUtils.inferMimeType("report.pdf", null))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            DownloadUtils.inferMimeType("tasks.xlsx", "application/octet-stream")
        )
        assertEquals("text/csv", DownloadUtils.inferMimeType("tasks.csv", null))
        assertEquals("text/plain", DownloadUtils.inferMimeType("summary.txt", null))
    }

    @Test
    fun `decodeDataUrl parses base64 pdf payload`() {
        val sample = "%PDF-1.4\nhello".toByteArray(Charsets.UTF_8)
        val encoded = Base64.encodeToString(sample, Base64.NO_WRAP)
        val decoded = DownloadUtils.decodeDataUrl("data:application/pdf;base64,$encoded")
        assertNotNull(decoded)
        assertEquals("application/pdf", decoded?.first)
        assertArrayEquals(sample, decoded?.second)
    }

    @Test
    fun `file upload helper parses extensions and image capture visibility`() {
        val mimes = FileUploadHelper.parseAcceptMimeTypes(arrayOf(".pdf", "image/*"))
        assertTrue(mimes.contains("application/pdf"))
        assertTrue(mimes.contains("image/*"))
        assertTrue(FileUploadHelper.shouldOfferCamera(arrayOf("image/*"), isCaptureEnabled = false))
    }
}

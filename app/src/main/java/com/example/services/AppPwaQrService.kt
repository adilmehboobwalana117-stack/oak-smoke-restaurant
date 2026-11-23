package com.example.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.FileOutputStream

enum class AppDownloadTarget(val label: String, val url: String, val description: String) {
    WEB_PWA(
        label = "Web PWA",
        url = AppPwaQrService.PWA_APP_URL,
        description = "Open in Chrome or Safari & tap 'Install App' / 'Add to Home Screen'."
    ),
    PLAY_TESTER(
        label = "Tester Opt-In",
        url = AppPwaQrService.PLAY_TESTER_URL,
        description = "Google Play Internal/Closed Testing opt-in link. Testers must accept here first before downloading."
    ),
    ANDROID_STORE(
        label = "Play Store",
        url = AppPwaQrService.ANDROID_PLAY_URL,
        description = "Direct Google Play Store listing for Oak & Smoke Restaurant Hasilpur."
    )
}

object AppPwaQrService {

    const val PWA_APP_URL: String =
        "https://ais-pre-rhal5qnhshjimdwqb2v4io-756142993760.asia-southeast1.run.app"

    val PLAY_TESTER_URL: String =
        "https://play.google.com/apps/testing/${BuildConfig.APPLICATION_ID}"

    val ANDROID_PLAY_URL: String =
        "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

    private const val PREFS_NAME = "oak_smoke_pwa_prefs"
    private const val KEY_PWA_ACTION_TAKEN = "pwa_action_taken"

    /**
     * Verifies that this Android app package is installed on the device.
     * Ensures the top QR code & PWA strip renders in the post-installation runtime.
     */
    fun isAppInstalled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasPinnedOrDownloadedPwa(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PWA_ACTION_TAKEN, false)
    }

    fun markPwaActionTaken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PWA_ACTION_TAKEN, true)
            .apply()
    }

    /**
     * Generates a genuine ISO/IEC 18004 QR Code matrix using ZXing Core.
     */
    fun encodeQrMatrix(content: String): BitMatrix? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders a crisp, high-contrast QR Code Bitmap suitable for PDF/poster printing.
     */
    fun generateQrBitmap(content: String, pixelSize: Int = 512): Bitmap? {
        val matrix = encodeQrMatrix(content) ?: return null
        val matrixW = matrix.width
        val matrixH = matrix.height
        val scale = (pixelSize / matrixW.coerceAtLeast(1)).coerceAtLeast(4)
        val bmpW = matrixW * scale
        val bmpH = matrixH * scale
        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val blackPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        for (y in 0 until matrixH) {
            for (x in 0 until matrixW) {
                if (matrix.get(x, y)) {
                    canvas.drawRect(
                        (x * scale).toFloat(),
                        (y * scale).toFloat(),
                        ((x + 1) * scale).toFloat(),
                        ((y + 1) * scale).toFloat(),
                        blackPaint
                    )
                }
            }
        }
        return bitmap
    }

    /**
     * Opens the Oak & Smoke Web PWA or Android App download URL in the system browser
     * so users can install the PWA ("Add to Home Screen") or download the app.
     */
    fun openPwaDownload(context: Context, url: String = PWA_APP_URL): Result<Unit> {
        return try {
            markPwaActionTaken(context)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Requests the Android launcher to pin an instant Oak & Smoke Home Screen shortcut.
     */
    fun pinAppShortcut(context: Context): Result<Boolean> {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return Result.success(false)
            }
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val shortcut = ShortcutInfoCompat.Builder(context, "oak_smoke_quick_menu")
                .setShortLabel("Oak & Smoke")
                .setLongLabel("Oak & Smoke Restaurant Hasilpur")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(launchIntent)
                .build()
            val requested = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            if (requested) markPwaActionTaken(context)
            Result.success(requested)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareAppLink(context: Context, url: String = PWA_APP_URL): Result<Unit> {
        return try {
            val shareText = "Order fresh Karahi, Smoked BBQ & Platters from Oak & Smoke Restaurant Hasilpur! Open or install our Web PWA App here:\n$url"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Oak & Smoke Restaurant Hasilpur App")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Oak & Smoke App").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun copyLinkToClipboard(context: Context, url: String = PWA_APP_URL): Result<Unit> {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Oak & Smoke App URL", url))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prints a branded Oak & Smoke table-tent QR Code poster via Android PrintManager.
     */
    fun printTableQrPoster(
        context: Context,
        url: String = PWA_APP_URL,
        targetLabel: String = "Web PWA App"
    ): Result<Unit> {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device."))
            val qrBitmap = generateQrBitmap(url, pixelSize = 600)
                ?: return Result.failure(IllegalStateException("Unable to generate QR code."))
            printManager.print(
                "OakSmoke_QR_Poster",
                TableQrPosterPrintAdapter(qrBitmap, url, targetLabel),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class TableQrPosterPrintAdapter(
        private val qrBitmap: Bitmap,
        private val url: String,
        private val targetLabel: String
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("OakSmoke_Table_QR.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val doc = PdfDocument()
            try {
                val pageWidth = 420
                val pageHeight = 595
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
                val canvas = page.canvas

                // Warm editorial card background
                val bgPaint = Paint().apply { color = Color.rgb(252, 248, 242) }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(196, 122, 22)
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawRoundRect(18f, 18f, pageWidth - 18f, pageHeight - 18f, 16f, 16f, borderPaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(24, 19, 15)
                    textSize = 24f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(184, 114, 18)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(55, 48, 42)
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                }
                val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(100, 92, 84)
                    textSize = 8.5f
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }

                val cx = pageWidth / 2f
                canvas.drawText("OAK & SMOKE", cx, 68f, titlePaint)
                canvas.drawText("RESTAURANT HASILPUR · KARAHI & SMOKED BBQ", cx, 90f, subtitlePaint)
                canvas.drawText("SCAN QR TO DOWNLOAD & OPEN $targetLabel", cx, 116f, bodyPaint)

                val qrSize = 260
                val left = (pageWidth - qrSize) / 2
                val top = 138
                val qrFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawRoundRect(
                    (left - 12).toFloat(),
                    (top - 12).toFloat(),
                    (left + qrSize + 12).toFloat(),
                    (top + qrSize + 12).toFloat(),
                    14f,
                    14f,
                    qrFramePaint
                )
                canvas.drawBitmap(qrBitmap, null, Rect(left, top, left + qrSize, top + qrSize), null)

                canvas.drawText("Digital Menu · Cash on Delivery · Table Booking · Loyalty Points", cx, 444f, bodyPaint)
                canvas.drawText("Open in Chrome/Safari & tap 'Install App' for instant access", cx, 466f, bodyPaint)

                val trimmedUrl = if (url.length > 58) url.take(58) + "..." else url
                canvas.drawText(trimmedUrl, cx, 505f, urlPaint)
                canvas.drawText("Oak & Smoke Restaurant · Hasilpur, Pakistan", cx, 550f, subtitlePaint)

                doc.finishPage(page)

                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                FileOutputStream(destination.fileDescriptor).use { out ->
                    doc.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
            }
        }
    }
}

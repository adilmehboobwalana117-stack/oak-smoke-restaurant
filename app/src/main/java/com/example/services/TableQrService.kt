package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.DiningTable
import com.example.data.OakAndSmokeSeeds
import com.example.data.SeatingZone
import java.io.FileOutputStream

data class ScannedTablePayload(
    val tableId: String,
    val tableLabel: String
)

object TableQrService {

    /**
     * Generates a unique, scannable HTTPS URL for each of the 25 Oak & Smoke dining tables.
     * - If the customer has the Android app installed, Android deep-link opens directly into that table.
     * - Otherwise any phone browser opens the Web App straight into Customer Menu locked to that table.
     */
    fun buildTableOrderUrl(
        context: Context,
        table: DiningTable,
        configPublicUrl: String = ""
    ): String {
        val candidate = configPublicUrl.trim().ifBlank { AppPwaQrService.getCachedPublicDownloadUrl(context) }
        val baseUrl = when {
            AppPwaQrService.isValidHttpsUrl(candidate) &&
                !candidate.contains("play.google.com", ignoreCase = true) -> {
                candidate.substringBefore("?").substringBefore("#").trimEnd('/')
            }
            else -> "${AppPwaQrService.PWA_APP_URL.trimEnd('/')}/app.html"
        }
        val encodedId = Uri.encode(table.id.ifBlank { "tbl_${table.displayOrder}" })
        val encodedLabel = Uri.encode(table.label)
        return "$baseUrl?table=$encodedId&label=$encodedLabel#customer"
    }

    /**
     * Parses a scanned deep-link URI (https://...?table=tbl_7&label=Table%207 or oaksmoke://table?...)
     * into a matched Oak & Smoke dining table.
     */
    fun parseTableFromUri(
        uri: Uri?,
        knownTables: List<DiningTable> = OakAndSmokeSeeds.starterTables
    ): ScannedTablePayload? {
        if (uri == null) return null
        val rawTableId = uri.getQueryParameter("table")?.trim().orEmpty()
        val rawLabel = uri.getQueryParameter("label")?.trim().orEmpty()
        if (rawTableId.isBlank() && rawLabel.isBlank()) return null

        val allKnown = if (knownTables.isNotEmpty()) knownTables else OakAndSmokeSeeds.starterTables
        val matched = allKnown.firstOrNull { tbl ->
            (rawTableId.isNotBlank() && tbl.id.equals(rawTableId, ignoreCase = true)) ||
                (rawLabel.isNotBlank() && tbl.label.equals(rawLabel, ignoreCase = true))
        }
        return when {
            matched != null -> ScannedTablePayload(matched.id, matched.label)
            rawLabel.length in 2..40 -> ScannedTablePayload(rawTableId.ifBlank { "custom_table" }, rawLabel)
            rawTableId.isNotBlank() -> {
                val number = rawTableId.filter { it.isDigit() }
                val fallbackLabel = if (number.isNotEmpty()) "Table $number" else rawTableId.take(40)
                ScannedTablePayload(rawTableId, fallbackLabel)
            }
            else -> null
        }
    }

    /**
     * Parses either a pasted Table QR URL or a typed table code/number (e.g., "7", "Table 7", "tbl_7").
     */
    fun parseTableFromRawInput(
        rawInput: String,
        knownTables: List<DiningTable> = OakAndSmokeSeeds.starterTables
    ): ScannedTablePayload? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("oaksmoke://", ignoreCase = true)
        ) {
            parseTableFromUri(Uri.parse(trimmed), knownTables)?.let { return it }
        }
        val allKnown = if (knownTables.isNotEmpty()) knownTables else OakAndSmokeSeeds.starterTables
        val directMatch = allKnown.firstOrNull {
            it.label.equals(trimmed, ignoreCase = true) ||
                it.id.equals(trimmed, ignoreCase = true) ||
                it.label.equals("Table $trimmed", ignoreCase = true)
        }
        if (directMatch != null) {
            return ScannedTablePayload(directMatch.id, directMatch.label)
        }
        val num = trimmed.toIntOrNull()
        if (num != null && num in 1..allKnown.size) {
            val byIndex = allKnown.firstOrNull { it.displayOrder.toInt() == num }
                ?: allKnown.getOrNull(num - 1)
            if (byIndex != null) return ScannedTablePayload(byIndex.id, byIndex.label)
        }
        return null
    }

    fun shareTableQrLink(context: Context, table: DiningTable, qrUrl: String): Result<Unit> {
        return try {
            val zoneLabel = SeatingZone.fromRaw(table.zone).displayLabel
            val shareText = "Scan or open this link to order directly from ${table.label} ($zoneLabel) at Oak & Smoke Restaurant Hasilpur:\n$qrUrl"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Oak & Smoke · ${table.label} QR Order Link")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share ${table.label} QR Link").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prints an individual branded A5 Table Tent QR Poster for one specific dining table.
     */
    fun printSingleTableQrPoster(
        context: Context,
        table: DiningTable,
        qrUrl: String
    ): Result<Unit> {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device."))
            val qrBitmap = AppPwaQrService.generateQrBitmap(qrUrl, pixelSize = 600)
                ?: return Result.failure(IllegalStateException("Unable to generate QR code for ${table.label}."))
            printManager.print(
                "OakSmoke_${table.label.replace(" ", "_")}_QR",
                SingleTableQrPrintAdapter(table, qrBitmap, qrUrl),
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

    /**
     * Prints a multi-page A4 sheet with 4 table tent QR cards per page (all 25 tables in one print job).
     */
    fun printAllTableQrCardsPdf(
        context: Context,
        tables: List<DiningTable>,
        configPublicUrl: String = ""
    ): Result<Unit> {
        if (tables.isEmpty()) return Result.failure(IllegalArgumentException("No tables available to print."))
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device."))
            val cards = tables.mapNotNull { tbl ->
                val url = buildTableOrderUrl(context, tbl, configPublicUrl)
                val bmp = AppPwaQrService.generateQrBitmap(url, pixelSize = 420) ?: return@mapNotNull null
                TableQrPrintCard(tbl, bmp, url)
            }
            if (cards.isEmpty()) {
                return Result.failure(IllegalStateException("Unable to generate table QR codes."))
            }
            printManager.print(
                "OakSmoke_All_${cards.size}_Table_QRs",
                BatchTableQrPrintAdapter(cards),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class TableQrPrintCard(
        val table: DiningTable,
        val qrBitmap: Bitmap,
        val url: String
    )

    private class SingleTableQrPrintAdapter(
        private val table: DiningTable,
        private val qrBitmap: Bitmap,
        private val url: String
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
            val info = PrintDocumentInfo.Builder("OakSmoke_${table.label.replace(" ", "_")}_QR.pdf")
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

                val bgPaint = Paint().apply { color = Color.rgb(252, 248, 242) }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(196, 122, 22)
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawRoundRect(16f, 16f, pageWidth - 16f, pageHeight - 16f, 16f, 16f, borderPaint)

                val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(24, 19, 15) }
                canvas.drawRoundRect(56f, 88f, pageWidth - 56f, 140f, 14f, 14f, badgeBg)

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(24, 19, 15)
                    textSize = 22f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(184, 114, 18)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(230, 148, 69)
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(45, 38, 32)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(92, 84, 76)
                    textSize = 10.5f
                    textAlign = Paint.Align.CENTER
                }
                val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(110, 102, 94)
                    textSize = 8f
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }

                val cx = pageWidth / 2f
                val zoneLabel = SeatingZone.fromRaw(table.zone).displayLabel
                canvas.drawText("OAK & SMOKE", cx, 54f, brandPaint)
                canvas.drawText("RESTAURANT HASILPUR · DIRECT TABLE ORDERING", cx, 74f, subPaint)
                canvas.drawText(table.label.uppercase(), cx, 122f, tablePaint)
                canvas.drawText("$zoneLabel · ${table.capacity} Guests", cx, 160f, detailPaint)

                val qrSize = 246
                val left = (pageWidth - qrSize) / 2
                val top = 178
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

                canvas.drawText("SCAN TO ORDER DIRECTLY FROM THIS TABLE", cx, 462f, bodyPaint)
                canvas.drawText("No waiting · Pick from 4 varieties · Sent live to kitchen", cx, 484f, detailPaint)
                canvas.drawText(if (url.length > 56) url.take(56) + "..." else url, cx, 520f, urlPaint)
                canvas.drawText("Oak & Smoke Restaurant · Hasilpur, Pakistan", cx, 556f, subPaint)

                doc.finishPage(page)
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                FileOutputStream(destination.fileDescriptor).use { out -> doc.writeTo(out) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
            }
        }
    }

    private class BatchTableQrPrintAdapter(
        private val cards: List<TableQrPrintCard>
    ) : PrintDocumentAdapter() {

        private val cardsPerPage = 4
        private val pageCount: Int get() = ((cards.size + cardsPerPage - 1) / cardsPerPage).coerceAtLeast(1)

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
            val info = PrintDocumentInfo.Builder("OakSmoke_25_Table_QR_Cards.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
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
                val pageWidth = 595
                val pageHeight = 842
                val margin = 24f
                val gap = 16f
                val cardW = (pageWidth - (margin * 2) - gap) / 2f
                val cardH = (pageHeight - (margin * 2) - gap) / 2f

                val bgPaint = Paint().apply { color = Color.rgb(252, 248, 242) }
                val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(196, 122, 22)
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                val cutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(190, 180, 170)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
                }
                val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(24, 19, 15) }
                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(24, 19, 15)
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(230, 148, 69)
                    textSize = 16f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(110, 95, 80)
                    textSize = 9.5f
                    textAlign = Paint.Align.CENTER
                }
                val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(34, 28, 24)
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val whitePaint = Paint().apply { color = Color.WHITE }

                for (pageIndex in 0 until pageCount) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    val page = doc.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                    )
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    // Subtle center cut lines
                    canvas.drawLine(pageWidth / 2f, 12f, pageWidth / 2f, pageHeight - 12f, cutLinePaint)
                    canvas.drawLine(12f, pageHeight / 2f, pageWidth - 12f, pageHeight / 2f, cutLinePaint)

                    val start = pageIndex * cardsPerPage
                    val slice = cards.subList(start, minOf(start + cardsPerPage, cards.size))
                    slice.forEachIndexed { idx, item ->
                        val col = idx % 2
                        val row = idx / 2
                        val left = margin + col * (cardW + gap)
                        val top = margin + row * (cardH + gap)
                        val right = left + cardW
                        val bottom = top + cardH
                        val cx = (left + right) / 2f

                        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, bgPaint)
                        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, cardBorder)

                        canvas.drawText("OAK & SMOKE HASILPUR", cx, top + 28f, brandPaint)
                        canvas.drawRoundRect(left + 28f, top + 38f, right - 28f, top + 72f, 8f, 8f, badgeBg)
                        canvas.drawText(item.table.label.uppercase(), cx, top + 61f, tablePaint)
                        val zoneLabel = SeatingZone.fromRaw(item.table.zone).displayLabel
                        canvas.drawText("$zoneLabel · ${item.table.capacity} Seats", cx, top + 88f, zonePaint)

                        val qrSize = 200
                        val qrLeft = (cx - qrSize / 2f).toInt()
                        val qrTop = (top + 102f).toInt()
                        canvas.drawRoundRect(
                            (qrLeft - 8).toFloat(),
                            (qrTop - 8).toFloat(),
                            (qrLeft + qrSize + 8).toFloat(),
                            (qrTop + qrSize + 8).toFloat(),
                            10f,
                            10f,
                            whitePaint
                        )
                        canvas.drawBitmap(
                            item.qrBitmap,
                            null,
                            Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
                            null
                        )

                        canvas.drawText("SCAN TO ORDER FROM THIS TABLE", cx, bottom - 34f, ctaPaint)
                        canvas.drawText("Karahi · Smoked BBQ · Platters · Direct to Kitchen", cx, bottom - 16f, zonePaint)
                    }

                    doc.finishPage(page)
                }

                FileOutputStream(destination.fileDescriptor).use { out -> doc.writeTo(out) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
            }
        }
    }
}

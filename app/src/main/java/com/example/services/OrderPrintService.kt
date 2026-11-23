package com.example.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.data.parsedLineItems
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OrderPrintService {

    /**
     * Prints a Kitchen Order Ticket (KOT) containing STRICTLY preparation/order details ONLY.
     * Deliberately excludes all prices, subtotals, discounts, totals, and customer billing info.
     */
    fun printKitchenTicket(context: Context, order: OrderRecord): Result<Unit> {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device."))
            val jobName = "OakSmoke_KOT_${order.orderNumber}"
            printManager.print(
                jobName,
                KitchenTicketPrintAdapter(order),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .build()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prints a complete Customer Bill / Invoice containing ALL order, customer, staff,
     * itemized pricing, loyalty discount, payment method, and grand total details.
     */
    fun printCustomerBill(context: Context, order: OrderRecord): Result<Unit> {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device."))
            val jobName = "OakSmoke_Bill_${order.orderNumber}"
            printManager.print(
                jobName,
                CustomerBillPrintAdapter(order),
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

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    private class KitchenTicketPrintAdapter(
        private val order: OrderRecord
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
            val info = PrintDocumentInfo.Builder("KOT_${order.orderNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
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
                val margin = 28f
                val maxTextWidth = pageWidth - (margin * 2)

                val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val orderNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 22f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.DKGRAY
                    textSize = 11f
                }
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    strokeWidth = 1.4f
                }
                val thinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GRAY
                    strokeWidth = 0.8f
                }

                var pageNumber = 1
                var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                var canvas: Canvas = page.canvas
                var y = 38f

                fun ensureSpace(needed: Float) {
                    if (y + needed > pageHeight - 40f) {
                        doc.finishPage(page)
                        pageNumber++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = page.canvas
                        y = 38f
                        canvas.drawText("KOT ${order.orderNumber} (Continued - Page $pageNumber)", margin, y, sectionPaint)
                        y += 16f
                        canvas.drawLine(margin, y, pageWidth - margin, y, thinLinePaint)
                        y += 16f
                    }
                }

                // Header
                canvas.drawText("KITCHEN ORDER TICKET (KOT)", margin, y, headerPaint)
                y += 16f
                canvas.drawText("Oak & Smoke Restaurant Hasilpur — Kitchen Copy", margin, y, bodyPaint)
                y += 12f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 24f

                // Order # and Type / Table ONLY (no prices, no customer billing info)
                val orderType = OrderType.fromRaw(order.orderType)
                val tableInfo = if (order.tableLabel.isNotBlank()) "  |  ${order.tableLabel}" else ""
                canvas.drawText(order.orderNumber, margin, y, orderNumPaint)
                y += 18f
                canvas.drawText("TYPE: ${orderType.displayLabel.uppercase(Locale.US)}$tableInfo", margin, y, sectionPaint)
                y += 15f
                val timeStr = formatTimestampShort(order.createdAt)
                val staffInfo = if (order.handledByStaffId.isNotBlank()) "  |  Staff: ${order.handledByStaffId}" else ""
                canvas.drawText("Time: $timeStr$staffInfo  |  Total Items: ${order.itemCount}", margin, y, bodyPaint)
                y += 12f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 20f

                // Table Headers (QTY & DISH ONLY)
                canvas.drawText("QTY", margin, y, sectionPaint)
                canvas.drawText("DISH / PREPARATION ITEM", margin + 52f, y, sectionPaint)
                y += 8f
                canvas.drawLine(margin, y, pageWidth - margin, y, thinLinePaint)
                y += 18f

                val items = order.parsedLineItems()
                items.forEach { item ->
                    val nameLines = wrapText(item.name, itemPaint, maxTextWidth - 56f)
                    ensureSpace(nameLines.size * 18f + 12f)
                    canvas.drawText("${item.qty}x", margin, y, itemPaint)
                    nameLines.forEachIndexed { idx, line ->
                        canvas.drawText(line, margin + 52f, y + (idx * 17f), itemPaint)
                    }
                    y += (nameLines.size * 17f) + 8f
                    canvas.drawLine(margin, y - 4f, pageWidth - margin, y - 4f, thinLinePaint)
                    y += 8f
                }

                // Kitchen Notes
                ensureSpace(70f)
                y += 6f
                canvas.drawText("KITCHEN NOTES / SPECIAL INSTRUCTIONS:", margin, y, sectionPaint)
                y += 16f
                val noteText = order.notes.ifBlank { "Standard spice & preparation." }
                wrapText(noteText, bodyPaint, maxTextWidth).forEach { line ->
                    ensureSpace(16f)
                    canvas.drawText(line, margin, y, bodyPaint)
                    y += 14f
                }

                y += 14f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 16f
                canvas.drawText("FOR KITCHEN PREPARATION ONLY — NO BILLING OR PRICE DETAILS", margin, y, bodyPaint)

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

    private class CustomerBillPrintAdapter(
        private val order: OrderRecord
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
            val info = PrintDocumentInfo.Builder("Bill_${order.orderNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
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
                val margin = 42f
                val contentWidth = pageWidth - (margin * 2)

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 22, 16)
                    textSize = 22f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(184, 114, 18)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 22, 16)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(45, 45, 45)
                    textSize = 11f
                }
                val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(25, 25, 25)
                    textSize = 11f
                    typeface = Typeface.MONOSPACE
                }
                val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(20, 20, 20)
                    textSize = 16f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GRAY
                    textSize = 9.5f
                }
                val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(248, 244, 238)
                    style = Paint.Style.FILL
                }
                val totalFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(252, 240, 215)
                    style = Paint.Style.FILL
                }
                val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(210, 198, 182)
                    strokeWidth = 1f
                }
                val boldDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(184, 114, 18)
                    strokeWidth = 2f
                }

                var pageNum = 1
                var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                var canvas: Canvas = page.canvas
                var y = 50f

                fun ensureSpace(needed: Float) {
                    if (y + needed > pageHeight - 54f) {
                        doc.finishPage(page)
                        pageNum++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                        canvas = page.canvas
                        y = 50f
                        canvas.drawText("Oak & Smoke Restaurant Hasilpur — Bill ${order.orderNumber} (Page $pageNum)", margin, y, sectionPaint)
                        y += 14f
                        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                        y += 18f
                    }
                }

                // Restaurant Header
                canvas.drawText("OAK & SMOKE RESTAURANT", margin, y, brandPaint)
                y += 17f
                canvas.drawText("HASILPUR  ·  AUTHENTIC PAKISTANI KARAHI, SMOKED BBQ & PLATTERS", margin, y, accentPaint)
                y += 15f
                val printedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                canvas.drawText("OFFICIAL CUSTOMER BILL / RECEIPT   |   Printed: $printedAt", margin, y, mutedPaint)
                y += 12f
                canvas.drawLine(margin, y, pageWidth - margin, y, boldDividerPaint)
                y += 18f

                // Order & Customer Details Card
                val orderType = OrderType.fromRaw(order.orderType)
                val orderStatus = OrderStatus.fromRaw(order.status)
                val paymentLabel = if (order.paymentMethod.equals("CASH_COUNTER", ignoreCase = true)) {
                    "Cash at Counter"
                } else {
                    "Cash on Delivery (COD)"
                }
                val locationLine = when {
                    order.deliveryAddress.isNotBlank() -> order.deliveryAddress
                    order.tableLabel.isNotBlank() -> "Dine-In (${order.tableLabel}), Oak & Smoke Hasilpur"
                    else -> "Oak & Smoke Restaurant Counter, Hasilpur"
                }
                val locationWrapped = wrapText(locationLine, bodyPaint, contentWidth - 20f)
                val infoBoxHeight = 112f + (locationWrapped.size * 14f)

                canvas.drawRoundRect(margin, y, pageWidth - margin, y + infoBoxHeight, 8f, 8f, boxFillPaint)
                var iy = y + 20f
                val col2X = margin + 270f

                canvas.drawText("Bill / Order #:", margin + 12f, iy, sectionPaint)
                canvas.drawText(order.orderNumber, margin + 102f, iy, monoPaint)
                canvas.drawText("Order Date:", col2X, iy, sectionPaint)
                canvas.drawText(formatTimestampShort(order.createdAt), col2X + 78f, iy, bodyPaint)
                iy += 18f

                canvas.drawText("Service Type:", margin + 12f, iy, sectionPaint)
                val serviceValue = if (order.tableLabel.isNotBlank()) "${orderType.displayLabel} (${order.tableLabel})" else orderType.displayLabel
                canvas.drawText(serviceValue, margin + 102f, iy, bodyPaint)
                canvas.drawText("Order Status:", col2X, iy, sectionPaint)
                canvas.drawText(orderStatus.displayLabel, col2X + 78f, iy, bodyPaint)
                iy += 18f

                canvas.drawText("Payment:", margin + 12f, iy, sectionPaint)
                canvas.drawText(paymentLabel, margin + 102f, iy, bodyPaint)
                canvas.drawText("Served By:", col2X, iy, sectionPaint)
                canvas.drawText(order.handledByStaffId.ifBlank { "Oak & Smoke Desk" }, col2X + 78f, iy, bodyPaint)
                iy += 20f

                canvas.drawLine(margin + 12f, iy - 6f, pageWidth - margin - 12f, iy - 6f, dividerPaint)
                iy += 10f

                canvas.drawText("Customer:", margin + 12f, iy, sectionPaint)
                canvas.drawText(order.customerName.ifBlank { "Walk-In Guest" }, margin + 102f, iy, bodyPaint)
                canvas.drawText("Phone:", col2X, iy, sectionPaint)
                canvas.drawText(order.customerPhone.ifBlank { "N/A" }, col2X + 78f, iy, bodyPaint)
                iy += 18f

                canvas.drawText("Address / Table:", margin + 12f, iy, sectionPaint)
                iy += 15f
                locationWrapped.forEach { line ->
                    canvas.drawText(line, margin + 12f, iy, bodyPaint)
                    iy += 14f
                }

                y += infoBoxHeight + 22f

                // Itemized Table Header
                val colNoX = margin
                val colItemX = margin + 26f
                val colQtyX = margin + 300f
                val colRateX = margin + 350f
                val colAmtX = margin + 432f

                canvas.drawText("#", colNoX, y, sectionPaint)
                canvas.drawText("ITEM DESCRIPTION", colItemX, y, sectionPaint)
                canvas.drawText("QTY", colQtyX, y, sectionPaint)
                canvas.drawText("RATE", colRateX, y, sectionPaint)
                canvas.drawText("AMOUNT", colAmtX, y, sectionPaint)
                y += 8f
                canvas.drawLine(margin, y, pageWidth - margin, y, boldDividerPaint)
                y += 18f

                val items = order.parsedLineItems()
                items.forEachIndexed { idx, lineItem ->
                    val nameLines = wrapText(lineItem.name, bodyPaint, 264f)
                    ensureSpace(nameLines.size * 15f + 14f)
                    canvas.drawText("${idx + 1}", colNoX, y, monoPaint)
                    nameLines.forEachIndexed { lIdx, textLine ->
                        canvas.drawText(textLine, colItemX, y + (lIdx * 14f), bodyPaint)
                    }
                    canvas.drawText("${lineItem.qty}", colQtyX, y, monoPaint)
                    val rateStr = if (lineItem.unitPricePkr > 0L) formatPkr(lineItem.unitPricePkr) else "—"
                    val amtStr = if (lineItem.lineTotalPkr > 0L) formatPkr(lineItem.lineTotalPkr) else "—"
                    canvas.drawText(rateStr, colRateX, y, monoPaint)
                    canvas.drawText(amtStr, colAmtX, y, monoPaint)
                    y += (nameLines.size * 14f) + 8f
                    canvas.drawLine(margin, y - 4f, pageWidth - margin, y - 4f, dividerPaint)
                    y += 8f
                }

                // Totals & Loyalty Summary
                ensureSpace(145f)
                y += 8f
                val labelX = margin + 240f
                val valX = margin + 410f

                canvas.drawText("Total Items:", labelX, y, bodyPaint)
                canvas.drawText("${order.itemCount}", valX, y, monoPaint)
                y += 17f

                canvas.drawText("Subtotal (PKR):", labelX, y, bodyPaint)
                canvas.drawText(formatPkr(order.subtotalPkr), valX, y, monoPaint)
                y += 17f

                canvas.drawText("Loyalty Discount (${order.loyaltyPointsRedeemed} pts):", labelX, y, bodyPaint)
                canvas.drawText("-${formatPkr(order.discountPkr)}", valX, y, monoPaint)
                y += 14f

                canvas.drawRoundRect(labelX - 10f, y, pageWidth - margin, y + 34f, 6f, 6f, totalFillPaint)
                canvas.drawText("GRAND TOTAL:", labelX, y + 22f, sectionPaint)
                canvas.drawText(formatPkr(order.totalPkr), valX - 10f, y + 22f, totalPaint)
                y += 48f

                if (order.notes.isNotBlank()) {
                    ensureSpace(46f)
                    canvas.drawText("Order Notes:", margin, y, sectionPaint)
                    y += 14f
                    wrapText(order.notes, bodyPaint, contentWidth).forEach { noteLine ->
                        ensureSpace(15f)
                        canvas.drawText(noteLine, margin, y, bodyPaint)
                        y += 14f
                    }
                    y += 10f
                }

                ensureSpace(48f)
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 18f
                canvas.drawText("Thank you for dining with Oak & Smoke Restaurant Hasilpur!", margin, y, sectionPaint)
                y += 14f
                canvas.drawText("All amounts are in Pakistani Rupees (PKR). Please keep this bill for your records.", margin, y, mutedPaint)

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

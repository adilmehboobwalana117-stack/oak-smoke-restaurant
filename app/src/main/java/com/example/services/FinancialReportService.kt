package com.example.services

import android.content.Context
import android.content.Intent
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
import androidx.core.content.FileProvider
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntry
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.StaffMember
import com.example.data.currentMonthKey
import com.example.data.formatPkr
import com.example.data.todayDateKey
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ReportPeriod(val displayLabel: String) {
    DAILY("Daily Report"),
    MONTHLY("Monthly Report")
}

data class MonthlyTrendPoint(
    val label: String,
    val salesPkr: Long,
    val expensesPkr: Long
) {
    val profitPkr: Long get() = salesPkr - expensesPkr
}

data class FinancialReportSnapshot(
    val period: ReportPeriod,
    val periodKey: String,
    val grossSalesPkr: Long,
    val totalExpensesPkr: Long,
    val netProfitPkr: Long,
    val deliveredOrderCount: Int,
    val activeOrderCount: Int,
    val averageTicketPkr: Long,
    val expensesByCategory: Map<ExpenseCategory, Long>,
    val filteredOrders: List<OrderRecord>,
    val filteredExpenses: List<ExpenseEntry>,
    val staffList: List<StaffMember>
)

object FinancialReportService {

    fun buildSnapshot(
        period: ReportPeriod,
        orders: List<OrderRecord>,
        expenses: List<ExpenseEntry>,
        staff: List<StaffMember>
    ): FinancialReportSnapshot {
        val key = if (period == ReportPeriod.DAILY) todayDateKey() else currentMonthKey()
        val matchingOrders = orders.filter {
            if (period == ReportPeriod.DAILY) it.dateKey == key else it.monthKey == key
        }
        val delivered = matchingOrders.filter { it.status == OrderStatus.DELIVERED.name }
        val active = matchingOrders.filter {
            it.status != OrderStatus.DELIVERED.name && it.status != OrderStatus.CANCELLED.name
        }
        val matchingExpenses = expenses.filter {
            if (period == ReportPeriod.DAILY) it.dateKey == key else it.monthKey == key
        }
        val grossSales = delivered.sumOf { it.totalPkr }
        val totalExpenses = matchingExpenses.sumOf { it.amountPkr }
        val netProfit = grossSales - totalExpenses
        val avgTicket = if (delivered.isNotEmpty()) grossSales / delivered.size else 0L
        val byCat = ExpenseCategory.entries.associateWith { cat ->
            matchingExpenses.filter { it.category == cat.name }.sumOf { it.amountPkr }
        }
        return FinancialReportSnapshot(
            period = period,
            periodKey = key,
            grossSalesPkr = grossSales,
            totalExpensesPkr = totalExpenses,
            netProfitPkr = netProfit,
            deliveredOrderCount = delivered.size,
            activeOrderCount = active.size,
            averageTicketPkr = avgTicket,
            expensesByCategory = byCat,
            filteredOrders = matchingOrders,
            filteredExpenses = matchingExpenses,
            staffList = staff
        )
    }

    fun buildMonthlyTrends(orders: List<OrderRecord>, expenses: List<ExpenseEntry>): List<MonthlyTrendPoint> {
        val keys = (orders.map { it.monthKey } + expenses.map { it.monthKey } + currentMonthKey())
            .filter { it.length == 7 }
            .distinct()
            .sorted()
            .takeLast(6)

        return keys.map { month ->
            val sales = orders
                .filter { it.monthKey == month && it.status == OrderStatus.DELIVERED.name }
                .sumOf { it.totalPkr }
            val exp = expenses
                .filter { it.monthKey == month }
                .sumOf { it.amountPkr }
            MonthlyTrendPoint(label = month.takeLast(2) + "/" + month.substring(2, 4), salesPkr = sales, expensesPkr = exp)
        }
    }

    fun writePdfReport(snapshot: FinancialReportSnapshot, outputStream: OutputStream) {
        val doc = createPdfDocument(snapshot)
        try {
            doc.writeTo(outputStream)
        } finally {
            doc.close()
        }
    }

    fun writeExcelWorkbook(snapshot: FinancialReportSnapshot, outputStream: OutputStream) {
        val xml = buildExcelSpreadsheetXml(snapshot)
        outputStream.write(xml.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    fun shareReportFile(context: Context, snapshot: FinancialReportSnapshot, isPdf: Boolean) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val ext = if (isPdf) "pdf" else "xls"
        val mime = if (isPdf) "application/pdf" else "application/vnd.ms-excel"
        val fileName = "OakAndSmoke_${snapshot.period.name.lowercase()}_${snapshot.periodKey}.$ext"
        val outFile = File(reportsDir, fileName)
        FileOutputStream(outFile).use { fos ->
            if (isPdf) writePdfReport(snapshot, fos) else writeExcelWorkbook(snapshot, fos)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Oak & Smoke Restaurant Hasilpur - ${snapshot.period.displayLabel} (${snapshot.periodKey})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${if (isPdf) "PDF" else "Excel"} Report"))
    }

    fun printFinancialReport(context: Context, snapshot: FinancialReportSnapshot) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Oak & Smoke ${snapshot.period.displayLabel} (${snapshot.periodKey})"
        val adapter = object : PrintDocumentAdapter() {
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
                val info = PrintDocumentInfo.Builder("oak_smoke_${snapshot.periodKey}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = createPdfDocument(snapshot)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage ?: "Print failed")
                } finally {
                    doc.close()
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun createPdfDocument(snapshot: FinancialReportSnapshot): PdfDocument {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerBg = Paint().apply { color = Color.parseColor("#1C1714") }
        val accentPaint = Paint().apply { color = Color.parseColor("#E69445") }
        val titlePaint = Paint().apply {
            color = Color.parseColor("#F6EFE6")
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#E69445")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#1C1714")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#2B2420")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val monoPaint = Paint().apply {
            color = Color.parseColor("#1C1714")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#D9CEC4")
            strokeWidth = 1f
        }

        // Branded Header
        canvas.drawRect(0f, 0f, 595f, 92f, headerBg)
        canvas.drawRect(0f, 88f, 595f, 92f, accentPaint)
        canvas.drawText("OAK & SMOKE RESTAURANT HASILPUR", 36f, 44f, titlePaint)
        canvas.drawText("${snapshot.period.displayLabel.uppercase()}  |  PERIOD: ${snapshot.periodKey}  |  CURRENCY: PKR", 36f, 68f, subtitlePaint)

        var y = 124f
        canvas.drawText("FINANCIAL SUMMARY", 36f, y, sectionPaint)
        y += 22f
        canvas.drawText("Gross COD Sales (Delivered):", 36f, y, bodyPaint)
        canvas.drawText(formatPkr(snapshot.grossSalesPkr), 320f, y, monoPaint)
        y += 18f
        canvas.drawText("Total Operating Expenses:", 36f, y, bodyPaint)
        canvas.drawText(formatPkr(snapshot.totalExpensesPkr), 320f, y, monoPaint)
        y += 18f
        canvas.drawText("Net Profit / (Loss):", 36f, y, sectionPaint)
        canvas.drawText(formatPkr(snapshot.netProfitPkr), 320f, y, monoPaint)
        y += 18f
        canvas.drawText("Delivered COD Orders / Avg Ticket:", 36f, y, bodyPaint)
        canvas.drawText("${snapshot.deliveredOrderCount} orders  (${formatPkr(snapshot.averageTicketPkr)} avg)", 320f, y, monoPaint)

        y += 18f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f

        canvas.drawText("EXPENSE BREAKDOWN BY CATEGORY", 36f, y, sectionPaint)
        y += 20f
        snapshot.expensesByCategory.forEach { (cat, total) ->
            canvas.drawText(cat.displayLabel, 36f, y, bodyPaint)
            canvas.drawText(formatPkr(total), 320f, y, monoPaint)
            y += 16f
        }

        y += 12f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f

        canvas.drawText("RECENT EXPENSES (${snapshot.filteredExpenses.size})", 36f, y, sectionPaint)
        y += 20f
        if (snapshot.filteredExpenses.isEmpty()) {
            canvas.drawText("No expenses recorded in this period.", 36f, y, bodyPaint)
            y += 18f
        } else {
            snapshot.filteredExpenses.take(10).forEach { exp ->
                if (y < 640f) {
                    canvas.drawText("${exp.dateKey}  ${exp.title.take(36)}", 36f, y, bodyPaint)
                    canvas.drawText(formatPkr(exp.amountPkr), 420f, y, monoPaint)
                    y += 16f
                }
            }
        }

        y += 14f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f
        canvas.drawText("STAFF PAYROLL & PERFORMANCE (${snapshot.staffList.size})", 36f, y, sectionPaint)
        y += 20f
        snapshot.staffList.take(8).forEach { member ->
            if (y < 795f) {
                canvas.drawText("${member.staffId} - ${member.fullName.take(24)} (${member.roleTier})", 36f, y, bodyPaint)
                canvas.drawText("${formatPkr(member.monthlySalaryPkr)} [${member.salaryStatus}] | ${member.ordersHandledCount} ord", 300f, y, monoPaint)
                y += 16f
            }
        }

        doc.finishPage(page)
        return doc
    }

    private fun escapeXml(raw: String): String =
        raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun buildExcelSpreadsheetXml(snapshot: FinancialReportSnapshot): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0"?>""")
        sb.appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        sb.appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")

        // Sheet 1: Profit & KPI Summary
        sb.appendLine("""  <Worksheet ss:Name="Summary">""")
        sb.appendLine("""    <Table>""")
        sb.appendLine(rowXml("Oak & Smoke Restaurant Hasilpur", "${snapshot.period.displayLabel} (${snapshot.periodKey})"))
        sb.appendLine(rowXml("Metric", "Amount (PKR)"))
        sb.appendLine(rowNumXml("Gross COD Sales (Delivered)", snapshot.grossSalesPkr))
        sb.appendLine(rowNumXml("Total Operating Expenses", snapshot.totalExpensesPkr))
        sb.appendLine(rowNumXml("Net Profit", snapshot.netProfitPkr))
        sb.appendLine(rowNumXml("Delivered COD Orders", snapshot.deliveredOrderCount.toLong()))
        sb.appendLine(rowNumXml("Average Order Value", snapshot.averageTicketPkr))
        sb.appendLine("""    </Table>""")
        sb.appendLine("""  </Worksheet>""")

        // Sheet 2: Expenses
        sb.appendLine("""  <Worksheet ss:Name="Expenses">""")
        sb.appendLine("""    <Table>""")
        sb.appendLine(rowXml("Date", "Title", "Category", "Amount (PKR)", "Notes"))
        snapshot.filteredExpenses.forEach { exp ->
            sb.appendLine(
                """      <Row>""" +
                    cellStr(exp.dateKey) + cellStr(exp.title) + cellStr(exp.category) +
                    cellNum(exp.amountPkr) + cellStr(exp.notes) +
                    """</Row>"""
            )
        }
        sb.appendLine("""    </Table>""")
        sb.appendLine("""  </Worksheet>""")

        // Sheet 3: Orders
        sb.appendLine("""  <Worksheet ss:Name="Orders">""")
        sb.appendLine("""    <Table>""")
        sb.appendLine(rowXml("Date", "Order #", "Type", "Table", "Customer", "Status", "Subtotal", "Discount", "Total (PKR)", "Staff ID"))
        snapshot.filteredOrders.forEach { ord ->
            sb.appendLine(
                """      <Row>""" +
                    cellStr(ord.dateKey) + cellStr(ord.orderNumber) + cellStr(ord.orderType) + cellStr(ord.tableLabel) +
                    cellStr(ord.customerName) + cellStr(ord.status) + cellNum(ord.subtotalPkr) + cellNum(ord.discountPkr) +
                    cellNum(ord.totalPkr) + cellStr(ord.handledByStaffId) +
                    """</Row>"""
            )
        }
        sb.appendLine("""    </Table>""")
        sb.appendLine("""  </Worksheet>""")

        // Sheet 4: Staff Payroll
        sb.appendLine("""  <Worksheet ss:Name="Staff_Payroll">""")
        sb.appendLine("""    <Table>""")
        sb.appendLine(rowXml("Staff ID", "Name", "Role Tier", "Monthly Salary (PKR)", "Salary Status", "Loyalty Pts", "Orders Handled"))
        snapshot.staffList.forEach { s ->
            sb.appendLine(
                """      <Row>""" +
                    cellStr(s.staffId) + cellStr(s.fullName) + cellStr(s.roleTier) +
                    cellNum(s.monthlySalaryPkr) + cellStr(s.salaryStatus) +
                    cellNum(s.loyaltyPoints) + cellNum(s.ordersHandledCount) +
                    """</Row>"""
            )
        }
        sb.appendLine("""    </Table>""")
        sb.appendLine("""  </Worksheet>""")
        sb.appendLine("""</Workbook>""")
        return sb.toString()
    }

    private fun cellStr(v: String) = """<Cell><Data ss:Type="String">${escapeXml(v)}</Data></Cell>"""
    private fun cellNum(v: Long) = """<Cell><Data ss:Type="Number">$v</Data></Cell>"""
    private fun rowXml(vararg cols: String) = "      <Row>${cols.joinToString("") { cellStr(it) }}</Row>"
    private fun rowNumXml(label: String, value: Long) = "      <Row>${cellStr(label)}${cellNum(value)}</Row>"
}

package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DishVariety
import com.example.data.ExpenseCategory
import com.example.data.MenuCategoryRecord
import com.example.data.MenuItem
import com.example.data.OrderType
import com.example.data.SpiceLevel
import com.example.data.UserRoleTier
import com.example.data.defaultFourVarietiesForPrice
import com.example.data.formatPkr
import com.example.data.parsedVarieties
import com.example.data.resolveCategoryDisplayLabel
import com.example.services.FinancialReportService
import com.example.services.OrderPrintService
import com.example.services.ReportPeriod
import com.example.ui.components.AdminAlarmControlStrip
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonthlyTrendChartCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import com.example.viewmodel.RestaurantViewModel

@Composable
fun OwnerDashboardScreen(viewModel: RestaurantViewModel) {
    val alarmEnabled by viewModel.orderAlarmEnabled.collectAsStateWithLifecycle()
    val isAlarmRinging by viewModel.isOrderAlarmRinging.collectAsStateWithLifecycle()
    val isTestAlarmActive by viewModel.isTestAlarmActive.collectAsStateWithLifecycle()
    val alarmRemainingSeconds by viewModel.alarmRemainingSeconds.collectAsStateWithLifecycle()
    val pendingIncomingOrders by viewModel.pendingIncomingOrders.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("KPIs & Reports", "Riders", "Tables", "Menu & Rates", "Staff & Salaries", "Loyalty & Stock")

    Column(modifier = Modifier.fillMaxSize()) {
        AdminAlarmControlStrip(
            alarmEnabled = alarmEnabled,
            isRinging = isAlarmRinging,
            isTestAlarmActive = isTestAlarmActive,
            remainingSeconds = alarmRemainingSeconds,
            pendingCount = pendingIncomingOrders.size,
            onToggleEnabled = { viewModel.setOrderAlarmEnabled(it) },
            onToggleTestAlarm = {
                if (isTestAlarmActive) viewModel.stopTestOrderAlarm()
                else viewModel.startTestOrderAlarm()
            }
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SmokedAmber,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) },
                    modifier = Modifier.testTag("owner_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> OwnerAnalyticsAndReportsTab(viewModel)
            1 -> OwnerRidersTab(viewModel)
            2 -> OwnerTablesTab(viewModel)
            3 -> OwnerMenuStudioTab(viewModel)
            4 -> OwnerStaffPayrollTab(viewModel)
            else -> OwnerLoyaltyAndStockTab(viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerAnalyticsAndReportsTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val dailyReport by viewModel.dailyReport.collectAsStateWithLifecycle()
    val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()
    val monthlyTrends by viewModel.monthlyTrends.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.DAILY) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    val activeReport = if (selectedPeriod == ReportPeriod.DAILY) dailyReport else monthlyReport
    val lowStockCount = inventory.count { it.isLowStock() }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FinancialReportService.writePdfReport(activeReport, out)
                }
                viewModel.showMessage("Saved ${activeReport.period.displayLabel} PDF!")
            } catch (e: Exception) {
                viewModel.showMessage("PDF export failed: ${e.localizedMessage}")
            }
        }
    }

    val excelExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FinancialReportService.writeExcelWorkbook(activeReport, out)
                }
                viewModel.showMessage("Saved ${activeReport.period.displayLabel} Excel workbook (.xls)!")
            } catch (e: Exception) {
                viewModel.showMessage("Excel export failed: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Oak & Smoke Analytics",
                subtitle = "Real-time COD sales, expenses & profit (${activeReport.periodKey})"
            )
        }

        // Period Switcher
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.displayLabel) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmokedAmber, selectedLabelColor = CharcoalDeep),
                        modifier = Modifier.testTag("period_chip_${period.name.lowercase()}")
                    )
                }
            }
        }

        // KPI Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    label = "COD Sales",
                    value = formatPkr(activeReport.grossSalesPkr),
                    accent = SmokedAmber,
                    icon = Icons.Filled.Payments,
                    caption = "${activeReport.deliveredOrderCount} delivered",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    label = "Expenses",
                    value = formatPkr(activeReport.totalExpensesPkr),
                    accent = CrimsonEmber,
                    icon = Icons.Filled.Receipt,
                    caption = "${activeReport.filteredExpenses.size} entries",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    label = "Net Profit",
                    value = formatPkr(activeReport.netProfitPkr),
                    accent = if (activeReport.netProfitPkr >= 0) CorianderGreen else CrimsonEmber,
                    icon = Icons.Filled.TrendingUp,
                    caption = "Avg ${formatPkr(activeReport.averageTicketPkr)}/ord",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    label = "Active / Alerts",
                    value = "${activeReport.activeOrderCount} / $lowStockCount",
                    accent = if (lowStockCount > 0) TandoorCopper else CorianderGreen,
                    icon = if (lowStockCount > 0) Icons.Filled.Warning else Icons.Filled.LocalShipping,
                    caption = "Live orders / Low stock",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Monthly Trends Chart
        item {
            MonthlyTrendChartCard(trends = monthlyTrends)
        }

        // Print & PDF/Excel Export Hub
        item {
            SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.6f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Cloud Print & Financial Export (${activeReport.period.displayLabel})",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Print Daily Summary", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Launch Android Cloud/Wi-Fi Print dialog automatically when closing day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config?.autoPrintDailyReport ?: false,
                            onCheckedChange = { enabled ->
                                viewModel.toggleAutoPrint(enabled)
                                if (enabled) {
                                    FinancialReportService.printFinancialReport(context, dailyReport)
                                }
                            }
                        )
                    }

                    Button(
                        onClick = { FinancialReportService.printFinancialReport(context, activeReport) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("print_report_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Print ${activeReport.period.displayLabel} (Cloud / Wi-Fi)", fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pdfExportLauncher.launch("OakAndSmoke_${activeReport.period.name.lowercase()}_${activeReport.periodKey}.pdf")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = CrimsonEmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                excelExportLauncher.launch("OakAndSmoke_${activeReport.period.name.lowercase()}_${activeReport.periodKey}.xls")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_btn")
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, tint = CorianderGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Excel")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { FinancialReportService.shareReportFile(context, activeReport, isPdf = true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share PDF", color = SmokedAmber)
                        }
                        TextButton(
                            onClick = { FinancialReportService.shareReportFile(context, activeReport, isPdf = false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share Excel", color = SmokedAmber)
                        }
                    }
                }
            }
        }

        // Quick Order Bills & KOT Printing
        if (allOrders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Order Bills & KOT Print",
                    subtitle = "One-tap Kitchen Ticket (no prices) or Full Customer Bill"
                )
            }
            items(allOrders.take(5), key = { "owner_bill_${it.id}" }) { ord ->
                SmokedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val typeLbl = if (ord.tableLabel.isNotBlank()) {
                                "${OrderType.fromRaw(ord.orderType).displayLabel} · ${ord.tableLabel}"
                            } else {
                                OrderType.fromRaw(ord.orderType).displayLabel
                            }
                            Text("${ord.orderNumber} · ${ord.customerName}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "$typeLbl · ${formatPkr(ord.totalPkr)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SmokedAmber
                            )
                        }
                        IconButton(
                            onClick = { OrderPrintService.printKitchenTicket(context, ord) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("owner_print_kot_${ord.id}")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Print KOT ${ord.orderNumber}", tint = TandoorCopper)
                        }
                        IconButton(
                            onClick = { OrderPrintService.printCustomerBill(context, ord) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("owner_print_bill_${ord.id}")
                        ) {
                            Icon(Icons.Filled.Receipt, contentDescription = "Print Bill ${ord.orderNumber}", tint = SmokedAmber)
                        }
                    }
                }
            }
        }

        // Daily & Monthly Expenses Ledger
        item {
            SectionHeader(
                title = "Operating Expenses",
                subtitle = "Ingredients, Oak Charcoal, Utilities & Salaries",
                trailing = {
                    Button(
                        onClick = { showAddExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                        modifier = Modifier.testTag("add_expense_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Expense", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Log")
                    }
                }
            )
        }

        if (expenses.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Receipt,
                    title = "No expenses logged",
                    subtitle = "Tap Log to record daily meat, oak charcoal, utility, or salary expenses for accurate net profit."
                )
            }
        } else {
            items(expenses, key = { it.id }) { exp ->
                SmokedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${ExpenseCategory.fromRaw(exp.category).displayLabel} · ${exp.dateKey}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (exp.notes.isNotBlank()) {
                                Text(exp.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(formatPkr(exp.amountPkr), style = MonoPriceStyle, color = CrimsonEmber)
                        if (effectiveRole == UserRoleTier.OWNER) {
                            IconButton(onClick = { viewModel.deleteExpense(exp.id) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete expense", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        var title by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(ExpenseCategory.INGREDIENTS) }
        var notes by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Log Operating Expense") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; errorText = null },
                        label = { Text("Expense Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() }; errorText = null },
                        label = { Text("Amount (PKR)") },
                        singleLine = true
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExpenseCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.displayLabel) }
                            )
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") })
                    if (errorText != null) {
                        Text(errorText!!, style = MaterialTheme.typography.bodySmall, color = CrimsonEmber)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        when {
                            title.trim().length < 2 -> errorText = "Enter an expense title (at least 2 characters)."
                            amt <= 0L -> errorText = "Enter a valid amount in PKR."
                            else -> {
                                viewModel.addExpense(title, category, amt, notes)
                                showAddExpenseDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerMenuStudioTab(viewModel: RestaurantViewModel) {
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val menuCategories by viewModel.menuCategories.collectAsStateWithLifecycle()
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    var editingDish by remember { mutableStateOf<MenuItem?>(null) }
    var rateEditingDish by remember { mutableStateOf<MenuItem?>(null) }
    var removingDish by remember { mutableStateOf<MenuItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var removingCategory by remember { mutableStateOf<MenuCategoryRecord?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("ALL") }

    val filteredDishes = remember(menuItems, searchQuery, categoryFilter) {
        menuItems.filter { dish ->
            val catOk = categoryFilter == "ALL" || dish.category == categoryFilter
            val qOk = searchQuery.isBlank() ||
                dish.name.contains(searchQuery, ignoreCase = true) ||
                dish.description.contains(searchQuery, ignoreCase = true)
            catOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Menu, Categories & Dish Rates",
                subtitle = "${menuItems.size} dishes · ${menuCategories.size} categories · Add/remove anytime",
                trailing = {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                        modifier = Modifier.testTag("add_menu_dish_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Dish", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Dish")
                    }
                }
            )
        }

        // Admin Category Manager Card (Add & Remove Categories)
        item {
            SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.45f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Category,
                                contentDescription = "Manage Categories",
                                tint = SmokedAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Menu Categories", style = MaterialTheme.typography.titleSmall, color = SmokedAmber)
                                Text(
                                    text = "Add custom sections (e.g. Handi, Fast Food, Desserts) or remove unused ones",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = true },
                            border = BorderStroke(1.dp, SmokedAmber),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_category_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Category", tint = SmokedAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Category", color = SmokedAmber, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        menuCategories.forEach { cat ->
                            val countInCat = menuItems.count { it.category.equals(cat.code, ignoreCase = true) }
                            FilterChip(
                                selected = categoryFilter == cat.code,
                                onClick = {
                                    categoryFilter = if (categoryFilter == cat.code) "ALL" else cat.code
                                },
                                label = { Text("${cat.displayLabel} ($countInCat)") },
                                trailingIcon = if (menuCategories.size > 1) {
                                    {
                                        IconButton(
                                            onClick = { removingCategory = cat },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("remove_category_${cat.code.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove ${cat.displayLabel}",
                                                tint = CrimsonEmber,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmokedAmber,
                                    selectedLabelColor = CharcoalDeep
                                ),
                                modifier = Modifier.testTag("admin_cat_chip_${cat.code.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search dish to update rate or remove...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search menu") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_menu_search_input")
            )
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val filters = listOf("ALL" to "All (${menuItems.size})") +
                    menuCategories.map { it.code to it.displayLabel }
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = categoryFilter == key,
                        onClick = { categoryFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmokedAmber,
                            selectedLabelColor = CharcoalDeep
                        )
                    )
                }
            }
        }

        if (menuItems.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.RestaurantMenu,
                    title = "No menu dishes yet",
                    subtitle = "Tap Add Dish above or load the Oak & Smoke starter Pakistani Karahi & BBQ menu below."
                )
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.seedStarterMenu() },
                    border = BorderStroke(1.dp, SmokedAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load Starter Karahi & BBQ Menu", color = SmokedAmber, fontWeight = FontWeight.Bold)
                }
            }
        } else if (filteredDishes.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Search,
                    title = "No matching dishes",
                    subtitle = "Clear your search or choose another category."
                )
            }
        } else {
            items(filteredDishes, key = { it.id }) { dish ->
                SmokedCard(borderColor = if (dish.available) MaterialTheme.colorScheme.outline else CrimsonEmber.copy(alpha = 0.6f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(dish.name, style = MaterialTheme.typography.titleSmall)
                                    if (dish.dailySpecial) StatusPill(label = "SPECIAL", color = TandoorCopper)
                                    if (!dish.available) StatusPill(label = "SOLD OUT", color = CrimsonEmber)
                                }
                                val dishVarieties = remember(dish.id, dish.varietiesDetail, dish.pricePkr) {
                                    dish.parsedVarieties()
                                }
                                val priceRangeText = if (dishVarieties.size > 1) {
                                    val minP = dishVarieties.minOf { it.pricePkr }
                                    val maxP = dishVarieties.maxOf { it.pricePkr }
                                    "${dishVarieties.size} varieties · ${formatPkr(minP)} – ${formatPkr(maxP)}"
                                } else {
                                    formatPkr(dish.pricePkr)
                                }
                                Text(
                                    text = "${resolveCategoryDisplayLabel(dish.category, menuCategories)} · $priceRangeText",
                                    style = MonoPriceStyle,
                                    color = SmokedAmber
                                )
                            }
                            OutlinedButton(
                                onClick = { rateEditingDish = dish },
                                border = BorderStroke(1.dp, SmokedAmber),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("quick_rate_${dish.id}")
                            ) {
                                Icon(Icons.Filled.PriceChange, contentDescription = "Change Rate", tint = SmokedAmber, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rate", color = SmokedAmber)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = dish.available,
                                    onClick = { viewModel.toggleMenuItemFlags(dish, !dish.available, dish.dailySpecial) },
                                    label = { Text(if (dish.available) "In Stock" else "Sold Out") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CorianderGreen.copy(alpha = 0.22f),
                                        selectedLabelColor = CorianderGreen
                                    )
                                )
                                FilterChip(
                                    selected = dish.dailySpecial,
                                    onClick = { viewModel.toggleMenuItemFlags(dish, dish.available, !dish.dailySpecial) },
                                    label = { Text("Special") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TandoorCopper.copy(alpha = 0.22f),
                                        selectedLabelColor = TandoorCopper
                                    )
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { editingDish = dish },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("edit_dish_${dish.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${dish.name}", tint = SmokedAmber)
                                }
                                if (effectiveRole == UserRoleTier.OWNER) {
                                    IconButton(
                                        onClick = { removingDish = dish },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_dish_${dish.id}")
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove ${dish.name}", tint = CrimsonEmber)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingDish != null) {
        MenuDishEditorDialog(
            existing = editingDish,
            categories = menuCategories,
            onDismiss = {
                showCreateDialog = false
                editingDish = null
            },
            onSave = { name, catCode, desc, price, varieties, imgKey, special, avail, spice, prep, broadcast ->
                viewModel.saveMenuItem(
                    existingId = editingDish?.id,
                    name = name,
                    categoryCode = catCode,
                    description = desc,
                    pricePkr = price,
                    varieties = varieties,
                    imageKey = imgKey,
                    dailySpecial = special,
                    available = avail,
                    spiceLevel = spice,
                    prepMinutes = prep,
                    broadcastAlert = broadcast
                )
                showCreateDialog = false
                editingDish = null
            }
        )
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var catError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Menu Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "New categories appear immediately in Customer Menu, Staff Counter POS, and Dish Editor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it; catError = null },
                        label = { Text("Category Name (e.g. Matka Handi, Fast Food, Desserts)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_category_name_input")
                    )
                    if (catError != null) {
                        Text(catError!!, style = MaterialTheme.typography.bodySmall, color = CrimsonEmber)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCatName.trim()
                        if (trimmed.length < 2) {
                            catError = "Enter a category name (at least 2 characters)."
                        } else if (menuCategories.any { it.displayLabel.equals(trimmed, ignoreCase = true) }) {
                            catError = "This category already exists."
                        } else {
                            viewModel.addMenuCategory(trimmed)
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                    modifier = Modifier.testTag("confirm_add_category_btn")
                ) {
                    Text("Add Category", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    removingCategory?.let { targetCat ->
        val affectedDishes = menuItems.count { it.category.equals(targetCat.code, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { removingCategory = null },
            title = { Text("Remove Category?") },
            text = {
                Text(
                    if (affectedDishes > 0) {
                        "Remove \"${targetCat.displayLabel}\" from the category filters? ($affectedDishes existing dish(es) will still remain in All Menu and can be reassigned anytime.)"
                    } else {
                        "Remove \"${targetCat.displayLabel}\" from the Oak & Smoke menu categories?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryFilter == targetCat.code) categoryFilter = "ALL"
                        viewModel.deleteMenuCategory(targetCat)
                        removingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmber, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.testTag("confirm_remove_category_btn")
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { removingCategory = null }) { Text("Cancel") }
            }
        )
    }

    rateEditingDish?.let { target ->
        var rateText by remember(target.id) { mutableStateOf(target.pricePkr.toString()) }
        var rateError by remember(target.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { rateEditingDish = null },
            title = { Text("Update Rate · ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current rate: ${formatPkr(target.pricePkr)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it.filter { c -> c.isDigit() }; rateError = null },
                        label = { Text("New Rate (PKR)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_rate_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-100L to "-100", -50L to "-50", 50L to "+50", 100L to "+100").forEach { (delta, lbl) ->
                            OutlinedButton(
                                onClick = {
                                    val cur = rateText.toLongOrNull() ?: target.pricePkr
                                    rateText = (cur + delta).coerceIn(50L, 250000L).toString()
                                    rateError = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(lbl, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (rateError != null) {
                        Text(rateError!!, style = MaterialTheme.typography.bodySmall, color = CrimsonEmber)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = rateText.toLongOrNull() ?: 0L
                        if (parsed !in 50L..250000L) {
                            rateError = "Enter a valid PKR rate between Rs. 50 and Rs. 250,000."
                        } else {
                            viewModel.updateMenuItemPrice(target, parsed)
                            rateEditingDish = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                    modifier = Modifier.testTag("confirm_quick_rate_btn")
                ) {
                    Text("Update Rate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rateEditingDish = null }) { Text("Cancel") }
            }
        )
    }

    removingDish?.let { target ->
        AlertDialog(
            onDismissRequest = { removingDish = null },
            title = { Text("Remove Dish from Menu?") },
            text = {
                Text("Remove \"${target.name}\" (${formatPkr(target.pricePkr)}) from the live Oak & Smoke menu? Existing past orders will not be affected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMenuItem(target.id)
                        removingDish = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmber, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.testTag("confirm_delete_dish_btn")
                ) {
                    Text("Remove Dish", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { removingDish = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuDishEditorDialog(
    existing: MenuItem?,
    categories: List<MenuCategoryRecord>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, List<DishVariety>, String, Boolean, Boolean, SpiceLevel, Long, Boolean) -> Unit
) {
    val initialVarieties = remember(existing?.id) {
        val parsed = existing?.parsedVarieties().orEmpty()
        if (parsed.size >= 4) {
            parsed.take(4)
        } else {
            val defaults = defaultFourVarietiesForPrice(existing?.pricePkr ?: 1200L)
            List(4) { idx -> parsed.getOrElse(idx) { defaults[idx] } }
        }
    }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var categoryCode by remember {
        mutableStateOf(existing?.category?.ifBlank { categories.firstOrNull()?.code ?: "KARAHI" } ?: (categories.firstOrNull()?.code ?: "KARAHI"))
    }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var priceText by remember { mutableStateOf(existing?.pricePkr?.toString() ?: initialVarieties[0].pricePkr.toString()) }
    var v1Label by remember { mutableStateOf(initialVarieties[0].label) }
    var v1Price by remember { mutableStateOf(initialVarieties[0].pricePkr.toString()) }
    var v2Label by remember { mutableStateOf(initialVarieties[1].label) }
    var v2Price by remember { mutableStateOf(initialVarieties[1].pricePkr.toString()) }
    var v3Label by remember { mutableStateOf(initialVarieties[2].label) }
    var v3Price by remember { mutableStateOf(initialVarieties[2].pricePkr.toString()) }
    var v4Label by remember { mutableStateOf(initialVarieties[3].label) }
    var v4Price by remember { mutableStateOf(initialVarieties[3].pricePkr.toString()) }
    var imageKey by remember { mutableStateOf(existing?.imageKey ?: "karahi") }
    var dailySpecial by remember { mutableStateOf(existing?.dailySpecial ?: false) }
    var available by remember { mutableStateOf(existing?.available ?: true) }
    var spiceLevel by remember { mutableStateOf(SpiceLevel.fromRaw(existing?.spiceLevel)) }
    var prepText by remember { mutableStateOf((existing?.prepMinutes ?: 20L).toString()) }
    var broadcastAlert by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Menu Dish (4 Varieties)" else "Edit Dish & 4 Varieties") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; errorText = null }, label = { Text("Dish Name") }, singleLine = true)
                OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter { c -> c.isDigit() }; errorText = null }, label = { Text("Base Price (PKR)") }, singleLine = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4 Product Varieties", style = MaterialTheme.typography.labelLarge, color = SmokedAmber)
                    TextButton(
                        onClick = {
                            val base = (priceText.toLongOrNull() ?: 1200L).coerceIn(50L, 250000L)
                            val gen = defaultFourVarietiesForPrice(base)
                            v1Label = gen[0].label; v1Price = gen[0].pricePkr.toString()
                            v2Label = gen[1].label; v2Price = gen[1].pricePkr.toString()
                            v3Label = gen[2].label; v3Price = gen[2].pricePkr.toString()
                            v4Label = gen[3].label; v4Price = gen[3].pricePkr.toString()
                        }
                    ) {
                        Text("Auto-Fill", color = SmokedAmber, style = MaterialTheme.typography.labelSmall)
                    }
                }

                @Composable
                fun VarietyInputRow(
                    index: Int,
                    labelValue: String,
                    onLabelChange: (String) -> Unit,
                    priceValue: String,
                    onPriceChange: (String) -> Unit
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = labelValue,
                            onValueChange = { onLabelChange(it); errorText = null },
                            label = { Text("Variety $index") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("dish_variety_label_$index")
                        )
                        OutlinedTextField(
                            value = priceValue,
                            onValueChange = { onPriceChange(it.filter { c -> c.isDigit() }); errorText = null },
                            label = { Text("PKR") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("dish_variety_price_$index")
                        )
                    }
                }

                VarietyInputRow(1, v1Label, { v1Label = it }, v1Price, { v1Price = it })
                VarietyInputRow(2, v2Label, { v2Label = it }, v2Price, { v2Price = it })
                VarietyInputRow(3, v3Label, { v3Label = it }, v3Price, { v3Price = it })
                VarietyInputRow(4, v4Label, { v4Label = it }, v4Price, { v4Price = it })

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3)
                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryCode.equals(cat.code, ignoreCase = true),
                            onClick = { categoryCode = cat.code },
                            label = { Text(cat.displayLabel) }
                        )
                    }
                }
                Text("Photo Style", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("karahi" to "Karahi", "platter" to "Platter", "kebabs" to "Kebabs/BBQ", "hero" to "Grill").forEach { (k, lbl) ->
                        FilterChip(selected = imageKey == k, onClick = { imageKey = k }, label = { Text(lbl) })
                    }
                }
                Text("Spice Level", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpiceLevel.entries.forEach { sp ->
                        FilterChip(selected = spiceLevel == sp, onClick = { spiceLevel = sp }, label = { Text(sp.displayLabel) })
                    }
                }
                OutlinedTextField(value = prepText, onValueChange = { prepText = it.filter { c -> c.isDigit() } }, label = { Text("Prep Time (mins)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dailySpecial, onCheckedChange = { dailySpecial = it })
                    Text("Feature as Daily Special")
                }
                if (dailySpecial) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = broadcastAlert, onCheckedChange = { broadcastAlert = it })
                        Text("Send Daily Special alert to customers")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = available, onCheckedChange = { available = it })
                    Text("Available for ordering")
                }
                if (errorText != null) {
                    Text(errorText!!, style = MaterialTheme.typography.bodySmall, color = CrimsonEmber)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val basePrice = priceText.toLongOrNull() ?: 0L
                    val prep = prepText.toLongOrNull() ?: 20L
                    val rawVarieties = listOf(
                        v1Label to (v1Price.toLongOrNull() ?: 0L),
                        v2Label to (v2Price.toLongOrNull() ?: 0L),
                        v3Label to (v3Price.toLongOrNull() ?: 0L),
                        v4Label to (v4Price.toLongOrNull() ?: 0L)
                    )
                    val parsedVarieties = rawVarieties.mapNotNull { (lbl, p) ->
                        val clean = lbl.trim()
                        if (clean.isNotBlank() && p in 50L..250000L) DishVariety(clean, p) else null
                    }
                    val effectivePrice = if (basePrice in 50L..250000L) basePrice else (parsedVarieties.firstOrNull()?.pricePkr ?: 0L)
                    when {
                        name.trim().length < 2 -> errorText = "Enter a dish name (at least 2 characters)."
                        effectivePrice < 50L -> errorText = "Base price must be at least Rs. 50."
                        parsedVarieties.isEmpty() -> errorText = "Enter at least 1 valid variety with a PKR price >= Rs. 50."
                        else -> onSave(
                            name,
                            categoryCode,
                            description,
                            effectivePrice,
                            parsedVarieties,
                            imageKey,
                            dailySpecial,
                            available,
                            spiceLevel,
                            prep,
                            broadcastAlert
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
            ) {
                Text("Save Dish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

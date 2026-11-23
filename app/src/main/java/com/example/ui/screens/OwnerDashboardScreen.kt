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
import com.example.data.ExpenseCategory
import com.example.data.MenuCategory
import com.example.data.MenuItem
import com.example.data.OrderType
import com.example.data.SpiceLevel
import com.example.data.UserRoleTier
import com.example.data.formatPkr
import com.example.services.FinancialReportService
import com.example.services.OrderPrintService
import com.example.services.ReportPeriod
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("KPIs & Reports", "Riders", "Tables", "Menu & Rates", "Staff & Salaries", "Loyalty & Stock")

    Column(modifier = Modifier.fillMaxSize()) {
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
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    var editingDish by remember { mutableStateOf<MenuItem?>(null) }
    var rateEditingDish by remember { mutableStateOf<MenuItem?>(null) }
    var removingDish by remember { mutableStateOf<MenuItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
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
                title = "Menu, Dish Rates & Specials",
                subtitle = "${menuItems.size} dishes · Add dishes, update PKR rates & remove items",
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
                    MenuCategory.entries.map { it.name to it.displayLabel }
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
                                Text(
                                    text = "${MenuCategory.fromRaw(dish.category).displayLabel} · ${formatPkr(dish.pricePkr)}",
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
            onDismiss = {
                showCreateDialog = false
                editingDish = null
            },
            onSave = { name, cat, desc, price, imgKey, special, avail, spice, prep, broadcast ->
                viewModel.saveMenuItem(
                    editingDish?.id, name, cat, desc, price, imgKey, special, avail, spice, prep, broadcast
                )
                showCreateDialog = false
                editingDish = null
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
    onDismiss: () -> Unit,
    onSave: (String, MenuCategory, String, Long, String, Boolean, Boolean, SpiceLevel, Long, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember { mutableStateOf(MenuCategory.fromRaw(existing?.category)) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var priceText by remember { mutableStateOf(existing?.pricePkr?.toString().orEmpty()) }
    var imageKey by remember { mutableStateOf(existing?.imageKey ?: "karahi") }
    var dailySpecial by remember { mutableStateOf(existing?.dailySpecial ?: false) }
    var available by remember { mutableStateOf(existing?.available ?: true) }
    var spiceLevel by remember { mutableStateOf(SpiceLevel.fromRaw(existing?.spiceLevel)) }
    var prepText by remember { mutableStateOf((existing?.prepMinutes ?: 20L).toString()) }
    var broadcastAlert by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Menu Dish" else "Edit Dish") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; errorText = null }, label = { Text("Dish Name") }, singleLine = true)
                OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter { c -> c.isDigit() }; errorText = null }, label = { Text("Price (PKR)") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3)
                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MenuCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.displayLabel) })
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
                    val price = priceText.toLongOrNull() ?: 0L
                    val prep = prepText.toLongOrNull() ?: 20L
                    when {
                        name.trim().length < 2 -> errorText = "Enter a dish name (at least 2 characters)."
                        price < 50L -> errorText = "Price must be at least Rs. 50."
                        else -> onSave(name, category, description, price, imageKey, dailySpecial, available, spiceLevel, prep, broadcastAlert)
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

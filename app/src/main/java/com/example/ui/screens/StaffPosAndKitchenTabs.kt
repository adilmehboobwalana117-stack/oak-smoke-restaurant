package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CartLine
import com.example.data.DiningTable
import com.example.data.MenuCategory
import com.example.data.MenuItem
import com.example.data.OakAndSmokeSeeds
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.SeatingZone
import com.example.data.UserRoleTier
import com.example.data.cartItemKey
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.data.parsedLineItems
import com.example.data.parsedVarieties
import com.example.data.resolveCartLines
import com.example.services.OrderPrintService
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import com.example.viewmodel.RestaurantViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffTakeOrderTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val diningTables by viewModel.diningTables.collectAsStateWithLifecycle()
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSyncIds by viewModel.pendingSyncOrderIds.collectAsStateWithLifecycle()

    val effectiveMenu = remember(menuItems) {
        if (menuItems.isNotEmpty()) menuItems else OakAndSmokeSeeds.starterMenu
    }
    val activeTables = remember(diningTables) {
        val liveActive = diningTables.filter { it.available }
        if (liveActive.isNotEmpty()) liveActive else OakAndSmokeSeeds.starterTables
    }
    val canManageTables = effectiveRole == UserRoleTier.OWNER || effectiveRole == UserRoleTier.MANAGER

    var orderType by remember { mutableStateOf(OrderType.DINE_IN) }
    var selectedZoneFilter by remember { mutableStateOf<SeatingZone?>(null) }
    var tableLabel by remember { mutableStateOf(activeTables.firstOrNull()?.label ?: "Table 1") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var kitchenNotes by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MenuCategory?>(null) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var createdOrderDialog by remember { mutableStateOf<Pair<OrderRecord, Boolean>?>(null) }
    var showQuickAddTableDialog by remember { mutableStateOf(false) }

    val filteredTables = remember(activeTables, selectedZoneFilter) {
        if (selectedZoneFilter == null) activeTables
        else activeTables.filter { it.zone == selectedZoneFilter?.name }
    }

    val availableMenu = remember(effectiveMenu, searchQuery, selectedCategory) {
        effectiveMenu.filter { dish ->
            dish.available &&
                (selectedCategory == null || dish.category == selectedCategory?.name) &&
                (searchQuery.isBlank() ||
                    dish.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    dish.varietiesDetail.contains(searchQuery.trim(), ignoreCase = true))
        }
    }

    val selectedLines = remember(effectiveMenu, quantities) {
        resolveCartLines(effectiveMenu, quantities)
    }
    val totalCount = selectedLines.sumOf { it.quantity }
    val totalAmountPkr = selectedLines.sumOf { it.unitPricePkr * it.quantity }

    val canSend = !isBusy &&
        selectedLines.isNotEmpty() &&
        (orderType != OrderType.DINE_IN || tableLabel.trim().isNotEmpty()) &&
        (orderType != OrderType.COD_DELIVERY || (customerPhone.trim().length >= 7 && deliveryAddress.trim().length >= 4))

    fun submitOrder() {
        viewModel.createStaffOrderDirectToKitchen(
            orderType = orderType,
            tableLabel = if (orderType == OrderType.DINE_IN) tableLabel else "",
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = if (orderType == OrderType.COD_DELIVERY) deliveryAddress else "",
            cartLines = selectedLines,
            notes = kitchenNotes
        ) { created, syncedOnline ->
            createdOrderDialog = created to syncedOnline
            quantities = emptyMap()
            customerName = ""
            customerPhone = ""
            deliveryAddress = ""
            kitchenNotes = ""
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideDesktop = maxWidth >= 840.dp

        if (isWideDesktop) {
            // Desktop / PC Two-Pane POS Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Pane: Connectivity, 25-Table Selector, 4-Variety Dish Picker
                LazyColumn(
                    modifier = Modifier
                        .weight(1.45f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StaffSyncStatusBanner(
                            isOnline = isOnline,
                            pendingSyncCount = pendingSyncIds.size
                        )
                    }
                    item {
                        SectionHeader(
                            title = "Staff Counter POS · 25 Tables & 4 Varieties",
                            subtitle = "Works Online & Offline · Instant KOT & Customer Bill printing"
                        )
                    }
                    item {
                        StaffOrderDetailsCard(
                            orderType = orderType,
                            onOrderTypeChange = { orderType = it },
                            activeTables = activeTables,
                            filteredTables = filteredTables,
                            selectedZoneFilter = selectedZoneFilter,
                            onSelectZone = { selectedZoneFilter = it },
                            tableLabel = tableLabel,
                            onTableLabelChange = { tableLabel = it },
                            canManageTables = canManageTables,
                            onQuickAddTable = { showQuickAddTableDialog = true },
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            customerPhone = customerPhone,
                            onCustomerPhoneChange = { customerPhone = it },
                            deliveryAddress = deliveryAddress,
                            onDeliveryAddressChange = { deliveryAddress = it },
                            kitchenNotes = kitchenNotes,
                            onKitchenNotesChange = { kitchenNotes = it }
                        )
                    }
                    item {
                        StaffDishSearchAndFilter(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            totalCount = totalCount,
                            totalAmountPkr = totalAmountPkr
                        )
                    }
                    if (availableMenu.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Restaurant,
                                title = "No matching dishes",
                                subtitle = "Adjust your search or category filter to select menu items."
                            )
                        }
                    } else {
                        items(availableMenu, key = { it.id }) { dish ->
                            StaffVarietyDishCard(
                                dish = dish,
                                quantities = quantities,
                                onIncrement = { key ->
                                    quantities = quantities.toMutableMap().apply {
                                        put(key, (get(key) ?: 0) + 1)
                                    }
                                },
                                onDecrement = { key ->
                                    quantities = quantities.toMutableMap().apply {
                                        val next = (get(key) ?: 0) - 1
                                        if (next <= 0) remove(key) else put(key, next)
                                    }
                                }
                            )
                        }
                    }
                }

                // Right Pane: Live PC Counter Ticket & Fire-to-Kitchen Panel
                Surface(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, EmberBorder),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Live Order Ticket", style = MaterialTheme.typography.titleMedium, color = SmokedAmber)
                                Text(
                                    text = if (orderType == OrderType.DINE_IN) "${orderType.displayLabel} · $tableLabel" else orderType.displayLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedLines.isNotEmpty()) {
                                TextButton(onClick = { quantities = emptyMap() }) {
                                    Text("Clear", color = TandoorCopper)
                                }
                            }
                        }

                        HorizontalDivider(color = EmberBorder)

                        if (selectedLines.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a variety and tap + on dishes from the left to build this ticket.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedLines.forEach { line ->
                                    val key = cartItemKey(line.item.id, line.varietyLabel)
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, EmberBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = line.displayNameWithVariety(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${line.quantity} x ${formatPkr(line.unitPricePkr)} = ${formatPkr(line.lineTotalPkr)}",
                                                    style = MonoPriceStyle,
                                                    color = SmokedAmber
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        quantities = quantities.toMutableMap().apply {
                                                            val next = (get(key) ?: 0) - 1
                                                            if (next <= 0) remove(key) else put(key, next)
                                                        }
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = SmokedAmber)
                                                }
                                                Text("${line.quantity}", style = MonoPriceStyle)
                                                IconButton(
                                                    onClick = {
                                                        quantities = quantities.toMutableMap().apply {
                                                            put(key, (get(key) ?: 0) + 1)
                                                        }
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = CorianderGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = EmberBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total ($totalCount items)", style = MaterialTheme.typography.titleMedium)
                            Text(formatPkr(totalAmountPkr), style = MonoMetricStyle, color = SmokedAmber)
                        }

                        Button(
                            onClick = { submitOrder() },
                            enabled = canSend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("send_direct_to_kitchen_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) "Send Direct to Kitchen" else "Save Offline & Print KOT",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Mobile / Compact Single-Pane Layout
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StaffSyncStatusBanner(
                            isOnline = isOnline,
                            pendingSyncCount = pendingSyncIds.size
                        )
                    }
                    item {
                        SectionHeader(
                            title = "Take Order (Online & Offline)",
                            subtitle = "25 Tables · 4 Varieties per dish · Fires immediately to Kitchen"
                        )
                    }
                    item {
                        StaffOrderDetailsCard(
                            orderType = orderType,
                            onOrderTypeChange = { orderType = it },
                            activeTables = activeTables,
                            filteredTables = filteredTables,
                            selectedZoneFilter = selectedZoneFilter,
                            onSelectZone = { selectedZoneFilter = it },
                            tableLabel = tableLabel,
                            onTableLabelChange = { tableLabel = it },
                            canManageTables = canManageTables,
                            onQuickAddTable = { showQuickAddTableDialog = true },
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            customerPhone = customerPhone,
                            onCustomerPhoneChange = { customerPhone = it },
                            deliveryAddress = deliveryAddress,
                            onDeliveryAddressChange = { deliveryAddress = it },
                            kitchenNotes = kitchenNotes,
                            onKitchenNotesChange = { kitchenNotes = it }
                        )
                    }
                    item {
                        StaffDishSearchAndFilter(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            totalCount = totalCount,
                            totalAmountPkr = totalAmountPkr
                        )
                    }
                    if (availableMenu.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Restaurant,
                                title = "No matching dishes",
                                subtitle = "Adjust your search or category filter to select menu items."
                            )
                        }
                    } else {
                        items(availableMenu, key = { it.id }) { dish ->
                            StaffVarietyDishCard(
                                dish = dish,
                                quantities = quantities,
                                onIncrement = { key ->
                                    quantities = quantities.toMutableMap().apply {
                                        put(key, (get(key) ?: 0) + 1)
                                    }
                                },
                                onDecrement = { key ->
                                    quantities = quantities.toMutableMap().apply {
                                        val next = (get(key) ?: 0) - 1
                                        if (next <= 0) remove(key) else put(key, next)
                                    }
                                }
                            )
                        }
                    }
                }

                // Sticky Send Direct to Kitchen Footer Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, EmberBorder),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (orderType == OrderType.DINE_IN) "${orderType.displayLabel} · $tableLabel" else orderType.displayLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SmokedAmber
                                )
                                Text(
                                    text = "$totalCount items selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatPkr(totalAmountPkr),
                                style = MonoPriceStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { submitOrder() },
                            enabled = canSend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_direct_to_kitchen_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) "Send Direct to Kitchen" else "Save Offline & Print KOT",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Post-Send Print Dialog (Instant Print KOT or Print Full Customer Bill — works Online & Offline)
    createdOrderDialog?.let { (order, syncedOnline) ->
        AlertDialog(
            onDismissRequest = { createdOrderDialog = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (syncedOnline) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = if (syncedOnline) CorianderGreen else TandoorCopper
                    )
                    Text(
                        if (syncedOnline) "Sent to Kitchen: ${order.orderNumber}"
                        else "Saved Offline: ${order.orderNumber}"
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        label = if (syncedOnline) "SYNCED ONLINE" else "OFFLINE QUEUED · AUTO-SYNCS",
                        color = if (syncedOnline) CorianderGreen else TandoorCopper
                    )
                    val label = if (order.tableLabel.isNotBlank()) {
                        "${OrderType.fromRaw(order.orderType).displayLabel} · ${order.tableLabel}"
                    } else {
                        OrderType.fromRaw(order.orderType).displayLabel
                    }
                    Text(label, style = MaterialTheme.typography.titleSmall, color = SmokedAmber)
                    Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Bill Total: ${formatPkr(order.totalPkr)}",
                        style = MonoPriceStyle,
                        color = CorianderGreen
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Print Kitchen Ticket (no prices) or Full Customer Bill immediately — even without internet:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            OrderPrintService.printKitchenTicket(context, order).onFailure {
                                viewModel.showMessage(it.message ?: "Unable to print KOT")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_print_kot_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = TandoorCopper, contentColor = CharcoalDeep)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Print KOT (Kitchen Only)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            OrderPrintService.printCustomerBill(context, order).onFailure {
                                viewModel.showMessage(it.message ?: "Unable to print Customer Bill")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_print_bill_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Print Full Customer Bill", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { createdOrderDialog = null }) {
                    Text("Done / New Order", color = SmokedAmber)
                }
            }
        )
    }

    if (showQuickAddTableDialog) {
        DiningTableEditorDialog(
            existing = null,
            existingLabels = diningTables.map { it.label },
            suggestedNextNumber = diningTables.size + 1,
            onDismiss = { showQuickAddTableDialog = false },
            onSave = { label, zone, capacity, available ->
                viewModel.saveDiningTable(null, label, zone, capacity, available)
                tableLabel = label
                showQuickAddTableDialog = false
            }
        )
    }
}

@Composable
private fun StaffSyncStatusBanner(
    isOnline: Boolean,
    pendingSyncCount: Int
) {
    val accent = when {
        !isOnline -> TandoorCopper
        pendingSyncCount > 0 -> SmokedAmber
        else -> CorianderGreen
    }
    val icon = when {
        !isOnline -> Icons.Filled.CloudOff
        pendingSyncCount > 0 -> Icons.Filled.Sync
        else -> Icons.Filled.CloudDone
    }
    val title = when {
        !isOnline -> "Offline POS Active · Taking Orders Locally"
        pendingSyncCount > 0 -> "Syncing $pendingSyncCount Offline Order(s) to Cloud..."
        else -> "Online · Cloud POS Synced"
    }
    val subtitle = when {
        !isOnline -> "Orders save locally, show in Kitchen KOT immediately, print bills, and auto-sync when internet returns."
        pendingSyncCount > 0 -> "Queued offline orders are uploading automatically to Firestore."
        else -> "All 25 tables & 4-variety menu orders sync live across Staff, Kitchen & Owner devices."
    }

    SmokedCard(borderColor = accent.copy(alpha = 0.7f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (pendingSyncCount > 0) {
                StatusPill(label = "$pendingSyncCount QUEUED", color = TandoorCopper)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffOrderDetailsCard(
    orderType: OrderType,
    onOrderTypeChange: (OrderType) -> Unit,
    activeTables: List<DiningTable>,
    filteredTables: List<DiningTable>,
    selectedZoneFilter: SeatingZone?,
    onSelectZone: (SeatingZone?) -> Unit,
    tableLabel: String,
    onTableLabelChange: (String) -> Unit,
    canManageTables: Boolean,
    onQuickAddTable: () -> Unit,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit,
    kitchenNotes: String,
    onKitchenNotesChange: (String) -> Unit
) {
    SmokedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Order Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderType.entries.forEach { type ->
                    FilterChip(
                        selected = orderType == type,
                        onClick = { onOrderTypeChange(type) },
                        label = { Text(type.displayLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmokedAmber,
                            selectedLabelColor = CharcoalDeep
                        ),
                        modifier = Modifier.testTag("staff_order_type_${type.name.lowercase()}")
                    )
                }
            }

            if (orderType == OrderType.DINE_IN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Table / Cabin (${activeTables.size} tables)", style = MaterialTheme.typography.labelLarge)
                    if (canManageTables) {
                        TextButton(
                            onClick = onQuickAddTable,
                            modifier = Modifier.testTag("pos_quick_add_table_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Table", tint = SmokedAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Table", color = SmokedAmber)
                        }
                    }
                }

                // Zone filter chips for fast selection across 25 tables
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedZoneFilter == null,
                        onClick = { onSelectZone(null) },
                        label = { Text("All (${activeTables.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmokedAmber,
                            selectedLabelColor = CharcoalDeep
                        )
                    )
                    SeatingZone.entries.forEach { zone ->
                        val count = activeTables.count { it.zone == zone.name }
                        FilterChip(
                            selected = selectedZoneFilter == zone,
                            onClick = { onSelectZone(zone) },
                            label = { Text("${zone.displayLabel.substringBefore(" ")} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredTables.forEach { tbl ->
                        FilterChip(
                            selected = tableLabel == tbl.label,
                            onClick = { onTableLabelChange(tbl.label) },
                            label = { Text("${tbl.label} (${tbl.capacity}p)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TandoorCopper,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = tableLabel,
                    onValueChange = onTableLabelChange,
                    label = { Text("Custom Table / Cabin Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_table_input")
                )
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                label = { Text("Customer Name (optional for Dine-In/Takeaway)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_customer_name_input")
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = onCustomerPhoneChange,
                label = {
                    Text(
                        if (orderType == OrderType.COD_DELIVERY) "Customer Phone (required)"
                        else "Customer Phone (optional)"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_customer_phone_input")
            )

            if (orderType == OrderType.COD_DELIVERY) {
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = onDeliveryAddressChange,
                    label = { Text("Hasilpur Delivery Address (required)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_delivery_address_input")
                )
            }

            OutlinedTextField(
                value = kitchenNotes,
                onValueChange = onKitchenNotesChange,
                label = { Text("Kitchen Notes (spice level, allergies, serve order)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_kitchen_notes_input")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffDishSearchAndFilter(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: MenuCategory?,
    onCategoryChange: (MenuCategory?) -> Unit,
    totalCount: Int,
    totalAmountPkr: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "Select Dishes & Variety",
            subtitle = if (totalCount > 0) "$totalCount items selected · ${formatPkr(totalAmountPkr)}" else "Pick one of 4 varieties on each dish, then tap +"
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search Karahi, BBQ, Platter, Naan or variety...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("staff_menu_search_input")
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategoryChange(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SmokedAmber,
                    selectedLabelColor = CharcoalDeep
                )
            )
            MenuCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat.displayLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmokedAmber,
                        selectedLabelColor = CharcoalDeep
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffVarietyDishCard(
    dish: MenuItem,
    quantities: Map<String, Int>,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit
) {
    val varieties = remember(dish.id, dish.varietiesDetail, dish.pricePkr) { dish.parsedVarieties() }
    var selectedIdx by remember(dish.id, varieties.size) { mutableIntStateOf(0) }
    val activeVariety = varieties.getOrElse(selectedIdx) { varieties.first() }
    val activeKey = cartItemKey(dish.id, activeVariety.label)
    val activeQty = quantities[activeKey] ?: 0
    val dishTotalQty = varieties.sumOf { quantities[cartItemKey(dish.id, it.label)] ?: 0 }

    SmokedCard(
        borderColor = if (dishTotalQty > 0) SmokedAmber else MaterialTheme.colorScheme.outline
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dish.name, style = MaterialTheme.typography.titleSmall)
                        if (dishTotalQty > 0) {
                            StatusPill(label = "$dishTotalQty in order", color = CorianderGreen)
                        }
                    }
                    Text(
                        text = "${activeVariety.label}: ${formatPkr(activeVariety.pricePkr)} · ${MenuCategory.fromRaw(dish.category).displayLabel}",
                        style = MonoPriceStyle,
                        color = SmokedAmber
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeQty > 0) {
                        IconButton(
                            onClick = { onDecrement(activeKey) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("staff_pos_dec_${dish.id}")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Remove ${dish.name}", tint = SmokedAmber)
                        }
                        Text("$activeQty", style = MonoPriceStyle)
                    }
                    IconButton(
                        onClick = { onIncrement(activeKey) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("staff_pos_inc_${dish.id}")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add ${dish.name}", tint = CorianderGreen)
                    }
                }
            }

            if (varieties.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    varieties.forEachIndexed { idx, variety ->
                        val vKey = cartItemKey(dish.id, variety.label)
                        val vQty = quantities[vKey] ?: 0
                        FilterChip(
                            selected = selectedIdx == idx,
                            onClick = { selectedIdx = idx },
                            label = {
                                val qtySuffix = if (vQty > 0) " [$vQty]" else ""
                                Text(
                                    text = "${variety.label} · ${formatPkr(variety.pricePkr)}$qtySuffix",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kitchen Display (KOT): STRICTLY displays order preparation details ONLY.
 * No prices, no totals, no discounts, and no customer phone/billing address are shown here.
 */
@Composable
fun KitchenDisplayTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val pendingSyncIds by viewModel.pendingSyncOrderIds.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val kitchenOrders = remember(allOrders) {
        allOrders.filter {
            it.status == OrderStatus.PLACED.name ||
                it.status == OrderStatus.CONFIRMED.name ||
                it.status == OrderStatus.GRILLING.name ||
                it.status == OrderStatus.READY.name
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StaffSyncStatusBanner(
                isOnline = isOnline,
                pendingSyncCount = pendingSyncIds.size
            )
        }
        item {
            SectionHeader(
                title = "Kitchen Display (KOT Only)",
                subtitle = "${kitchenOrders.size} active tickets · Order preparation details only (no prices/billing)"
            )
        }

        if (kitchenOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    title = "Kitchen queue is clear",
                    subtitle = "Orders taken by staff (online or offline) or placed by customers appear here with item varieties, quantities, and notes only."
                )
            }
        } else {
            items(kitchenOrders, key = { it.id }) { order ->
                KitchenTicketCard(
                    order = order,
                    isPendingSync = order.id in pendingSyncIds,
                    onStartGrilling = { viewModel.advanceOrderStatus(order, OrderStatus.GRILLING) },
                    onMarkReady = { viewModel.advanceOrderStatus(order, OrderStatus.READY) },
                    onPrintKot = {
                        OrderPrintService.printKitchenTicket(context, order).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to print KOT")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun KitchenTicketCard(
    order: OrderRecord,
    isPendingSync: Boolean,
    onStartGrilling: () -> Unit,
    onMarkReady: () -> Unit,
    onPrintKot: () -> Unit
) {
    val status = OrderStatus.fromRaw(order.status)
    val orderType = OrderType.fromRaw(order.orderType)
    val lineItems = remember(order.itemsDetail, order.itemsSummary) { order.parsedLineItems() }
    val borderAccent = when (status) {
        OrderStatus.GRILLING -> TandoorCopper
        OrderStatus.READY -> CorianderGreen
        else -> SmokedAmber
    }

    SmokedCard(borderColor = borderAccent) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.orderNumber,
                        style = MonoMetricStyle,
                        color = SmokedAmber
                    )
                    val typeLine = if (order.tableLabel.isNotBlank()) {
                        "${orderType.displayLabel} · ${order.tableLabel}"
                    } else {
                        orderType.displayLabel
                    }
                    Text(
                        text = "$typeLine · ${formatTimestampShort(order.createdAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isPendingSync) {
                        StatusPill(label = "PENDING SYNC", color = TandoorCopper)
                    }
                    StatusPill(label = status.displayLabel, color = borderAccent)
                }
            }

            if (order.handledByStaffId.isNotBlank()) {
                Text(
                    text = "Staff ID: ${order.handledByStaffId} · ${order.itemCount} total items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = EmberBorder)

            // Itemized Dishes — Quantities & Dish/Variety Names ONLY (zero prices!)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lineItems.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(SmokedAmber.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${line.qty}x",
                                style = MonoPriceStyle,
                                color = SmokedAmber
                            )
                        }
                        Text(
                            text = line.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (order.notes.isNotBlank()) {
                Surface(
                    color = TandoorCopper.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kitchen Note: ${order.notes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmokedAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (status) {
                    OrderStatus.PLACED, OrderStatus.CONFIRMED -> {
                        Button(
                            onClick = onStartGrilling,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kot_start_grill_${order.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TandoorCopper, contentColor = CharcoalDeep)
                        ) {
                            Text("Start Grilling", fontWeight = FontWeight.Bold)
                        }
                    }
                    OrderStatus.GRILLING -> {
                        Button(
                            onClick = onMarkReady,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kot_mark_ready_${order.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CorianderGreen, contentColor = CharcoalDeep)
                        ) {
                            Text("Mark Ready", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        StatusPill(label = "Ready for Pickup / Serve", color = CorianderGreen)
                        Spacer(Modifier.weight(1f))
                    }
                }

                OutlinedButton(
                    onClick = onPrintKot,
                    border = BorderStroke(1.dp, SmokedAmber),
                    modifier = Modifier.testTag("kot_print_btn_${order.id}")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = "Print KOT ${order.orderNumber}", tint = SmokedAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Print KOT", color = SmokedAmber)
                }
            }
        }
    }
}

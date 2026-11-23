package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
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
import com.example.data.CartLine
import com.example.data.MenuCategory
import com.example.data.OakAndSmokeSeeds
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.UserRoleTier
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.data.parsedLineItems
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

    val activeTables = remember(diningTables) {
        val liveActive = diningTables.filter { it.available }
        if (liveActive.isNotEmpty()) liveActive else OakAndSmokeSeeds.starterTables
    }
    val canManageTables = effectiveRole == UserRoleTier.OWNER || effectiveRole == UserRoleTier.MANAGER

    var orderType by remember { mutableStateOf(OrderType.DINE_IN) }
    var tableLabel by remember { mutableStateOf(activeTables.firstOrNull()?.label ?: "Table 1") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var kitchenNotes by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MenuCategory?>(null) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var createdOrderForPrint by remember { mutableStateOf<OrderRecord?>(null) }
    var showQuickAddTableDialog by remember { mutableStateOf(false) }

    val availableMenu = remember(menuItems, searchQuery, selectedCategory) {
        menuItems.filter { dish ->
            dish.available &&
                (selectedCategory == null || dish.category == selectedCategory?.name) &&
                (searchQuery.isBlank() || dish.name.contains(searchQuery.trim(), ignoreCase = true))
        }
    }

    val selectedLines = remember(menuItems, quantities) {
        menuItems.mapNotNull { dish ->
            val q = quantities[dish.id] ?: 0
            if (q > 0 && dish.available) CartLine(dish, q) else null
        }
    }
    val totalCount = selectedLines.sumOf { it.quantity }
    val totalAmountPkr = selectedLines.sumOf { it.item.pricePkr * it.quantity }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Take Order (Direct to Kitchen)",
                    subtitle = "Create Dine-In, Takeaway, or Phone COD orders and fire immediately to Kitchen"
                )
            }

            item {
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
                                    onClick = { orderType = type },
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
                                Text("Table / Cabin (${activeTables.size} active)", style = MaterialTheme.typography.labelLarge)
                                if (canManageTables) {
                                    TextButton(
                                        onClick = { showQuickAddTableDialog = true },
                                        modifier = Modifier.testTag("pos_quick_add_table_btn")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add Table", tint = SmokedAmber, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Table", color = SmokedAmber)
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                activeTables.forEach { tbl ->
                                    FilterChip(
                                        selected = tableLabel == tbl.label,
                                        onClick = { tableLabel = tbl.label },
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
                                onValueChange = { tableLabel = it },
                                label = { Text("Custom Table / Cabin Label") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_table_input")
                            )
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name (optional for Dine-In/Takeaway)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_customer_name_input")
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
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
                                onValueChange = { deliveryAddress = it },
                                label = { Text("Hasilpur Delivery Address (required)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_delivery_address_input")
                            )
                        }

                        OutlinedTextField(
                            value = kitchenNotes,
                            onValueChange = { kitchenNotes = it },
                            label = { Text("Kitchen Notes (spice level, allergies, serve order)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_kitchen_notes_input")
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Select Dishes",
                    subtitle = if (totalCount > 0) "$totalCount items selected · ${formatPkr(totalAmountPkr)}" else "Tap + to add dishes to order"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Karahi, BBQ, Platter, Naan...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_menu_search_input")
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                        MenuCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
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
                    val qty = quantities[dish.id] ?: 0
                    SmokedCard(
                        borderColor = if (qty > 0) SmokedAmber else MaterialTheme.colorScheme.outline
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(dish.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${formatPkr(dish.pricePkr)} · ${MenuCategory.fromRaw(dish.category).displayLabel}",
                                    style = MonoPriceStyle,
                                    color = SmokedAmber
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (qty > 0) {
                                    IconButton(
                                        onClick = {
                                            quantities = quantities.toMutableMap().apply {
                                                val next = qty - 1
                                                if (next <= 0) remove(dish.id) else put(dish.id, next)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("staff_pos_dec_${dish.id}")
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Remove ${dish.name}", tint = SmokedAmber)
                                    }
                                    Text("$qty", style = MonoPriceStyle)
                                }
                                IconButton(
                                    onClick = {
                                        quantities = quantities.toMutableMap().apply {
                                            put(dish.id, qty + 1)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("staff_pos_inc_${dish.id}")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add ${dish.name}", tint = CorianderGreen)
                                }
                            }
                        }
                    }
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

                val canSend = !isBusy &&
                    selectedLines.isNotEmpty() &&
                    (orderType != OrderType.DINE_IN || tableLabel.trim().isNotEmpty()) &&
                    (orderType != OrderType.COD_DELIVERY || (customerPhone.trim().length >= 7 && deliveryAddress.trim().length >= 4))

                Button(
                    onClick = {
                        viewModel.createStaffOrderDirectToKitchen(
                            orderType = orderType,
                            tableLabel = if (orderType == OrderType.DINE_IN) tableLabel else "",
                            customerName = customerName,
                            customerPhone = customerPhone,
                            deliveryAddress = if (orderType == OrderType.COD_DELIVERY) deliveryAddress else "",
                            cartLines = selectedLines,
                            notes = kitchenNotes
                        ) { created ->
                            createdOrderForPrint = created
                            quantities = emptyMap()
                            customerName = ""
                            customerPhone = ""
                            deliveryAddress = ""
                            kitchenNotes = ""
                        }
                    },
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
                    Text("Send Direct to Kitchen", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Post-Send Print Dialog (Instant Print KOT or Print Full Customer Bill)
    createdOrderForPrint?.let { order ->
        AlertDialog(
            onDismissRequest = { createdOrderForPrint = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CorianderGreen)
                    Text("Sent to Kitchen: ${order.orderNumber}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        text = "Choose Kitchen Ticket (order details only, no prices) or Full Customer Bill (all itemized details):",
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
                TextButton(onClick = { createdOrderForPrint = null }) {
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

/**
 * Kitchen Display (KOT): STRICTLY displays order preparation details ONLY.
 * No prices, no totals, no discounts, and no customer phone/billing address are shown here.
 */
@Composable
fun KitchenDisplayTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()

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
                    subtitle = "Orders taken by staff or placed by customers appear here with item quantities and preparation notes only."
                )
            }
        } else {
            items(kitchenOrders, key = { it.id }) { order ->
                KitchenTicketCard(
                    order = order,
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
                Column {
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
                StatusPill(label = status.displayLabel, color = borderAccent)
            }

            if (order.handledByStaffId.isNotBlank()) {
                Text(
                    text = "Staff ID: ${order.handledByStaffId} · ${order.itemCount} total items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = EmberBorder)

            // Itemized Dishes — Quantities & Dish Names ONLY (zero prices!)
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

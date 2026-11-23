package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
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
import com.example.data.FeedbackCategory
import com.example.data.FeedbackEntry
import com.example.data.InventoryCategory
import com.example.data.InventoryItem
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.ReservationRecord
import com.example.data.ReservationStatus
import com.example.data.SeatingZone
import com.example.data.formatCoordinates
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.services.LocationHelper
import com.example.services.OrderPrintService
import com.example.ui.components.EmptyStateCard
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
fun StaffOperationsScreen(viewModel: RestaurantViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Take Order",
        "Kitchen (KOT)",
        "Orders & Bills",
        "Reservations",
        "Inventory",
        "My Shift & Reviews"
    )

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
                    modifier = Modifier.testTag("staff_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> StaffTakeOrderTab(viewModel)
            1 -> KitchenDisplayTab(viewModel)
            2 -> StaffOrdersBoardTab(viewModel)
            3 -> StaffReservationsDeskTab(viewModel)
            4 -> StaffInventoryTab(viewModel)
            else -> StaffProfileFeedbackTab(viewModel)
        }
    }
}

@Composable
private fun StaffOrdersBoardTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("ACTIVE") }

    val filtered = remember(allOrders, filter) {
        when (filter) {
            "ACTIVE" -> allOrders.filter { it.status != OrderStatus.DELIVERED.name && it.status != OrderStatus.CANCELLED.name }
            "DELIVERED" -> allOrders.filter { it.status == OrderStatus.DELIVERED.name }
            else -> allOrders
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Orders & Billing Counter",
                subtitle = "${allOrders.count { it.status != OrderStatus.DELIVERED.name && it.status != OrderStatus.CANCELLED.name }} active · Print KOT or Full Customer Bill anytime"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ACTIVE" to "Active", "DELIVERED" to "Completed", "ALL" to "All").forEach { (k, label) ->
                    FilterChip(
                        selected = filter == k,
                        onClick = { filter = k },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmokedAmber, selectedLabelColor = CharcoalDeep)
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocalShipping,
                    title = "No orders in this queue",
                    subtitle = "Orders taken by staff or placed by customers appear here with KOT and Full Bill print options."
                )
            }
        } else {
            items(filtered, key = { it.id }) { order ->
                StaffOrderActionCard(
                    order = order,
                    onAdvance = { next -> viewModel.advanceOrderStatus(order, next) },
                    onOpenMap = {
                        LocationHelper.openNavigationToLocation(
                            context,
                            order.customerLat,
                            order.customerLng,
                            order.locationLabel.ifBlank { order.deliveryAddress }
                        ).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to launch Maps")
                        }
                    },
                    onPrintKot = {
                        OrderPrintService.printKitchenTicket(context, order).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to print KOT")
                        }
                    },
                    onPrintBill = {
                        OrderPrintService.printCustomerBill(context, order).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to print Customer Bill")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StaffOrderActionCard(
    order: OrderRecord,
    onAdvance: (OrderStatus) -> Unit,
    onOpenMap: () -> Unit,
    onPrintKot: () -> Unit,
    onPrintBill: () -> Unit
) {
    val status = OrderStatus.fromRaw(order.status)
    val orderType = OrderType.fromRaw(order.orderType)
    val next = status.nextStaffStatus(orderType)
    val pillColor = when (status) {
        OrderStatus.DELIVERED, OrderStatus.READY -> CorianderGreen
        OrderStatus.CANCELLED -> CrimsonEmber
        OrderStatus.GRILLING, OrderStatus.OUT_FOR_DELIVERY -> TandoorCopper
        else -> SmokedAmber
    }
    val typeSummary = if (order.tableLabel.isNotBlank()) {
        "${orderType.displayLabel} · ${order.tableLabel}"
    } else {
        orderType.displayLabel
    }
    val payLabel = if (order.paymentMethod == "CASH_COUNTER") "Cash at Counter" else "COD"

    SmokedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${order.orderNumber} · ${order.customerName}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "$typeSummary · ${formatTimestampShort(order.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = status.displayLabel, color = pillColor)
            }

            Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
            val contactLine = listOfNotNull(
                "$payLabel: ${formatPkr(order.totalPkr)}",
                order.deliveryAddress.takeIf { it.isNotBlank() },
                order.customerPhone.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            Text(
                text = contactLine,
                style = MaterialTheme.typography.bodySmall,
                color = SmokedAmber
            )
            if (order.discountPkr > 0L) {
                Text(
                    text = "Subtotal ${formatPkr(order.subtotalPkr)} · Loyalty Discount -${formatPkr(order.discountPkr)} (${order.loyaltyPointsRedeemed} pts)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CorianderGreen
                )
            }
            if (order.notes.isNotBlank()) {
                Text("Note: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (order.hasLocation) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "GPS Pin", tint = CorianderGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "GPS: ${formatCoordinates(order.customerLat, order.customerLng)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CorianderGreen
                        )
                    }
                    TextButton(onClick = onOpenMap) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, tint = CorianderGreen, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Map", color = CorianderGreen)
                    }
                }
            }
            if (order.assignedRiderName.isNotBlank()) {
                Text(
                    text = "Rider: ${order.assignedRiderName} (${order.assignedRiderId})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SmokedAmber
                )
            }

            // Print KOT (Kitchen Only) & Print Full Customer Bill (All Details)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPrintKot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_print_kot_${order.id}"),
                    border = BorderStroke(1.dp, TandoorCopper)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = TandoorCopper, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Print KOT", color = TandoorCopper)
                }
                OutlinedButton(
                    onClick = onPrintBill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_print_bill_${order.id}"),
                    border = BorderStroke(1.dp, SmokedAmber)
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Print Bill", color = SmokedAmber)
                }
            }

            if (next != null) {
                val advanceLabel = if (next == OrderStatus.READY && orderType == OrderType.COD_DELIVERY) {
                    "Ready & Alert Rider + Guest"
                } else {
                    "Move to ${next.displayLabel}"
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAdvance(next) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("advance_order_${order.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Text(advanceLabel, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAdvance(OrderStatus.CANCELLED) },
                        border = BorderStroke(1.dp, CrimsonEmber)
                    ) {
                        Text("Cancel", color = CrimsonEmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffReservationsDeskTab(viewModel: RestaurantViewModel) {
    val reservations by viewModel.allReservations.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Table Reservations Desk",
                subtitle = "${reservations.size} total bookings"
            )
        }
        if (reservations.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.TableRestaurant,
                    title = "No table bookings yet",
                    subtitle = "Incoming guest reservations for Charcoal Hall, Family Cabins, and Lawn appear here."
                )
            }
        } else {
            items(reservations, key = { it.id }) { booking ->
                StaffReservationCard(
                    booking = booking,
                    onUpdateStatus = { st -> viewModel.updateReservationStatus(booking, st) }
                )
            }
        }
    }
}

@Composable
private fun StaffReservationCard(
    booking: ReservationRecord,
    onUpdateStatus: (ReservationStatus) -> Unit
) {
    val status = ReservationStatus.fromRaw(booking.status)
    val badgeColor = when (status) {
        ReservationStatus.CONFIRMED, ReservationStatus.SEATED, ReservationStatus.COMPLETED -> CorianderGreen
        ReservationStatus.CANCELLED -> CrimsonEmber
        else -> SmokedAmber
    }

    SmokedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${booking.customerName} (${booking.partySize} Guests)", style = MaterialTheme.typography.titleSmall)
                StatusPill(label = status.displayLabel, color = badgeColor)
            }
            Text(
                text = "${booking.dateLabel} · ${booking.timeSlot} · ${SeatingZone.fromRaw(booking.seatingZone).displayLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = SmokedAmber
            )
            Text("Phone: ${booking.customerPhone}", style = MaterialTheme.typography.bodySmall)
            if (booking.specialNotes.isNotBlank()) {
                Text("Notes: ${booking.specialNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (status != ReservationStatus.COMPLETED && status != ReservationStatus.CANCELLED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val nextStatus = when (status) {
                        ReservationStatus.PENDING -> ReservationStatus.CONFIRMED
                        ReservationStatus.CONFIRMED -> ReservationStatus.SEATED
                        else -> ReservationStatus.COMPLETED
                    }
                    Button(
                        onClick = { onUpdateStatus(nextStatus) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Text("Mark ${nextStatus.displayLabel}", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus(ReservationStatus.CANCELLED) },
                        border = BorderStroke(1.dp, CrimsonEmber)
                    ) {
                        Text("Cancel", color = CrimsonEmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffInventoryTab(viewModel: RestaurantViewModel) {
    val inventory by viewModel.inventoryItems.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val lowCount = inventory.count { it.isLowStock() }
            SectionHeader(
                title = "Kitchen & Grill Inventory",
                subtitle = if (lowCount > 0) "$lowCount items running low — restock needed" else "All stock levels healthy"
            )
        }
        if (inventory.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Inventory2,
                    title = "No inventory tracked yet",
                    subtitle = "Owner can add meat, oak charcoal, spices, and packaging stock items from the Owner Portal."
                )
            }
        } else {
            items(inventory, key = { it.id }) { item ->
                InventoryControlCard(
                    item = item,
                    onAdjust = { delta -> viewModel.adjustInventory(item, delta) }
                )
            }
        }
    }
}

@Composable
fun InventoryControlCard(
    item: InventoryItem,
    onAdjust: (Long) -> Unit
) {
    val low = item.isLowStock()
    SmokedCard(borderColor = if (low) CrimsonEmber else MaterialTheme.colorScheme.outline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                    if (low) StatusPill(label = "LOW", color = CrimsonEmber)
                }
                Text(
                    text = "${InventoryCategory.fromRaw(item.category).displayLabel} · Min ${item.minThreshold} ${item.unit} · ${formatPkr(item.unitCostPkr)}/${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAdjust(-1L) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease ${item.name}", tint = CrimsonEmber)
                }
                Text(
                    text = "${item.quantity} ${item.unit}",
                    style = MonoPriceStyle,
                    color = if (low) CrimsonEmber else SmokedAmber
                )
                IconButton(onClick = { onAdjust(1L) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase ${item.name}", tint = CorianderGreen)
                }
            }
        }
    }
}

@Composable
private fun StaffProfileFeedbackTab(viewModel: RestaurantViewModel) {
    val ownStaff by viewModel.ownStaffRecord.collectAsStateWithLifecycle()
    val feedback by viewModel.allFeedback.collectAsStateWithLifecycle()
    var replyingTo by remember { mutableStateOf<FeedbackEntry?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(title = "My Schedule & Performance")
        }
        item {
            if (ownStaff != null) {
                val member = ownStaff!!
                SmokedCard(borderColor = SmokedAmber) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${member.fullName} (${member.staffId})", style = MaterialTheme.typography.titleMedium)
                            StatusPill(label = member.roleTier, color = SmokedAmber)
                        }
                        Text("Shift: ${member.shiftSummary}", style = MaterialTheme.typography.bodyMedium)
                        Text("Days: ${member.scheduleDays}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Orders Handled: ${member.ordersHandledCount}", style = MonoPriceStyle, color = CorianderGreen)
                            Text("Loyalty: ${member.loyaltyPoints} pts", style = MonoPriceStyle, color = SmokedAmber)
                        }
                        Text(
                            text = "Salary: ${formatPkr(member.monthlySalaryPkr)} (${member.salaryStatus})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                EmptyStateCard(
                    icon = Icons.Filled.Badge,
                    title = "Staff Record Linked on Sign-In",
                    subtitle = "Sign in with your Owner-issued Staff ID & Passcode in Profile to view your personal shift schedule, loyalty balance, and salary status."
                )
            }
        }

        item {
            SectionHeader(title = "Customer Feedback Desk", subtitle = "Respond to guest reviews")
        }
        if (feedback.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Reviews,
                    title = "No customer feedback yet",
                    subtitle = "Submitted guest reviews will appear here."
                )
            }
        } else {
            items(feedback, key = { it.id }) { fb ->
                SmokedCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${fb.customerName} · ${FeedbackCategory.fromRaw(fb.category).displayLabel}", style = MaterialTheme.typography.titleSmall)
                            Text("${fb.rating} ★", style = MonoPriceStyle, color = SmokedAmber)
                        }
                        Text(fb.comment, style = MaterialTheme.typography.bodyMedium)
                        if (fb.staffReply.isNotBlank()) {
                            Text("Reply: ${fb.staffReply}", style = MaterialTheme.typography.bodySmall, color = CorianderGreen)
                        }
                        TextButton(onClick = { replyingTo = fb }) {
                            Text(if (fb.staffReply.isBlank()) "Reply to Guest" else "Edit Reply", color = SmokedAmber)
                        }
                    }
                }
            }
        }
    }

    replyingTo?.let { target ->
        var replyText by remember(target.id) { mutableStateOf(target.staffReply) }
        AlertDialog(
            onDismissRequest = { replyingTo = null },
            title = { Text("Reply to ${target.customerName}") },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Management Response") },
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.replyToFeedback(target, replyText)
                        replyingTo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    Text("Send Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingTo = null }) { Text("Cancel") }
            }
        )
    }
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.StaffMember
import com.example.data.UserRoleTier
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
fun OwnerRidersTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val riders by viewModel.allRiders.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()

    var showCreateRider by remember { mutableStateOf(false) }
    var editingRider by remember { mutableStateOf<StaffMember?>(null) }
    var assigningOrder by remember { mutableStateOf<OrderRecord?>(null) }

    val activeCodOrders = remember(allOrders) {
        allOrders.filter {
            it.orderType == OrderType.COD_DELIVERY.name &&
                it.status != OrderStatus.DELIVERED.name &&
                it.status != OrderStatus.CANCELLED.name
        }
    }
    val activeRiders = remember(riders) { riders.filter { it.active } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Delivery Riders Fleet",
                subtitle = "${activeRiders.size} active riders · Manage Rider IDs, Bikes & GPS Dispatch",
                trailing = {
                    if (effectiveRole == UserRoleTier.OWNER) {
                        Button(
                            onClick = { showCreateRider = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                            modifier = Modifier.testTag("add_rider_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Rider", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rider")
                        }
                    }
                }
            )
        }

        if (riders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.TwoWheeler,
                    title = "No delivery riders added yet",
                    subtitle = "Tap + Rider to create Rider IDs & Passcodes (e.g., RD-101). Riders sign in to receive Order Ready sound alerts and navigate to customer GPS pins."
                )
            }
        } else {
            items(riders, key = { it.id }) { rider ->
                SmokedCard(borderColor = if (rider.active) SmokedAmber.copy(alpha = 0.5f) else CrimsonEmber) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeliveryDining,
                                    contentDescription = "Rider ${rider.fullName}",
                                    tint = SmokedAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text("${rider.fullName} · ${rider.staffId}", style = MaterialTheme.typography.titleSmall)
                                    val vehicleLine = listOfNotNull(
                                        rider.vehicleInfo.takeIf { it.isNotBlank() },
                                        rider.phone.takeIf { it.isNotBlank() }
                                    ).joinToString(" · ")
                                    Text(
                                        text = vehicleLine.ifBlank { "${rider.shiftSummary} (${rider.scheduleDays})" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusPill(
                                label = if (rider.active) "ACTIVE" else "OFFLINE",
                                color = if (rider.active) CorianderGreen else CrimsonEmber
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Deliveries: ${rider.ordersHandledCount} · Salary: ${formatPkr(rider.monthlySalaryPkr)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SmokedAmber
                            )
                            Row {
                                if (rider.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            LocationHelper.dialPhoneNumber(context, rider.phone).onFailure {
                                                viewModel.showMessage(it.message ?: "Cannot open dialer")
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Call, contentDescription = "Call ${rider.fullName}", tint = CorianderGreen)
                                    }
                                }
                                if (effectiveRole == UserRoleTier.OWNER) {
                                    IconButton(
                                        onClick = { editingRider = rider },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${rider.fullName}", tint = SmokedAmber)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(
                title = "Live COD Dispatch & Customer GPS",
                subtitle = "${activeCodOrders.size} active delivery orders · Mark Ready to trigger Rider + Customer sound alerts"
            )
        }

        if (activeCodOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocationOn,
                    title = "No active COD deliveries right now",
                    subtitle = "Incoming Cash on Delivery orders with pinned customer GPS coordinates will appear here for instant Rider dispatch."
                )
            }
        } else {
            items(activeCodOrders, key = { "dispatch_${it.id}" }) { order ->
                AdminCodDispatchCard(
                    order = order,
                    hasRiders = activeRiders.isNotEmpty(),
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
                    onAssignRider = { assigningOrder = order },
                    onMarkReadyAndAlert = { viewModel.advanceOrderStatus(order, OrderStatus.READY) },
                    onPrintBill = {
                        OrderPrintService.printCustomerBill(context, order).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to print bill")
                        }
                    }
                )
            }
        }
    }

    if (showCreateRider || editingRider != null) {
        RiderEditorDialog(
            existing = editingRider,
            onDismiss = {
                showCreateRider = false
                editingRider = null
            },
            onSave = { riderId, name, pass, phone, vehicle, salary, shift, days, active ->
                viewModel.saveStaffMember(
                    existing = editingRider,
                    staffId = riderId,
                    fullName = name,
                    newPasscode = pass,
                    roleTier = UserRoleTier.RIDER,
                    monthlySalaryPkr = salary,
                    shiftSummary = shift,
                    scheduleDays = days,
                    loyaltyPoints = editingRider?.loyaltyPoints ?: 0L,
                    active = active,
                    phone = phone,
                    vehicleInfo = vehicle
                )
                showCreateRider = false
                editingRider = null
            }
        )
    }

    assigningOrder?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { assigningOrder = null },
            title = { Text("Assign Rider · ${targetOrder.orderNumber}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select an active Rider to dispatch. Both the Rider and ${targetOrder.customerName} will receive an immediate sound alert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeRiders.isEmpty()) {
                        Text(
                            text = "No active Riders available. Add a Rider first above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonEmber
                        )
                    } else {
                        activeRiders.forEach { r ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.assignRiderToOrder(targetOrder, r)
                                    assigningOrder = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_rider_${r.id}"),
                                border = BorderStroke(1.dp, SmokedAmber)
                            ) {
                                Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${r.fullName} (${r.staffId})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    val sub = listOfNotNull(
                                        r.vehicleInfo.takeIf { it.isNotBlank() },
                                        r.phone.takeIf { it.isNotBlank() }
                                    ).joinToString(" · ")
                                    if (sub.isNotBlank()) {
                                        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { assigningOrder = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AdminCodDispatchCard(
    order: OrderRecord,
    hasRiders: Boolean,
    onOpenMap: () -> Unit,
    onAssignRider: () -> Unit,
    onMarkReadyAndAlert: () -> Unit,
    onPrintBill: () -> Unit
) {
    val status = OrderStatus.fromRaw(order.status)
    val badgeColor = when (status) {
        OrderStatus.READY -> CorianderGreen
        OrderStatus.OUT_FOR_DELIVERY -> TandoorCopper
        else -> SmokedAmber
    }

    SmokedCard(borderColor = if (status == OrderStatus.READY) CorianderGreen else MaterialTheme.colorScheme.outline) {
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
                        text = "${formatTimestampShort(order.createdAt)} · ${order.customerPhone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = status.displayLabel, color = badgeColor)
            }

            Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "COD: ${formatPkr(order.totalPkr)} · ${order.deliveryAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = SmokedAmber
            )

            // GPS Pin Row
            Surface(
                color = if (order.hasLocation) CorianderGreen.copy(alpha = 0.12f) else CrimsonEmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (order.hasLocation) CorianderGreen.copy(alpha = 0.5f) else CrimsonEmber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Customer GPS",
                            tint = if (order.hasLocation) CorianderGreen else CrimsonEmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (order.hasLocation) "Customer GPS Attached" else "Manual Phone Order (No GPS Pin)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (order.hasLocation) CorianderGreen else CrimsonEmber
                            )
                            if (order.hasLocation) {
                                Text(
                                    text = order.locationLabel.ifBlank { formatCoordinates(order.customerLat, order.customerLng) },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (order.hasLocation) {
                        OutlinedButton(
                            onClick = onOpenMap,
                            border = BorderStroke(1.dp, CorianderGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = CorianderGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Map", color = CorianderGreen)
                        }
                    }
                }
            }

            if (order.assignedRiderName.isNotBlank()) {
                Text(
                    text = "Assigned Rider: ${order.assignedRiderName} (${order.assignedRiderId})${if (order.assignedRiderPhone.isNotBlank()) " · ${order.assignedRiderPhone}" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CorianderGreen
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status != OrderStatus.READY && status != OrderStatus.OUT_FOR_DELIVERY) {
                    Button(
                        onClick = onMarkReadyAndAlert,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_ready_alert_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CorianderGreen, contentColor = CharcoalDeep),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ready & Alert", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onAssignRider,
                    enabled = hasRiders,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assign_rider_btn_${order.id}"),
                    border = BorderStroke(1.dp, SmokedAmber)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (order.assignedRiderId.isBlank()) "Assign Rider" else "Change Rider", color = SmokedAmber)
                }
                IconButton(
                    onClick = onPrintBill,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = "Print Bill", tint = SmokedAmber)
                }
            }
        }
    }
}

@Composable
private fun RiderEditorDialog(
    existing: StaffMember?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Long, String, String, Boolean) -> Unit
) {
    var riderId by remember { mutableStateOf(existing?.staffId ?: "RD-101") }
    var fullName by remember { mutableStateOf(existing?.fullName.orEmpty()) }
    var passcode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var vehicleInfo by remember { mutableStateOf(existing?.vehicleInfo ?: "Honda CD-70") }
    var salaryText by remember { mutableStateOf((existing?.monthlySalaryPkr ?: 28000L).toString()) }
    var shiftSummary by remember { mutableStateOf(existing?.shiftSummary ?: "Evening 5 PM - 1 AM") }
    var scheduleDays by remember { mutableStateOf(existing?.scheduleDays ?: "Mon - Sun") }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Delivery Rider" else "Edit Delivery Rider") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = riderId,
                    onValueChange = { riderId = it; errorText = null },
                    label = { Text("Rider ID (e.g. RD-101)") },
                    singleLine = true,
                    modifier = Modifier.testTag("rider_id_input")
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorText = null },
                    label = { Text("Rider Full Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("rider_name_input")
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it; errorText = null },
                    label = { Text(if (existing == null) "Rider Passcode (min 4 chars)" else "New Passcode (blank to keep)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.testTag("rider_passcode_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Rider Mobile Number (e.g. 0300-1234567)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = vehicleInfo,
                    onValueChange = { vehicleInfo = it },
                    label = { Text("Bike / Vehicle (e.g. CD-70 BWL-4521)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it.filter { c -> c.isDigit() } },
                    label = { Text("Monthly Salary (PKR)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = shiftSummary,
                    onValueChange = { shiftSummary = it },
                    label = { Text("Shift Hours") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = scheduleDays,
                    onValueChange = { scheduleDays = it },
                    label = { Text("Work Days") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active Rider Access")
                }
                if (errorText != null) {
                    Text(errorText!!, style = MaterialTheme.typography.bodySmall, color = CrimsonEmber)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sal = salaryText.toLongOrNull() ?: 0L
                    val cleanId = riderId.trim().uppercase()
                    val changedId = existing != null && !cleanId.equals(existing.staffId, ignoreCase = true)
                    when {
                        cleanId.length < 2 -> errorText = "Enter a valid Rider ID (e.g., RD-101)."
                        fullName.trim().length < 2 -> errorText = "Enter the rider's full name."
                        existing == null && passcode.trim().length < 4 -> errorText = "Passcode must be at least 4 characters."
                        changedId && passcode.trim().length < 4 -> errorText = "Enter a new passcode (min 4 chars) when changing Rider ID."
                        else -> onSave(riderId, fullName, passcode, phone, vehicleInfo, sal, shiftSummary, scheduleDays, active)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                modifier = Modifier.testTag("save_rider_confirm_btn")
            ) {
                Text("Save Rider", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

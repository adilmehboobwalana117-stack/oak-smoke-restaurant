package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.formatCoordinates
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.services.LocalNotificationHelper
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
fun RiderPortalScreen(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val ownRider by viewModel.ownStaffRecord.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("READY") }

    val codOrders = remember(allOrders) {
        allOrders.filter { it.orderType == OrderType.COD_DELIVERY.name }
    }
    val readyCount = remember(codOrders) { codOrders.count { it.status == OrderStatus.READY.name } }
    val onWayCount = remember(codOrders) { codOrders.count { it.status == OrderStatus.OUT_FOR_DELIVERY.name } }

    val filtered = remember(codOrders, filter) {
        when (filter) {
            "READY" -> codOrders.filter { it.status == OrderStatus.READY.name }
            "ON_WAY" -> codOrders.filter { it.status == OrderStatus.OUT_FOR_DELIVERY.name }
            "COMPLETED" -> codOrders.filter { it.status == OrderStatus.DELIVERED.name }
            else -> codOrders.filter {
                it.status != OrderStatus.DELIVERED.name && it.status != OrderStatus.CANCELLED.name
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.65f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = "Rider Portal",
                            tint = SmokedAmber,
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                text = ownRider?.let { "${it.fullName} (${it.staffId})" } ?: "Oak & Smoke Rider Dispatch",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$readyCount ready for pickup · $onWayCount out for delivery",
                                style = MaterialTheme.typography.bodySmall,
                                color = CorianderGreen
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            LocalNotificationHelper.playAlertSound(context, isOrderReady = true)
                            viewModel.showMessage("Order Ready sound alert active!")
                        },
                        border = BorderStroke(1.dp, SmokedAmber),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("rider_test_sound_btn")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = "Test Sound Alert", tint = SmokedAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sound", color = SmokedAmber)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Rider COD Deliveries",
                subtitle = "Fetch customer's pinned GPS location & navigate directly"
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "READY" to "Ready ($readyCount)",
                    "ON_WAY" to "On Way ($onWayCount)",
                    "COMPLETED" to "Delivered"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmokedAmber,
                            selectedLabelColor = CharcoalDeep
                        ),
                        modifier = Modifier.testTag("rider_filter_${key.lowercase()}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.DeliveryDining,
                    title = when (filter) {
                        "READY" -> "No orders waiting for pickup"
                        "ON_WAY" -> "No deliveries in progress"
                        else -> "No completed deliveries yet"
                    },
                    subtitle = "When the Kitchen marks a COD order READY, you will receive an instant sound alert and the customer's GPS pin will appear here."
                )
            }
        } else {
            items(filtered, key = { it.id }) { order ->
                RiderDeliveryCard(
                    order = order,
                    onFetchLocation = {
                        LocationHelper.openNavigationToLocation(
                            context = context,
                            lat = order.customerLat,
                            lng = order.customerLng,
                            label = order.locationLabel.ifBlank { order.deliveryAddress }
                        ).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to open Google Maps")
                        }
                    },
                    onCallCustomer = {
                        LocationHelper.dialPhoneNumber(context, order.customerPhone).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to open phone dialer")
                        }
                    },
                    onStartDelivery = { viewModel.riderClaimAndStartDelivery(order) },
                    onMarkDelivered = { viewModel.advanceOrderStatus(order, OrderStatus.DELIVERED) },
                    onPrintBill = {
                        OrderPrintService.printCustomerBill(context, order).onFailure {
                            viewModel.showMessage(it.message ?: "Unable to print bill")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RiderDeliveryCard(
    order: OrderRecord,
    onFetchLocation: () -> Unit,
    onCallCustomer: () -> Unit,
    onStartDelivery: () -> Unit,
    onMarkDelivered: () -> Unit,
    onPrintBill: () -> Unit
) {
    val status = OrderStatus.fromRaw(order.status)
    val badgeColor = when (status) {
        OrderStatus.READY -> CorianderGreen
        OrderStatus.OUT_FOR_DELIVERY -> TandoorCopper
        OrderStatus.DELIVERED -> CorianderGreen
        else -> SmokedAmber
    }

    SmokedCard(borderColor = if (status == OrderStatus.READY) CorianderGreen else SmokedAmber.copy(alpha = 0.5f)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${order.orderNumber} · ${order.customerName}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatTimestampShort(order.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = status.displayLabel, color = badgeColor)
            }

            Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Collect Cash on Delivery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPkr(order.totalPkr), style = MonoPriceStyle, color = SmokedAmber)
                }
                if (order.assignedRiderName.isNotBlank()) {
                    StatusPill(label = "Rider: ${order.assignedRiderName}", color = SmokedAmber)
                }
            }

            // Prominent Customer GPS Location Fetch Card
            Surface(
                color = if (order.hasLocation) CorianderGreen.copy(alpha = 0.14f) else CrimsonEmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (order.hasLocation) CorianderGreen.copy(alpha = 0.65f) else CrimsonEmber.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Customer Pinned Location",
                            tint = if (order.hasLocation) CorianderGreen else CrimsonEmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (order.hasLocation) "Customer Live GPS Pinned" else "Address Only (No GPS Attached)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (order.hasLocation) CorianderGreen else CrimsonEmber
                        )
                    }
                    Text(
                        text = order.deliveryAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (order.hasLocation) {
                        Text(
                            text = "Coordinates: ${formatCoordinates(order.customerLat, order.customerLng)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onFetchLocation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rider_fetch_location_${order.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = CorianderGreen, contentColor = CharcoalDeep),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Fetch Customer Location (Open Maps)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (order.notes.isNotBlank()) {
                Text("Customer Note: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallCustomer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rider_call_${order.id}"),
                    border = BorderStroke(1.dp, CorianderGreen)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = CorianderGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call", color = CorianderGreen)
                }
                OutlinedButton(
                    onClick = onPrintBill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rider_print_bill_${order.id}"),
                    border = BorderStroke(1.dp, SmokedAmber)
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Print Bill", color = SmokedAmber)
                }
            }

            when (status) {
                OrderStatus.READY -> {
                    Button(
                        onClick = onStartDelivery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_start_delivery_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Up & Start Delivery", fontWeight = FontWeight.Bold)
                    }
                }
                OrderStatus.OUT_FOR_DELIVERY -> {
                    Button(
                        onClick = onMarkDelivered,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_mark_delivered_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CorianderGreen, contentColor = CharcoalDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark Delivered (COD Collected)", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }
    }
}

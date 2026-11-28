package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FeedbackCategory
import com.example.data.FeedbackEntry
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.ReservationRecord
import com.example.data.ReservationStatus
import com.example.data.SeatingZone
import com.example.data.formatCoordinates
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.services.OrderPrintService
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import com.example.viewmodel.RestaurantViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CustomerOrdersReservationsScreen(
    viewModel: RestaurantViewModel,
    initialTab: Int = 0
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Live Orders", "Reserve Table", "Loyalty & Reviews")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SmokedAmber
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("customer_hub_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> CustomerOrdersTab(viewModel)
            1 -> CustomerReservationsTab(viewModel)
            else -> CustomerLoyaltyFeedbackTab(viewModel)
        }
    }
}

@Composable
private fun CustomerOrdersTab(viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val orders by viewModel.customerOrders.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Live Order Tracking & Bills",
                subtitle = "Real-time kitchen & delivery status · Print itemized bill anytime"
            )
        }
        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No orders yet",
                    subtitle = "Explore Oak & Smoke's Mutton Karahi and BBQ Platters in Menu to place your first Cash on Delivery order."
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
                CustomerOrderCard(
                    order = order,
                    onCancel = { viewModel.cancelOwnOrder(order.id) },
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
private fun CustomerOrderCard(
    order: OrderRecord,
    onCancel: () -> Unit,
    onPrintBill: () -> Unit
) {
    val status = OrderStatus.fromRaw(order.status)
    val orderType = OrderType.fromRaw(order.orderType)
    val targetBadgeColor = when (status) {
        OrderStatus.DELIVERED, OrderStatus.READY -> CorianderGreen
        OrderStatus.CANCELLED -> CrimsonEmber
        OrderStatus.GRILLING, OrderStatus.OUT_FOR_DELIVERY -> TandoorCopper
        else -> SmokedAmber
    }
    val badgeColor by animateColorAsState(
        targetValue = targetBadgeColor,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "order_status_badge_color"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (status == OrderStatus.GRILLING || status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.READY) {
            targetBadgeColor.copy(alpha = 0.55f)
        } else {
            EmberBorder
        },
        animationSpec = tween(durationMillis = 450),
        label = "order_card_border_color"
    )
    val paymentLabel = if (order.paymentMethod == "CASH_COUNTER") "Cash at Counter" else "Cash on Delivery"

    SmokedCard(borderColor = cardBorderColor) {
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
                    Text(order.orderNumber, style = MonoPriceStyle, color = SmokedAmber)
                    val subtitle = if (order.tableLabel.isNotBlank()) {
                        "${orderType.displayLabel} (${order.tableLabel}) · ${formatTimestampShort(order.createdAt)}"
                    } else {
                        "${orderType.displayLabel} · ${formatTimestampShort(order.createdAt)}"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 }) togetherWith
                            (fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 3 })
                    },
                    label = "order_status_pill_transition"
                ) { animStatus ->
                    StatusPill(label = animStatus.displayLabel, color = badgeColor)
                }
            }

            if (status != OrderStatus.CANCELLED) {
                AnimatedOrderStatusBanner(
                    status = status,
                    orderType = orderType,
                    accentColor = badgeColor
                )
                OrderStatusStepper(currentStep = status.stepIndex, accentColor = badgeColor)
            }

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (order.deliveryAddress.isNotBlank() || order.customerPhone.isNotBlank()) {
                val dest = listOfNotNull(
                    order.deliveryAddress.takeIf { it.isNotBlank() },
                    order.customerPhone.takeIf { it.isNotBlank() }?.let { "($it)" }
                ).joinToString(" ")
                Text(
                    text = "Details: $dest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (order.hasLocation) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "GPS Pinned", tint = CorianderGreen, modifier = Modifier.size(15.dp))
                    Text(
                        text = "GPS Location Shared with Rider (${formatCoordinates(order.customerLat, order.customerLng)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = CorianderGreen
                    )
                }
            }
            if (order.assignedRiderName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.TwoWheeler, contentDescription = "Assigned Rider", tint = SmokedAmber, modifier = Modifier.size(15.dp))
                    val phonePart = if (order.assignedRiderPhone.isNotBlank()) " · ${order.assignedRiderPhone}" else ""
                    Text(
                        text = "Delivery Rider: ${order.assignedRiderName}$phonePart",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmokedAmber
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = paymentLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = CorianderGreen
                    )
                    Text(
                        text = formatPkr(order.totalPkr),
                        style = MonoPriceStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (order.discountPkr > 0L) {
                        Text(
                            text = "Saved ${formatPkr(order.discountPkr)} (${order.loyaltyPointsRedeemed} pts)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SmokedAmber
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPrintBill,
                        border = BorderStroke(1.dp, SmokedAmber),
                        modifier = Modifier.testTag("customer_print_bill_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = "Print Bill ${order.orderNumber}",
                            tint = SmokedAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Print Bill", color = SmokedAmber)
                    }
                    if (status == OrderStatus.PLACED) {
                        OutlinedButton(
                            onClick = onCancel,
                            border = BorderStroke(1.dp, CrimsonEmber),
                            modifier = Modifier.testTag("cancel_order_${order.id}")
                        ) {
                            Text("Cancel", color = CrimsonEmber)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedOrderStatusBanner(
    status: OrderStatus,
    orderType: OrderType,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "order_status_motion")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_pulse_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_halo_alpha"
    )
    val riderGlideDp by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rider_glide_offset"
    )

    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("animated_order_status_banner")
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(tween(340)) + slideInVertically(tween(340)) { it / 4 }) togetherWith
                    (fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 4 })
            },
            label = "order_banner_content"
        ) { active ->
            val (headline, detailText, icon) = when (active) {
                OrderStatus.PLACED -> Triple(
                    "Order Received",
                    "Waiting for Oak & Smoke counter confirmation.",
                    Icons.Filled.ReceiptLong
                )
                OrderStatus.CONFIRMED -> Triple(
                    "Order Confirmed",
                    "Queued for our live oak-wood charcoal kitchen.",
                    Icons.Filled.CheckCircle
                )
                OrderStatus.GRILLING -> Triple(
                    "Preparing on Oak Charcoal Grill",
                    "Chefs are freshly searing & simmering your order right now.",
                    Icons.Filled.LocalFireDepartment
                )
                OrderStatus.READY -> Triple(
                    if (orderType == OrderType.COD_DELIVERY) "Packed & Ready for Rider" else "Fresh & Ready to Serve",
                    if (orderType == OrderType.COD_DELIVERY) "Hot from the grill — rider pickup in progress." else "Your dishes are piping hot and ready.",
                    Icons.Filled.Restaurant
                )
                OrderStatus.OUT_FOR_DELIVERY -> Triple(
                    "Out for Delivery",
                    "Our rider is heading to your pinned Hasilpur location.",
                    Icons.Filled.TwoWheeler
                )
                OrderStatus.DELIVERED -> Triple(
                    "Delivered & Completed",
                    "Thank you for dining with Oak & Smoke Hasilpur!",
                    Icons.Filled.CheckCircle
                )
                OrderStatus.CANCELLED -> Triple(
                    "Order Cancelled",
                    "This order was cancelled.",
                    Icons.Filled.ReceiptLong
                )
            }

            val isLiveMotion = active == OrderStatus.PLACED ||
                active == OrderStatus.CONFIRMED ||
                active == OrderStatus.GRILLING ||
                active == OrderStatus.READY ||
                active == OrderStatus.OUT_FOR_DELIVERY

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLiveMotion) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = haloAlpha))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = headline,
                            tint = accentColor,
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = if (active == OrderStatus.OUT_FOR_DELIVERY) riderGlideDp.dp else 0.dp)
                                .scale(if (active == OrderStatus.GRILLING) pulseScale else 1f)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderStatusStepper(
    currentStep: Int,
    accentColor: Color = SmokedAmber
) {
    val steps = listOf("Placed", "Confirmed", "Preparing", "Ready", "On Way", "Done")
    val clampedStep = currentStep.coerceIn(0, steps.lastIndex)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedStep.toFloat() / steps.lastIndex.toFloat(),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stepper_progress_fill"
    )
    val activePulse = rememberInfiniteTransition(label = "stepper_active_pulse")
    val activeRingScale by activePulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "active_step_ring_scale"
    )
    val activeRingAlpha by activePulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "active_step_ring_alpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(EmberBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { idx, label ->
                val reached = idx <= clampedStep
                val isCurrentActive = idx == clampedStep && clampedStep < steps.lastIndex
                val nodeColor by animateColorAsState(
                    targetValue = if (reached) accentColor else EmberBorder,
                    animationSpec = tween(durationMillis = 360),
                    label = "step_node_color_$idx"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (reached) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 360),
                    label = "step_label_color_$idx"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentActive) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(activeRingScale)
                                    .alpha(activeRingAlpha)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(nodeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (reached) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = label,
                                    tint = CharcoalDeep,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Normal,
                        color = labelColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerReservationsTab(viewModel: RestaurantViewModel) {
    val reservations by viewModel.customerReservations.collectAsStateWithLifecycle()
    val diningTables by viewModel.diningTables.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val dateOptions = remember {
        val fmt = SimpleDateFormat("EEE, dd MMM", Locale.US)
        (0..3).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            if (offset == 0) "Today (${fmt.format(cal.time)})" else fmt.format(cal.time)
        }
    }
    val timeSlots = listOf("6:00 PM", "7:30 PM", "8:30 PM", "9:30 PM", "10:30 PM")

    var selectedDate by remember { mutableStateOf(dateOptions.first()) }
    var selectedSlot by remember { mutableStateOf("8:30 PM") }
    var partySize by remember { mutableLongStateOf(4L) }
    var seatingZone by remember { mutableStateOf(SeatingZone.FAMILY_CABIN) }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone.orEmpty()) }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Book a Table",
                subtitle = "Oak & Smoke Restaurant Hasilpur"
            )
        }

        item {
            SmokedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Date", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dateOptions.forEach { d ->
                            FilterChip(
                                selected = selectedDate == d,
                                onClick = { selectedDate = d },
                                label = { Text(d) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmokedAmber,
                                    selectedLabelColor = CharcoalDeep
                                )
                            )
                        }
                    }

                    Text("Time Slot", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        timeSlots.forEach { slot ->
                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = slot },
                                label = { Text(slot) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmokedAmber,
                                    selectedLabelColor = CharcoalDeep
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Party Size", style = MaterialTheme.typography.titleSmall)
                            Text("Up to 30 guests", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (partySize > 1L) partySize-- },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease guests", tint = SmokedAmber)
                            }
                            Text("$partySize Guests", style = MonoPriceStyle)
                            IconButton(
                                onClick = { if (partySize < 30L) partySize++ },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase guests", tint = SmokedAmber)
                            }
                        }
                    }

                    Text("Seating Zone", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SeatingZone.entries.forEach { zone ->
                            FilterChip(
                                selected = seatingZone == zone,
                                onClick = { seatingZone = zone },
                                label = { Text(zone.displayLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmokedAmber,
                                    selectedLabelColor = CharcoalDeep
                                )
                            )
                        }
                    }
                    val zoneTables = remember(diningTables, seatingZone) {
                        diningTables.filter { it.available && it.zone == seatingZone.name }
                    }
                    if (zoneTables.isNotEmpty()) {
                        val maxCap = zoneTables.maxOf { it.capacity }
                        Text(
                            text = "${zoneTables.size} active tables in ${seatingZone.displayLabel} (up to $maxCap seats/table): ${zoneTables.joinToString(", ") { it.label }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CorianderGreen
                        )
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reservation_phone_input")
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Occasion or Special Request (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val name = profile?.displayName?.ifBlank { viewModel.userDisplayName } ?: viewModel.userDisplayName
                            viewModel.createReservation(
                                customerName = name,
                                customerPhone = phone,
                                dateLabel = selectedDate,
                                timeSlot = selectedSlot,
                                partySize = partySize,
                                seatingZone = seatingZone,
                                specialNotes = notes
                            ) {
                                notes = ""
                            }
                        },
                        enabled = !isBusy && phone.trim().length >= 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("book_table_submit_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Text("Confirm Table Reservation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SectionHeader(title = "My Reservations")
        }

        if (reservations.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.TableRestaurant,
                    title = "No upcoming reservations",
                    subtitle = "Book a Family Cabin, Charcoal Hall table, or Outdoor Grill Lawn spot above."
                )
            }
        } else {
            items(reservations, key = { it.id }) { booking ->
                CustomerReservationCard(
                    booking = booking,
                    onCancel = { viewModel.cancelOwnReservation(booking.id) }
                )
            }
        }
    }
}

@Composable
private fun CustomerReservationCard(
    booking: ReservationRecord,
    onCancel: () -> Unit
) {
    val status = ReservationStatus.fromRaw(booking.status)
    val color = when (status) {
        ReservationStatus.CONFIRMED, ReservationStatus.SEATED, ReservationStatus.COMPLETED -> CorianderGreen
        ReservationStatus.CANCELLED -> CrimsonEmber
        else -> SmokedAmber
    }
    SmokedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.dateLabel} · ${booking.timeSlot}",
                    style = MaterialTheme.typography.titleSmall,
                    color = SmokedAmber
                )
                StatusPill(label = status.displayLabel, color = color)
            }
            Text(
                text = "${booking.partySize} Guests · ${SeatingZone.fromRaw(booking.seatingZone).displayLabel}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (booking.specialNotes.isNotBlank()) {
                Text(
                    text = "Note: ${booking.specialNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, CrimsonEmber)
                ) {
                    Text("Cancel Booking", color = CrimsonEmber)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerLoyaltyFeedbackTab(viewModel: RestaurantViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val feedbackList by viewModel.customerFeedback.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var rating by remember { mutableLongStateOf(5L) }
    var category by remember { mutableStateOf(FeedbackCategory.FOOD) }
    var comment by remember { mutableStateOf("") }

    val pts = profile?.loyaltyPoints ?: 0L
    val ptVal = config?.loyaltyPointValuePkr ?: 5L
    val earnPer100 = config?.loyaltyEarnPer100Pkr ?: 1L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SmokedCard(borderColor = SmokedAmber) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "OAK & SMOKE LOYALTY CLUB",
                            style = MaterialTheme.typography.labelSmall,
                            color = SmokedAmber
                        )
                        Text(
                            text = "$pts Points",
                            style = MonoMetricStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Redeemable value: ${formatPkr(pts * ptVal)} (1 pt = ${formatPkr(ptVal)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = CorianderGreen
                        )
                        Text(
                            text = if (earnPer100 > 0L) {
                                "Earning rate: $earnPer100 pt per Rs. 100 on delivered COD orders"
                            } else {
                                "Point earning is currently paused by management"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SmokedAmber.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Loyalty,
                            contentDescription = "Loyalty Rewards",
                            tint = SmokedAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Share Your Feedback",
                subtitle = "Help Oak & Smoke Hasilpur serve you even better"
            )
        }

        item {
            SmokedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1L..5L).forEach { star ->
                            IconButton(
                                onClick = { rating = star },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$star stars",
                                    tint = SmokedAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("$rating / 5", style = MonoPriceStyle, color = SmokedAmber)
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeedbackCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.displayLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmokedAmber,
                                    selectedLabelColor = CharcoalDeep
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Tell us about the Karahi, BBQ, or delivery...") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_comment_input")
                    )

                    Button(
                        onClick = {
                            viewModel.submitFeedback(rating, category, comment) {
                                comment = ""
                            }
                        },
                        enabled = !isBusy && comment.trim().length >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_feedback_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Text("Submit Feedback", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (feedbackList.isNotEmpty()) {
            item {
                SectionHeader(title = "Your Submitted Reviews")
            }
            items(feedbackList, key = { it.id }) { fb ->
                FeedbackItemCard(fb)
            }
        }
    }
}

@Composable
private fun FeedbackItemCard(entry: FeedbackEntry) {
    SmokedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(label = FeedbackCategory.fromRaw(entry.category).displayLabel, color = SmokedAmber)
                Text("${entry.rating} ★", style = MonoPriceStyle, color = SmokedAmber)
            }
            Text(entry.comment, style = MaterialTheme.typography.bodyMedium)
            if (entry.staffReply.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Oak & Smoke Management Reply",
                            style = MaterialTheme.typography.labelSmall,
                            color = CorianderGreen
                        )
                        Text(entry.staffReply, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

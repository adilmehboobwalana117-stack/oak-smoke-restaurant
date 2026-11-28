package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.OrderRecord
import com.example.data.OrderType
import com.example.data.formatPkr
import com.example.data.formatTimestampShort
import com.example.data.parsedLineItems
import com.example.services.IncomingOrderAlarmController
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper

/**
 * Compact 3-Minute Order Sound Alarm control strip for Owner Admin Panel & Staff Operations Portal.
 */
@Composable
fun AdminAlarmControlStrip(
    alarmEnabled: Boolean,
    isRinging: Boolean,
    isTestAlarmActive: Boolean,
    remainingSeconds: Int,
    pendingCount: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleTestAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isRinging) CrimsonEmber.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            when {
                isRinging -> CrimsonEmber
                alarmEnabled -> SmokedAmber.copy(alpha = 0.5f)
                else -> EmberBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (alarmEnabled) Icons.Filled.VolumeUp else Icons.Filled.NotificationsOff,
                contentDescription = "Order alarm status",
                tint = when {
                    isRinging -> CrimsonEmber
                    alarmEnabled -> SmokedAmber
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isTestAlarmActive -> "Test Alarm Ringing · ${IncomingOrderAlarmController.formatCountdown(remainingSeconds)}"
                        isRinging -> "Ringing for $pendingCount New Order(s) · ${IncomingOrderAlarmController.formatCountdown(remainingSeconds)}"
                        alarmEnabled -> "3-Min Order Sound Alarm: ON"
                        else -> "3-Min Order Sound Alarm: OFF"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isRinging) CrimsonEmber else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (alarmEnabled) {
                        "Rings continuously for 3 min until Staff or Admin accepts or rejects"
                    } else {
                        "Enable to ring continuously when new orders arrive"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onToggleTestAlarm,
                border = BorderStroke(1.dp, if (isTestAlarmActive) CrimsonEmber else SmokedAmber),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("admin_alarm_test_btn")
            ) {
                Icon(
                    imageVector = if (isTestAlarmActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isTestAlarmActive) "Stop test alarm" else "Test alarm sound",
                    tint = if (isTestAlarmActive) CrimsonEmber else SmokedAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isTestAlarmActive) "Stop" else "Test",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isTestAlarmActive) CrimsonEmber else SmokedAmber
                )
            }
            Switch(
                checked = alarmEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CharcoalDeep,
                    checkedTrackColor = SmokedAmber
                ),
                modifier = Modifier.testTag("admin_alarm_toggle_switch")
            )
        }
    }
}

/**
 * High-urgency pulsing banner pinned above Admin & Staff portals whenever one or more
 * newly placed orders await acceptance, or when a live test alarm is active.
 */
@Composable
fun IncomingOrderAlarmBanner(
    pendingOrders: List<OrderRecord>,
    isRinging: Boolean,
    isTestAlarmActive: Boolean,
    remainingSeconds: Int,
    onAcceptOrder: (OrderRecord) -> Unit,
    onRequestRejectOrder: (OrderRecord) -> Unit,
    onMuteAlarm: () -> Unit,
    onStopTestAlarm: () -> Unit,
    onOpenQueueSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = pendingOrders.isNotEmpty() || isTestAlarmActive
    val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val bellScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_scale"
    )
    val borderAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val newestOrder = pendingOrders.firstOrNull()
        val countdownFraction = (remainingSeconds.toFloat() / IncomingOrderAlarmController.DEFAULT_ALARM_DURATION_SECONDS.toFloat())
            .coerceIn(0f, 1f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(enabled = pendingOrders.isNotEmpty()) { onOpenQueueSheet() }
                .testTag("incoming_order_alarm_banner"),
            shape = RoundedCornerShape(16.dp),
            color = if (isRinging) CrimsonEmber.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = if (isRinging) 2.dp else 1.dp,
                color = if (isRinging) CrimsonEmber.copy(alpha = borderAlpha) else SmokedAmber
            ),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(if (isRinging) bellScale else 1f)
                            .clip(CircleShape)
                            .background(if (isRinging) CrimsonEmber else SmokedAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Incoming order alarm",
                            tint = CharcoalDeep,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when {
                                    isTestAlarmActive && newestOrder == null -> "TEST ORDER ALARM"
                                    pendingOrders.size > 1 -> "${pendingOrders.size} NEW ORDERS WAITING"
                                    else -> "NEW ORDER · ${newestOrder?.orderNumber.orEmpty()}"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRinging) CrimsonEmber else SmokedAmber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (newestOrder != null) {
                            val typeBadge = when {
                                newestOrder.tableLabel.isNotBlank() -> newestOrder.tableLabel
                                else -> OrderType.fromRaw(newestOrder.orderType).displayLabel
                            }
                            Text(
                                text = "$typeBadge · ${newestOrder.customerName.ifBlank { "Guest" }} · ${formatPkr(newestOrder.totalPkr)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Verifying 3-minute continuous kitchen/admin alarm volume",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live 3:00 Countdown Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRinging) CrimsonEmber else CharcoalDeep,
                        border = BorderStroke(1.dp, if (isRinging) CrimsonEmber else SmokedAmber)
                    ) {
                        Text(
                            text = if (isRinging) IncomingOrderAlarmController.formatCountdown(remainingSeconds) else "PENDING",
                            style = MonoPriceStyle,
                            color = if (isRinging) CharcoalDeep else SmokedAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isRinging) {
                        IconButton(
                            onClick = if (isTestAlarmActive && newestOrder == null) onStopTestAlarm else onMuteAlarm,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("alarm_mute_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeOff,
                                contentDescription = "Mute alarm sound",
                                tint = SmokedAmber
                            )
                        }
                    }
                }

                if (isRinging) {
                    LinearProgressIndicator(
                        progress = { countdownFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CrimsonEmber,
                        trackColor = EmberBorder
                    )
                }

                if (newestOrder != null) {
                    val itemsPreview = newestOrder.itemsSummary.ifBlank {
                        newestOrder.parsedLineItems().joinToString(", ") { "${it.qty}x ${it.name}" }
                    }
                    if (itemsPreview.isNotBlank()) {
                        Text(
                            text = itemsPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onAcceptOrder(newestOrder) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CorianderGreen,
                                contentColor = CharcoalDeep
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                                .testTag("alarm_accept_order_btn")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Accept Order", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onRequestRejectOrder(newestOrder) },
                            border = BorderStroke(1.dp, CrimsonEmber),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(48.dp)
                                .testTag("alarm_reject_order_btn")
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, tint = CrimsonEmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", color = CrimsonEmber, fontWeight = FontWeight.Bold)
                        }

                        if (pendingOrders.size > 1) {
                            OutlinedButton(
                                onClick = onOpenQueueSheet,
                                border = BorderStroke(1.dp, SmokedAmber),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("alarm_open_queue_btn")
                            ) {
                                Text("All (${pendingOrders.size})", color = SmokedAmber)
                            }
                        }
                    }
                } else if (isTestAlarmActive) {
                    Button(
                        onClick = onStopTestAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stop_test_alarm_btn")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop Test Alarm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Full incoming orders queue sheet allowing Admin & Staff to review all unaccepted PLACED
 * orders and Accept or Reject each one individually.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingOrdersQueueSheet(
    pendingOrders: List<OrderRecord>,
    isRinging: Boolean,
    remainingSeconds: Int,
    onDismiss: () -> Unit,
    onAcceptOrder: (OrderRecord) -> Unit,
    onRequestRejectOrder: (OrderRecord) -> Unit,
    onMuteAlarm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Incoming Orders Queue (${pendingOrders.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isRinging) {
                            "Continuous alarm active · ${IncomingOrderAlarmController.formatCountdown(remainingSeconds)} remaining"
                        } else {
                            "Accept to send to Kitchen or Reject to cancel"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRinging) CrimsonEmber else SmokedAmber
                    )
                }
                if (isRinging) {
                    OutlinedButton(
                        onClick = onMuteAlarm,
                        border = BorderStroke(1.dp, SmokedAmber)
                    ) {
                        Icon(Icons.Filled.VolumeOff, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mute", color = SmokedAmber)
                    }
                }
            }

            if (pendingOrders.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "All incoming orders handled",
                    subtitle = "No unaccepted orders waiting. The alarm is silenced and armed for the next order."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(pendingOrders, key = { it.id }) { order ->
                        val typeLabel = if (order.tableLabel.isNotBlank()) {
                            "Dine-In · ${order.tableLabel}"
                        } else {
                            OrderType.fromRaw(order.orderType).displayLabel
                        }
                        SmokedCard(borderColor = CrimsonEmber.copy(alpha = 0.7f)) {
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
                                    Column {
                                        Text(
                                            text = "${order.orderNumber} · $typeLabel",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SmokedAmber
                                        )
                                        Text(
                                            text = "${order.customerName.ifBlank { "Guest" }} · ${formatTimestampShort(order.createdAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatPkr(order.totalPkr),
                                        style = MonoPriceStyle,
                                        color = SmokedAmber
                                    )
                                }

                                HorizontalDivider(color = EmberBorder)

                                val parsedItems = order.parsedLineItems()
                                if (parsedItems.isNotEmpty()) {
                                    parsedItems.forEach { item ->
                                        Text(
                                            text = "${item.qty}x ${item.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else if (order.itemsSummary.isNotBlank()) {
                                    Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                                }

                                if (order.customerPhone.isNotBlank()) {
                                    Text(
                                        text = "Phone: ${order.customerPhone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (order.deliveryAddress.isNotBlank()) {
                                    Text(
                                        text = "Address: ${order.deliveryAddress}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (order.notes.isNotBlank()) {
                                    Text(
                                        text = "Note: ${order.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TandoorCopper
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onAcceptOrder(order) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CorianderGreen,
                                            contentColor = CharcoalDeep
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(48.dp)
                                            .testTag("queue_accept_${order.id}")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Accept & Grill", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onRequestRejectOrder(order) },
                                        border = BorderStroke(1.dp, CrimsonEmber),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .height(48.dp)
                                            .testTag("queue_reject_${order.id}")
                                    ) {
                                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = CrimsonEmber, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Reject", color = CrimsonEmber, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RejectIncomingOrderDialog(
    order: OrderRecord,
    onDismiss: () -> Unit,
    onConfirmReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Order ${order.orderNumber}?") },
        text = {
            Text(
                text = "This will cancel ${order.orderNumber} (${formatPkr(order.totalPkr)}) for ${order.customerName.ifBlank { "the guest" }}, notify the customer, and stop the incoming order alarm."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmReject,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmber, contentColor = CharcoalDeep),
                modifier = Modifier.testTag("confirm_reject_order_btn")
            ) {
                Text("Reject & Stop Alarm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Waiting")
            }
        }
    )
}

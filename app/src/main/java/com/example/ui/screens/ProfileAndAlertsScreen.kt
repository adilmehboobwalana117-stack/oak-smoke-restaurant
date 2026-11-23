package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NotificationItem
import com.example.data.UserRoleTier
import com.example.data.formatTimestampShort
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import com.example.viewmodel.PortalMode
import com.example.viewmodel.RestaurantViewModel

@Composable
fun ProfileAndAlertsScreen(
    viewModel: RestaurantViewModel,
    onSignOut: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    val portalMode by viewModel.portalMode.collectAsStateWithLifecycle()
    val roleGrant by viewModel.roleGrant.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var displayName by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName?.ifBlank { viewModel.userDisplayName } ?: viewModel.userDisplayName)
    }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone.orEmpty()) }
    var address by remember(profile?.deliveryAddress) { mutableStateOf(profile?.deliveryAddress.orEmpty()) }
    var notifEnabled by remember(profile?.notificationsEnabled) { mutableStateOf(profile?.notificationsEnabled ?: true) }

    var showStaffUnlockDialog by remember { mutableStateOf(false) }
    var showOwnerInitDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.showMessage("Real-time Android alerts enabled!")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tiered Workspace & Staff Portal Card
        item {
            SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.65f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Portal access", tint = SmokedAmber)
                            Text("Role & Staff Portal", style = MaterialTheme.typography.titleMedium)
                        }
                        StatusPill(
                            label = if (roleGrant != null) "${effectiveRole.displayLabel} · ${roleGrant!!.staffId}" else effectiveRole.displayLabel,
                            color = if (effectiveRole == UserRoleTier.CUSTOMER) SmokedAmber else CorianderGreen
                        )
                    }

                    if (config?.ownerInitialized != true) {
                        Text(
                            text = "Oak & Smoke Owner setup is not initialized yet. Claim Owner access to set your Master Staff ID, Passcode, and seed the Pakistani Karahi & BBQ menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showOwnerInitDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("claim_owner_setup_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                        ) {
                            Text("Initialize Oak & Smoke Owner Portal", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (effectiveRole != UserRoleTier.CUSTOMER) {
                        Text("Switch Active Workspace", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = portalMode == PortalMode.CUSTOMER,
                                onClick = { viewModel.setPortalMode(PortalMode.CUSTOMER) },
                                label = { Text("Customer") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmokedAmber, selectedLabelColor = CharcoalDeep),
                                modifier = Modifier.testTag("switch_portal_customer")
                            )
                            if (effectiveRole == UserRoleTier.RIDER || effectiveRole == UserRoleTier.MANAGER || effectiveRole == UserRoleTier.OWNER) {
                                FilterChip(
                                    selected = portalMode == PortalMode.RIDER,
                                    onClick = { viewModel.setPortalMode(PortalMode.RIDER) },
                                    label = { Text("Rider") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmokedAmber, selectedLabelColor = CharcoalDeep),
                                    modifier = Modifier.testTag("switch_portal_rider")
                                )
                            }
                            if (effectiveRole != UserRoleTier.RIDER) {
                                FilterChip(
                                    selected = portalMode == PortalMode.STAFF,
                                    onClick = { viewModel.setPortalMode(PortalMode.STAFF) },
                                    label = { Text("Staff") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmokedAmber, selectedLabelColor = CharcoalDeep),
                                    modifier = Modifier.testTag("switch_portal_staff")
                                )
                            }
                            if (effectiveRole == UserRoleTier.OWNER || effectiveRole == UserRoleTier.MANAGER) {
                                FilterChip(
                                    selected = portalMode == PortalMode.OWNER,
                                    onClick = { viewModel.setPortalMode(PortalMode.OWNER) },
                                    label = { Text("Owner") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SmokedAmber, selectedLabelColor = CharcoalDeep),
                                    modifier = Modifier.testTag("switch_portal_owner")
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showStaffUnlockDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unlock_staff_id_btn")
                        ) {
                            Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Staff / Rider Sign-In")
                        }
                        if (roleGrant != null) {
                            OutlinedButton(
                                onClick = { viewModel.lockStaffPortal() },
                                border = BorderStroke(1.dp, CrimsonEmber)
                            ) {
                                Text("Lock Role", color = CrimsonEmber)
                            }
                        }
                    }
                }
            }
        }

        // Customer Profile Customization
        item {
            SectionHeader(
                title = "Profile & Preferences",
                subtitle = viewModel.userEmail
            )
        }
        item {
            SmokedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (for COD & Bookings)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Default Hasilpur Delivery Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Real-Time Alerts", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Order updates, table confirmations & daily specials",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { checked ->
                                notifEnabled = checked
                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                    Button(
                        onClick = { viewModel.saveOwnProfile(displayName, phone, address, notifEnabled) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Notifications & Alerts Feed
        item {
            SectionHeader(
                title = "Account & Restaurant Alerts",
                subtitle = "Tap an unread notification to mark as read"
            )
        }
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Notifications,
                    title = "No alerts yet",
                    subtitle = "Order status changes, reservation approvals, loyalty updates, and Daily Specials appear here in real time."
                )
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                NotificationRowCard(
                    item = notif,
                    onClick = { if (!notif.read) viewModel.markNotificationRead(notif.id) }
                )
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, CrimsonEmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = CrimsonEmber)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out of Google Account", color = CrimsonEmber)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showStaffUnlockDialog) {
        var staffIdInput by remember { mutableStateOf("") }
        var passcodeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showStaffUnlockDialog = false },
            title = { Text("Staff, Rider & Owner Sign-In") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the Staff ID or Rider ID and Passcode assigned by the Oak & Smoke Owner to unlock your tiered permissions.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = staffIdInput,
                        onValueChange = { staffIdInput = it },
                        label = { Text("Staff / Rider ID (e.g., OS-001 or RD-101)") },
                        singleLine = true,
                        modifier = Modifier.testTag("staff_id_dialog_input")
                    )
                    OutlinedTextField(
                        value = passcodeInput,
                        onValueChange = { passcodeInput = it },
                        label = { Text("Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.testTag("staff_passcode_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlockStaffPortal(staffIdInput, passcodeInput) {
                            showStaffUnlockDialog = false
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStaffUnlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showOwnerInitDialog) {
        var masterId by remember { mutableStateOf("OS-001") }
        var masterPasscode by remember { mutableStateOf("") }
        var seedCatalog by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showOwnerInitDialog = false },
            title = { Text("Initialize Oak & Smoke Owner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Create your Master Owner Staff ID & Passcode for Oak & Smoke Restaurant Hasilpur.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = masterId,
                        onValueChange = { masterId = it },
                        label = { Text("Master Owner Staff ID") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = masterPasscode,
                        onValueChange = { masterPasscode = it },
                        label = { Text("Owner Passcode (min 4 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = seedCatalog, onCheckedChange = { seedCatalog = it })
                        Text(
                            "Seed starter Pakistani Karahi, BBQ & Platters menu + kitchen inventory",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.initializeOwner(masterId, masterPasscode, seedCatalog) {
                            showOwnerInitDialog = false
                        }
                    },
                    enabled = !isBusy && masterPasscode.trim().length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    Text("Initialize Owner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOwnerInitDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotificationRowCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    SmokedCard(
        modifier = Modifier.clickable(onClick = onClick),
        borderColor = if (!item.read) SmokedAmber else MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (!item.read) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                contentDescription = item.type,
                tint = if (!item.read) SmokedAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusPill(
                        label = if (item.type == "RIDER_READY") "READY ALERT" else item.type,
                        color = when (item.type) {
                            "RIDER_READY" -> CorianderGreen
                            "SPECIAL" -> TandoorCopper
                            else -> SmokedAmber
                        }
                    )
                }
                Text(item.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatTimestampShort(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

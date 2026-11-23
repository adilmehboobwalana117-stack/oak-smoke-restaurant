package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.InventoryCategory
import com.example.data.StaffMember
import com.example.data.UserProfile
import com.example.data.UserRoleTier
import com.example.data.formatPkr
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.viewmodel.RestaurantViewModel

@Composable
fun OwnerStaffPayrollTab(viewModel: RestaurantViewModel) {
    val staffList by viewModel.allStaffMembers.collectAsStateWithLifecycle()
    var showCreateStaff by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffMember?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val totalPayroll = staffList.filter { it.active }.sumOf { it.monthlySalaryPkr }
            SectionHeader(
                title = "Staff, Schedules & Salaries",
                subtitle = "Active monthly payroll: ${formatPkr(totalPayroll)}",
                trailing = {
                    Button(
                        onClick = { showCreateStaff = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                        modifier = Modifier.testTag("add_staff_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Staff", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Staff")
                    }
                }
            )
        }

        if (staffList.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Badge,
                    title = "No staff accounts yet",
                    subtitle = "Create Staff IDs & Passcodes with tiered permissions (Staff, Manager, Owner), shift schedules, and monthly PKR salaries."
                )
            }
        } else {
            items(staffList, key = { it.id }) { member ->
                SmokedCard(borderColor = if (member.active) MaterialTheme.colorScheme.outline else CrimsonEmber) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${member.fullName} · ${member.staffId}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${member.shiftSummary} (${member.scheduleDays})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                label = if (member.active) member.roleTier else "INACTIVE",
                                color = if (member.active) SmokedAmber else CrimsonEmber
                            )
                            IconButton(onClick = { editingStaff = member }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${member.fullName}", tint = SmokedAmber)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Salary: ${formatPkr(member.monthlySalaryPkr)}", style = MonoPriceStyle, color = SmokedAmber)
                            StatusPill(
                                label = member.salaryStatus,
                                color = if (member.salaryStatus == "PAID") CorianderGreen else CrimsonEmber
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Orders Handled: ${member.ordersHandledCount}", style = MaterialTheme.typography.bodySmall, color = CorianderGreen)
                            Text("Staff Loyalty: ${member.loyaltyPoints} pts", style = MonoPriceStyle, color = SmokedAmber)
                        }

                        if (member.monthlySalaryPkr > 0L && member.salaryStatus != "PAID") {
                            OutlinedButton(
                                onClick = { viewModel.payStaffSalary(member, logExpense = true) },
                                border = BorderStroke(1.dp, CorianderGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pay_salary_${member.id}")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, tint = CorianderGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mark Salary Paid & Log Expense", color = CorianderGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateStaff || editingStaff != null) {
        StaffEditorDialog(
            existing = editingStaff,
            onDismiss = {
                showCreateStaff = false
                editingStaff = null
            },
            onSave = { staffId, name, pass, tier, salary, shift, days, loyalty, active, phone, vehicle ->
                viewModel.saveStaffMember(
                    editingStaff, staffId, name, pass, tier, salary, shift, days, loyalty, active, phone, vehicle
                )
                showCreateStaff = false
                editingStaff = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffEditorDialog(
    existing: StaffMember?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, UserRoleTier, Long, String, String, Long, Boolean, String, String) -> Unit
) {
    var staffId by remember { mutableStateOf(existing?.staffId ?: "OS-") }
    var fullName by remember { mutableStateOf(existing?.fullName.orEmpty()) }
    var passcode by remember { mutableStateOf("") }
    var roleTier by remember { mutableStateOf(UserRoleTier.fromRaw(existing?.roleTier ?: "STAFF")) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var vehicleInfo by remember { mutableStateOf(existing?.vehicleInfo.orEmpty()) }
    var salaryText by remember { mutableStateOf((existing?.monthlySalaryPkr ?: 35000L).toString()) }
    var shiftSummary by remember { mutableStateOf(existing?.shiftSummary ?: "Evening 4 PM - 12 AM") }
    var scheduleDays by remember { mutableStateOf(existing?.scheduleDays ?: "Mon - Sat") }
    var loyaltyText by remember { mutableStateOf((existing?.loyaltyPoints ?: 0L).toString()) }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Staff / Rider Account" else "Edit Staff & Salary") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = staffId, onValueChange = { staffId = it; errorText = null }, label = { Text("Staff / Rider ID (e.g. OS-102 or RD-101)") }, singleLine = true)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it; errorText = null }, label = { Text("Full Name") }, singleLine = true)
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it; errorText = null },
                    label = { Text(if (existing == null) "Passcode (min 4 chars)" else "New Passcode (blank to keep)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Text("Permission Tier", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(UserRoleTier.RIDER, UserRoleTier.STAFF, UserRoleTier.MANAGER, UserRoleTier.OWNER).forEach { tier ->
                        FilterChip(
                            selected = roleTier == tier,
                            onClick = { roleTier = tier },
                            label = { Text(tier.displayLabel) }
                        )
                    }
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number (optional)") }, singleLine = true)
                if (roleTier == UserRoleTier.RIDER) {
                    OutlinedTextField(value = vehicleInfo, onValueChange = { vehicleInfo = it }, label = { Text("Bike / Vehicle Info") }, singleLine = true)
                }
                OutlinedTextField(value = salaryText, onValueChange = { salaryText = it.filter { c -> c.isDigit() } }, label = { Text("Monthly Salary (PKR)") }, singleLine = true)
                OutlinedTextField(value = loyaltyText, onValueChange = { loyaltyText = it.filter { c -> c.isDigit() } }, label = { Text("Staff Loyalty Points") }, singleLine = true)
                OutlinedTextField(value = shiftSummary, onValueChange = { shiftSummary = it }, label = { Text("Shift Hours") }, singleLine = true)
                OutlinedTextField(value = scheduleDays, onValueChange = { scheduleDays = it }, label = { Text("Work Days") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active Staff / Rider Access")
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
                    val pts = loyaltyText.toLongOrNull() ?: 0L
                    val cleanId = staffId.trim().uppercase()
                    val changedId = existing != null && !cleanId.equals(existing.staffId, ignoreCase = true)
                    when {
                        cleanId.length < 2 -> errorText = "Enter a valid ID (e.g., OS-102 or RD-101)."
                        fullName.trim().length < 2 -> errorText = "Enter the employee's full name."
                        existing == null && passcode.trim().length < 4 -> errorText = "Passcode must be at least 4 characters."
                        changedId && passcode.trim().length < 4 -> errorText = "Enter a new passcode (min 4 chars) when changing ID."
                        else -> onSave(staffId, fullName, passcode, roleTier, sal, shiftSummary, scheduleDays, pts, active, phone, vehicleInfo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerLoyaltyAndStockTab(viewModel: RestaurantViewModel) {
    val customers by viewModel.allUsers.collectAsStateWithLifecycle()
    val staffList by viewModel.allStaffMembers.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryItems.collectAsStateWithLifecycle()

    var editingCustomerLoyalty by remember { mutableStateOf<UserProfile?>(null) }
    var editingStaffLoyalty by remember { mutableStateOf<StaffMember?>(null) }
    var showAddInventoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Customer Loyalty Control",
                subtitle = "Admin edit customer reward points"
            )
        }
        if (customers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Loyalty,
                    title = "No customer profiles loaded",
                    subtitle = "Signed-in Oak & Smoke customers will appear here so you can reward or edit loyalty points."
                )
            }
        } else {
            items(customers, key = { it.userId }) { user ->
                SmokedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName.ifBlank { "Guest" }, style = MaterialTheme.typography.titleSmall)
                            Text(user.email.ifBlank { user.phone }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${user.loyaltyPoints} pts", style = MonoPriceStyle, color = SmokedAmber)
                        IconButton(
                            onClick = { editingCustomerLoyalty = user },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_customer_loyalty_${user.userId}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit loyalty", tint = SmokedAmber)
                        }
                    }
                }
            }
        }

        if (staffList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Staff Loyalty Rewards",
                    subtitle = "Admin edit employee reward points"
                )
            }
            items(staffList, key = { "stf_loy_${it.id}" }) { staff ->
                SmokedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${staff.fullName} (${staff.staffId})", style = MaterialTheme.typography.titleSmall)
                            Text("${staff.ordersHandledCount} orders handled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${staff.loyaltyPoints} pts", style = MonoPriceStyle, color = CorianderGreen)
                        IconButton(
                            onClick = { editingStaffLoyalty = staff },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit staff loyalty", tint = SmokedAmber)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Inventory Management",
                subtitle = "${inventory.count { it.isLowStock() }} low-stock items",
                trailing = {
                    Button(
                        onClick = { showAddInventoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Stock", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stock")
                    }
                }
            )
        }

        items(inventory, key = { "own_inv_${it.id}" }) { item ->
            InventoryControlCard(
                item = item,
                onAdjust = { delta -> viewModel.adjustInventory(item, delta) }
            )
        }
    }

    editingCustomerLoyalty?.let { target ->
        LoyaltyEditDialog(
            title = "Edit Customer Loyalty (${target.displayName})",
            currentPoints = target.loyaltyPoints,
            onDismiss = { editingCustomerLoyalty = null },
            onSave = { newPts, reason ->
                viewModel.editCustomerLoyalty(target, newPts, reason)
                editingCustomerLoyalty = null
            }
        )
    }

    editingStaffLoyalty?.let { target ->
        LoyaltyEditDialog(
            title = "Edit Staff Loyalty (${target.fullName})",
            currentPoints = target.loyaltyPoints,
            onDismiss = { editingStaffLoyalty = null },
            onSave = { newPts, reason ->
                viewModel.editStaffLoyalty(target, newPts, reason)
                editingStaffLoyalty = null
            }
        )
    }

    if (showAddInventoryDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(InventoryCategory.MEAT) }
        var qtyText by remember { mutableStateOf("20") }
        var unit by remember { mutableStateOf("kg") }
        var minText by remember { mutableStateOf("5") }
        var costText by remember { mutableStateOf("500") }

        AlertDialog(
            onDismissRequest = { showAddInventoryDialog = false },
            title = { Text("Add Inventory Item") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ingredient / Supply Name") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InventoryCategory.entries.forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.displayLabel) })
                        }
                    }
                    OutlinedTextField(value = qtyText, onValueChange = { qtyText = it.filter { c -> c.isDigit() } }, label = { Text("Initial Quantity") }, singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (kg, L, pcs)") }, singleLine = true)
                    OutlinedTextField(value = minText, onValueChange = { minText = it.filter { c -> c.isDigit() } }, label = { Text("Low-Stock Alert Threshold") }, singleLine = true)
                    OutlinedTextField(value = costText, onValueChange = { costText = it.filter { c -> c.isDigit() } }, label = { Text("Unit Cost (PKR)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addInventoryItem(
                            name = name,
                            category = category,
                            quantity = qtyText.toLongOrNull() ?: 0L,
                            unit = unit,
                            minThreshold = minText.toLongOrNull() ?: 5L,
                            unitCostPkr = costText.toLongOrNull() ?: 0L
                        )
                        showAddInventoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = { TextButton(onClick = { showAddInventoryDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LoyaltyEditDialog(
    title: String,
    currentPoints: Long,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var pointsText by remember { mutableStateOf(currentPoints.toString()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it.filter { c -> c.isDigit() } },
                    label = { Text("New Loyalty Points Balance") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Note for User") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = pointsText.toLongOrNull() ?: currentPoints
                    onSave(pts, reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
            ) {
                Text("Update Points", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

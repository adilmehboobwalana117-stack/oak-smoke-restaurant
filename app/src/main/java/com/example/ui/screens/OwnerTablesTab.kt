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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TableRestaurant
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DiningTable
import com.example.data.SeatingZone
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerTablesTab(viewModel: RestaurantViewModel) {
    val tables by viewModel.diningTables.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedZone by remember { mutableStateOf<SeatingZone?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<DiningTable?>(null) }
    var deletingTable by remember { mutableStateOf<DiningTable?>(null) }

    val filteredTables = remember(tables, selectedZone) {
        if (selectedZone == null) tables else tables.filter { it.zone == selectedZone?.name }
    }
    val totalSeats = tables.filter { it.available }.sumOf { it.capacity }
    val activeCount = tables.count { it.available }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Dining Tables & Cabins",
                subtitle = "${tables.size} tables ($activeCount active · $totalSeats seats)",
                trailing = {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                        modifier = Modifier.testTag("add_dining_table_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Table", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Table")
                    }
                }
            )
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedZone == null,
                    onClick = { selectedZone = null },
                    label = { Text("All (${tables.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmokedAmber,
                        selectedLabelColor = CharcoalDeep
                    )
                )
                SeatingZone.entries.forEach { zone ->
                    val count = tables.count { it.zone == zone.name }
                    FilterChip(
                        selected = selectedZone == zone,
                        onClick = { selectedZone = zone },
                        label = { Text("${zone.displayLabel} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmokedAmber,
                            selectedLabelColor = CharcoalDeep
                        )
                    )
                }
            }
        }

        if (tables.size in 1..24) {
            item {
                SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.7f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Expand Floor to 25 Tables",
                                style = MaterialTheme.typography.titleSmall,
                                color = SmokedAmber
                            )
                            Text(
                                text = "Currently ${tables.size}/25 tables. Tap to add remaining Charcoal Hall (15), Family Cabins (5) & Outdoor Lawn (5).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { viewModel.seedStarterTables() },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                            modifier = Modifier.testTag("load_all_25_tables_btn")
                        ) {
                            Text("Load 25", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (tables.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.TableRestaurant,
                        title = "No dining tables configured yet",
                        subtitle = "Add your Oak & Smoke tables, Family Cabins, and Outdoor Lawn tables so staff can pick them when taking orders."
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.seedStarterTables() },
                            enabled = !isBusy,
                            border = BorderStroke(1.dp, SmokedAmber),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seed_starter_tables_btn")
                        ) {
                            Text("Load Default Tables", color = SmokedAmber)
                        }
                        Button(
                            onClick = { showCreateDialog = true },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Custom Table", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredTables.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.TableRestaurant,
                    title = "No tables in this zone",
                    subtitle = "Tap + Table above to add a table or cabin to ${selectedZone?.displayLabel ?: "this zone"}."
                )
            }
        } else {
            items(filteredTables, key = { it.id }) { table ->
                SmokedCard(
                    borderColor = if (table.available) MaterialTheme.colorScheme.outline else CrimsonEmber.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(table.label, style = MaterialTheme.typography.titleMedium)
                                StatusPill(
                                    label = if (table.available) "ACTIVE" else "PAUSED",
                                    color = if (table.available) CorianderGreen else CrimsonEmber
                                )
                            }
                            Text(
                                text = "${SeatingZone.fromRaw(table.zone).displayLabel} · ${table.capacity} Guests",
                                style = MonoPriceStyle,
                                color = SmokedAmber
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = table.available,
                                onCheckedChange = { checked ->
                                    viewModel.toggleDiningTableAvailability(table, checked)
                                },
                                modifier = Modifier.testTag("toggle_table_${table.id}")
                            )
                            IconButton(
                                onClick = { editingTable = table },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("edit_table_${table.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${table.label}", tint = SmokedAmber)
                            }
                            IconButton(
                                onClick = { deletingTable = table },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_table_${table.id}")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${table.label}", tint = CrimsonEmber)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingTable != null) {
        DiningTableEditorDialog(
            existing = editingTable,
            existingLabels = tables.filter { it.id != editingTable?.id }.map { it.label },
            suggestedNextNumber = tables.size + 1,
            onDismiss = {
                showCreateDialog = false
                editingTable = null
            },
            onSave = { label, zone, capacity, available ->
                viewModel.saveDiningTable(
                    existingId = editingTable?.id,
                    label = label,
                    zone = zone,
                    capacity = capacity,
                    available = available
                )
                showCreateDialog = false
                editingTable = null
            }
        )
    }

    deletingTable?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingTable = null },
            title = { Text("Delete ${target.label}?") },
            text = {
                Text("This table will be removed from Staff POS and dining lists. Past orders and bills will remain unaffected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDiningTable(target)
                        deletingTable = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmber)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTable = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiningTableEditorDialog(
    existing: DiningTable?,
    existingLabels: List<String>,
    suggestedNextNumber: Int,
    onDismiss: () -> Unit,
    onSave: (label: String, zone: SeatingZone, capacity: Long, available: Boolean) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "Table $suggestedNextNumber") }
    var zone by remember { mutableStateOf(SeatingZone.fromRaw(existing?.zone)) }
    var capacityText by remember { mutableStateOf((existing?.capacity ?: 4L).toString()) }
    var available by remember { mutableStateOf(existing?.available ?: true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Dining Table / Cabin" else "Edit Table") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        errorText = null
                    },
                    label = { Text("Table / Cabin Name (e.g., Table 10, VIP Cabin 3)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_label_dialog_input")
                )

                Text("Seating Zone", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SeatingZone.entries.forEach { z ->
                        FilterChip(
                            selected = zone == z,
                            onClick = { zone = z },
                            label = { Text(z.displayLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = capacityText,
                    onValueChange = {
                        capacityText = it.filter { c -> c.isDigit() }
                        errorText = null
                    },
                    label = { Text("Seating Capacity (1 - 30 Guests)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_capacity_dialog_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = available, onCheckedChange = { available = it })
                    Text("Available for Staff POS & Dine-In")
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonEmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = label.trim()
                    val cap = capacityText.toLongOrNull() ?: 0L
                    val isDuplicate = existingLabels.any { it.equals(clean, ignoreCase = true) }
                    when {
                        clean.length !in 2..40 -> errorText = "Enter a table name (2–40 characters)."
                        isDuplicate -> errorText = "A table named \"$clean\" already exists."
                        cap !in 1L..30L -> errorText = "Capacity must be between 1 and 30 guests."
                        else -> onSave(clean, zone, cap, available)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                modifier = Modifier.testTag("save_table_dialog_btn")
            ) {
                Text(if (existing == null) "Add Table" else "Save Table", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

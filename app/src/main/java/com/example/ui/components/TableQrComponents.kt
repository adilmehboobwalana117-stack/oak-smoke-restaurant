package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DiningTable
import com.example.data.OakAndSmokeSeeds
import com.example.data.SeatingZone
import com.example.services.AppPwaQrService
import com.example.services.TableQrService
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.SmokedAmber

/**
 * Full-screen/BottomSheet QR code detail card for ONE specific Oak & Smoke dining table.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableQrDetailSheet(
    table: DiningTable,
    configPublicUrl: String,
    onDismiss: () -> Unit,
    onFeedback: (String) -> Unit,
    onOrderForThisTable: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val qrUrl = remember(table.id, table.label, configPublicUrl) {
        TableQrService.buildTableOrderUrl(context, table, configPublicUrl)
    }
    val zoneLabel = SeatingZone.fromRaw(table.zone).displayLabel

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${table.label} · Unique Table QR",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$zoneLabel · ${table.capacity} Guests",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmokedAmber
                    )
                }
                StatusPill(
                    label = if (table.available) "ACTIVE" else "PAUSED",
                    color = if (table.available) CorianderGreen else CrimsonEmber
                )
            }

            SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.7f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScannableQrCodeBox(
                        content = qrUrl,
                        size = 224.dp,
                        padding = 14.dp,
                        modifier = Modifier.testTag("single_table_qr_${table.id}")
                    )
                    Text(
                        text = "Scan to Order Directly from ${table.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = SmokedAmber,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = qrUrl,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "Place this QR card on ${table.label}. Guests scan with any phone camera to open the menu locked directly to ${table.label} — no waiter delay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    TableQrService.printSingleTableQrPoster(context, table, qrUrl)
                        .onSuccess { onFeedback("Print preview opened for ${table.label} Table Tent QR.") }
                        .onFailure { err -> onFeedback(err.message ?: "Unable to print QR card.") }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("print_single_table_qr_btn")
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Table QR Tent", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Print ${table.label} QR Tent (A5)", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        TableQrService.shareTableQrLink(context, table, qrUrl)
                            .onFailure { onFeedback("Unable to open Share sheet.") }
                    },
                    border = BorderStroke(1.dp, CorianderGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_table_qr_btn")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share Table Link", tint = CorianderGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Link", color = CorianderGreen)
                }
                OutlinedButton(
                    onClick = {
                        AppPwaQrService.copyLinkToClipboard(context, qrUrl)
                            .onSuccess { onFeedback("${table.label} QR link copied to clipboard.") }
                            .onFailure { onFeedback("Unable to copy link.") }
                    },
                    border = BorderStroke(1.dp, EmberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_table_qr_btn")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Table Link", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Link")
                }
            }

            if (onOrderForThisTable != null) {
                OutlinedButton(
                    onClick = {
                        onOrderForThisTable(table.label)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, SmokedAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_order_from_table_btn")
                ) {
                    Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Menu as ${table.label} (Test Order)", color = SmokedAmber, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Studio modal displaying unique live scannable QR codes for all 25 Oak & Smoke tables
 * with zone filters and 1-tap batch PDF printing (4 table tent cards per A4 page).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllTablesQrStudioSheet(
    tables: List<DiningTable>,
    configPublicUrl: String,
    onDismiss: () -> Unit,
    onFeedback: (String) -> Unit,
    onSelectTableForDetail: (DiningTable) -> Unit
) {
    val context = LocalContext.current
    val resolvedTables = remember(tables) {
        if (tables.isNotEmpty()) tables else OakAndSmokeSeeds.starterTables
    }
    var selectedZone by remember { mutableStateOf<SeatingZone?>(null) }
    val filtered = remember(resolvedTables, selectedZone) {
        if (selectedZone == null) resolvedTables else resolvedTables.filter { it.zone == selectedZone?.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${resolvedTables.size} Unique Table QR Codes",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Each table has its own direct-order QR · Tap any card to enlarge or print",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = "${filtered.size} QRs", color = SmokedAmber)
            }

            Button(
                onClick = {
                    TableQrService.printAllTableQrCardsPdf(context, filtered, configPublicUrl)
                        .onSuccess {
                            onFeedback("Preparing printable PDF sheet with ${filtered.size} unique Table QR cards (4 per A4 page)!")
                        }
                        .onFailure { err ->
                            onFeedback(err.message ?: "Unable to print Table QR batch PDF.")
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("print_all_25_table_qrs_btn")
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print all Table QR cards", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (selectedZone == null) "Print All ${filtered.size} Table QR Cards (PDF)" else "Print ${filtered.size} ${selectedZone!!.displayLabel} QRs",
                    fontWeight = FontWeight.Bold
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedZone == null,
                    onClick = { selectedZone = null },
                    label = { Text("All (${resolvedTables.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmokedAmber,
                        selectedLabelColor = CharcoalDeep
                    )
                )
                SeatingZone.entries.forEach { zone ->
                    val count = resolvedTables.count { it.zone == zone.name }
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

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered, key = { it.id }) { table ->
                    val qrUrl = remember(table.id, table.label, configPublicUrl) {
                        TableQrService.buildTableOrderUrl(context, table, configPublicUrl)
                    }
                    SmokedCard(
                        borderColor = SmokedAmber.copy(alpha = 0.55f),
                        modifier = Modifier
                            .clickable { onSelectTableForDetail(table) }
                            .testTag("table_qr_grid_card_${table.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ScannableQrCodeBox(
                                content = qrUrl,
                                size = 112.dp,
                                padding = 6.dp
                            )
                            Text(
                                text = table.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = SmokedAmber,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${SeatingZone.fromRaw(table.zone).displayLabel} · ${table.capacity}p",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Customer-facing Table QR Selector / Link Parser Sheet:
 * Lets dine-in customers check into their exact table by pasting a scanned Table QR link,
 * entering their table number (1–25), or tapping their table across all 3 zones.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectOrScanTableSheet(
    tables: List<DiningTable>,
    currentTableLabel: String?,
    onDismiss: () -> Unit,
    onSelectTable: (String) -> Unit,
    onClearTable: () -> Unit
) {
    val resolvedTables = remember(tables) {
        val active = tables.filter { it.available }
        if (active.isNotEmpty()) active else OakAndSmokeSeeds.starterTables
    }
    var qrOrTableInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = SmokedAmber)
                    Text("Order from Your Table", style = MaterialTheme.typography.titleLarge)
                }
                if (currentTableLabel != null) {
                    StatusPill(label = currentTableLabel, color = CorianderGreen)
                }
            }

            Text(
                text = "Point your phone camera at the Oak & Smoke QR card on your table to auto-select it, or enter your table number / pick your table below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = qrOrTableInput,
                    onValueChange = { qrOrTableInput = it; parseError = null },
                    label = { Text("Table # or Scanned QR Link") },
                    placeholder = { Text("e.g., 7 or Table 7") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("table_qr_manual_input")
                )
                Button(
                    onClick = {
                        val parsed = TableQrService.parseTableFromRawInput(qrOrTableInput, resolvedTables)
                        if (parsed != null) {
                            onSelectTable(parsed.tableLabel)
                            onDismiss()
                        } else {
                            parseError = "Could not match that table. Enter a number 1–${resolvedTables.size} or pick below."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("apply_table_qr_btn")
                ) {
                    Text("Check In", fontWeight = FontWeight.Bold)
                }
            }

            if (parseError != null) {
                Text(parseError!!, style = MaterialTheme.typography.bodySmall, color = CrimsonEmber)
            }

            HorizontalDivider(color = EmberBorder)

            SeatingZone.entries.forEach { zone ->
                val zoneTables = resolvedTables.filter { it.zone == zone.name }
                if (zoneTables.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TableRestaurant,
                            contentDescription = null,
                            tint = SmokedAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${zone.displayLabel} (${zoneTables.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = SmokedAmber
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        zoneTables.forEach { tbl ->
                            val isSelected = currentTableLabel.equals(tbl.label, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onSelectTable(tbl.label)
                                    onDismiss()
                                },
                                label = { Text("${tbl.label} (${tbl.capacity}p)") },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmokedAmber,
                                    selectedLabelColor = CharcoalDeep
                                ),
                                modifier = Modifier.testTag("pick_table_chip_${tbl.id}")
                            )
                        }
                    }
                }
            }

            if (currentTableLabel != null) {
                HorizontalDivider(color = EmberBorder)
                OutlinedButton(
                    onClick = {
                        onClearTable()
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, CrimsonEmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("switch_back_to_delivery_btn")
                ) {
                    Text("Switch Back to COD Delivery Mode", color = CrimsonEmber)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

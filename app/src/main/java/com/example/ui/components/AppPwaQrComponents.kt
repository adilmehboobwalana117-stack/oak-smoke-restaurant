package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.services.AppDownloadTarget
import com.example.services.AppPwaQrService
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.SmokedAmber

/**
 * Renders a crisp, ISO/IEC 18004 scannable QR Code directly on Compose Canvas
 * framed inside a high-contrast white quiet-zone container.
 */
@Composable
fun ScannableQrCodeBox(
    content: String,
    size: Dp,
    modifier: Modifier = Modifier,
    padding: Dp = 6.dp
) {
    val matrix = remember(content) { AppPwaQrService.encodeQrMatrix(content) }
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, SmokedAmber.copy(alpha = 0.85f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (matrix != null) {
                Canvas(modifier = Modifier.size(size - (padding * 2))) {
                    val cols = matrix.width.coerceAtLeast(1)
                    val rows = matrix.height.coerceAtLeast(1)
                    val cellW = this.size.width / cols
                    val cellH = this.size.height / rows
                    for (y in 0 until rows) {
                        for (x in 0 until cols) {
                            if (matrix.get(x, y)) {
                                drawRect(
                                    color = Color(0xFF14110F),
                                    topLeft = Offset(x * cellW, y * cellH),
                                    size = Size(cellW + 0.5f, cellH + 0.5f)
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = "Oak & Smoke QR Code",
                    tint = Color.Black,
                    modifier = Modifier.size(size * 0.65f)
                )
            }
        }
    }
}

/**
 * Compact TopAppBar QR & PWA trigger button (48dp touch target) always reachable at the top of the app.
 */
@Composable
fun TopBarQrPwaActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag("topbar_qr_pwa_btn")
    ) {
        Icon(
            imageVector = Icons.Filled.QrCode2,
            contentDescription = "Open App QR Code and Download Link",
            tint = SmokedAmber
        )
    }
}

/**
 * Top-of-App Public Download & Scannable QR Strip rendered after app installation.
 * Displays a live scannable mini QR code + direct download trigger right at the top of the app.
 */
@Composable
fun TopInstalledPwaQrStrip(
    visible: Boolean,
    onOpenQrModal: () -> Unit,
    onDownloadPwa: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    qrUrl: String = ""
) {
    val context = LocalContext.current
    val isInstalled = remember(context) { AppPwaQrService.isAppInstalled(context) }
    val resolvedQrUrl = remember(qrUrl, context) {
        if (AppPwaQrService.isValidHttpsUrl(qrUrl)) {
            qrUrl.trim()
        } else {
            AppPwaQrService.resolveTargetUrl(context, AppDownloadTarget.PUBLIC_DOWNLOAD)
        }
    }

    AnimatedVisibility(
        visible = visible && isInstalled,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val stripSurface = MaterialTheme.colorScheme.surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("top_pwa_qr_strip"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SmokedAmber.copy(alpha = 0.5f)),
            color = stripSurface,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to CharcoalDeep.copy(alpha = 0.95f),
                            0.65f to stripSurface,
                            1.0f to SmokedAmber.copy(alpha = 0.14f)
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Scannable Mini QR Code right on top of the app (tap to enlarge / print)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenQrModal)
                        .testTag("top_qr_code_preview")
                ) {
                    ScannableQrCodeBox(
                        content = resolvedQrUrl,
                        size = 58.dp,
                        padding = 3.dp
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenQrModal),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Public QR Active",
                            tint = CorianderGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.pwa_banner_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmokedAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(R.string.pwa_banner_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onDownloadPwa,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmokedAmber,
                        contentColor = CharcoalDeep
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("top_pwa_download_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.InstallMobile,
                        contentDescription = "Scan or Download App",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.pwa_download_cta),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_top_pwa_qr_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Hide QR and download strip",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full Scannable QR Code & Public App Download Sheet allowing anyone to:
 * - Scan the live public QR code immediately with any smartphone camera to download the app
 * - Switch between Public App (Play Store / Owner Custom Link), PC & Web PWA, and Tester Opt-In
 * - Open, share, copy, pin Home Screen shortcut, or print a Table-Tent QR Poster
 * - (Owner/Manager) Customize the live public download link synced across all devices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PwaDownloadAndQrModal(
    onDismiss: () -> Unit,
    onFeedback: (String) -> Unit,
    configPublicDownloadUrl: String = "",
    canEditDownloadUrl: Boolean = false,
    onSaveCustomDownloadUrl: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTarget by remember { mutableStateOf(AppDownloadTarget.PUBLIC_DOWNLOAD) }
    var pwaActivated by remember { mutableStateOf(AppPwaQrService.hasPinnedOrDownloadedPwa(context)) }
    var showCustomUrlEditor by remember { mutableStateOf(false) }
    var customUrlInput by remember(configPublicDownloadUrl) {
        mutableStateOf(
            configPublicDownloadUrl.ifBlank { AppPwaQrService.getCachedPublicDownloadUrl(context) }
        )
    }

    val activeUrl = remember(selectedTarget, configPublicDownloadUrl, customUrlInput) {
        AppPwaQrService.resolveTargetUrl(context, selectedTarget, configPublicDownloadUrl)
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pwa_qr_modal_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Oak & Smoke Restaurant Hasilpur",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmokedAmber
                    )
                }
                StatusPill(
                    label = if (pwaActivated) "SCAN READY" else "PUBLIC QR",
                    color = CorianderGreen
                )
            }

            // Target selector: Public App (Default), PC & Web App, or Play Tester Opt-In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppDownloadTarget.entries.forEach { target ->
                    FilterChip(
                        selected = selectedTarget == target,
                        onClick = { selectedTarget = target },
                        label = {
                            Text(
                                text = target.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmokedAmber,
                            selectedLabelColor = CharcoalDeep
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_target_${target.name.lowercase()}")
                    )
                }
            }

            // Large Scannable QR Code Card with high-contrast quiet zone
            SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.65f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScannableQrCodeBox(
                        content = activeUrl,
                        size = 224.dp,
                        padding = 14.dp,
                        modifier = Modifier.testTag("modal_large_qr_code")
                    )
                    Text(
                        text = "Point any phone camera to scan & download",
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
                            text = activeUrl,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = selectedTarget.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Primary Open / Download Button
            Button(
                onClick = {
                    AppPwaQrService.openPwaDownload(context, activeUrl)
                        .onSuccess {
                            pwaActivated = true
                            onFeedback("Opening ${selectedTarget.label} link in browser.")
                        }
                        .onFailure {
                            onFeedback("Unable to open browser for ${selectedTarget.label}.")
                        }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmokedAmber,
                    contentColor = CharcoalDeep
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("modal_download_pwa_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Open Download Link",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (selectedTarget) {
                        AppDownloadTarget.PUBLIC_DOWNLOAD -> "Open Public App Download Link"
                        AppDownloadTarget.WEB_PWA -> stringResource(R.string.pwa_qr_open_browser)
                        AppDownloadTarget.PLAY_TESTER -> "Open Play Tester Opt-In Link"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary actions: Pin Shortcut + Share Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AppPwaQrService.pinAppShortcut(context)
                            .onSuccess { supported ->
                                if (supported) {
                                    pwaActivated = true
                                    onFeedback("Home Screen shortcut prompt sent for Oak & Smoke.")
                                } else {
                                    onFeedback("Launcher does not support pinned shortcuts; use the QR or download link.")
                                }
                            }
                            .onFailure {
                                onFeedback("Could not pin Home Screen shortcut on this device.")
                            }
                    },
                    border = BorderStroke(1.dp, SmokedAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("modal_pin_shortcut_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddToHomeScreen,
                        contentDescription = "Add Home Shortcut",
                        tint = SmokedAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pwa_qr_pin_shortcut),
                        color = SmokedAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        AppPwaQrService.shareAppLink(context, activeUrl)
                            .onFailure { onFeedback("Unable to open Share sheet.") }
                    },
                    border = BorderStroke(1.dp, CorianderGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("modal_share_pwa_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Download Link",
                        tint = CorianderGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pwa_qr_share_link),
                        color = CorianderGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Copy Link + Print Table QR Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AppPwaQrService.copyLinkToClipboard(context, activeUrl)
                            .onSuccess { onFeedback("Public download link copied to clipboard.") }
                            .onFailure { onFeedback("Could not copy link.") }
                    },
                    border = BorderStroke(1.dp, EmberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("modal_copy_pwa_link_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy Link",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pwa_qr_copy_link),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        AppPwaQrService.printTableQrPoster(context, activeUrl, selectedTarget.label)
                            .onSuccess { onFeedback("Print preview opened for Table QR Poster.") }
                            .onFailure { err -> onFeedback(err.message ?: "Print failed.") }
                    },
                    border = BorderStroke(1.dp, EmberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("modal_print_qr_card_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Print,
                        contentDescription = "Print Table QR Poster",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pwa_qr_print_poster),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            // Owner / Manager Live Public Download URL Override
            if (canEditDownloadUrl && onSaveCustomDownloadUrl != null) {
                SmokedCard(borderColor = EmberBorder) {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Public QR Link (Admin)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SmokedAmber
                                )
                                Text(
                                    text = "Point the public QR to your Google Play URL, GitHub Pages PWA, or hosted APK link.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showCustomUrlEditor = !showCustomUrlEditor },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_custom_qr_url_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit custom public QR download URL",
                                    tint = SmokedAmber
                                )
                            }
                        }

                        AnimatedVisibility(visible = showCustomUrlEditor) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customUrlInput,
                                    onValueChange = { customUrlInput = it },
                                    label = { Text("Public HTTPS Download URL") },
                                    placeholder = { Text("https://play.google.com/store/apps/details?id=...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_qr_url_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            customUrlInput = ""
                                            AppPwaQrService.cachePublicDownloadUrl(context, "")
                                            onSaveCustomDownloadUrl("")
                                            selectedTarget = AppDownloadTarget.PUBLIC_DOWNLOAD
                                            showCustomUrlEditor = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("Reset Default")
                                    }
                                    Button(
                                        onClick = {
                                            val cleaned = customUrlInput.trim()
                                            if (cleaned.isNotEmpty() && !AppPwaQrService.isValidHttpsUrl(cleaned)) {
                                                onFeedback("Please enter a valid https:// download link.")
                                            } else {
                                                AppPwaQrService.cachePublicDownloadUrl(context, cleaned)
                                                onSaveCustomDownloadUrl(cleaned)
                                                selectedTarget = AppDownloadTarget.PUBLIC_DOWNLOAD
                                                showCustomUrlEditor = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SmokedAmber,
                                            contentColor = CharcoalDeep
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("save_custom_qr_url_btn")
                                    ) {
                                        Text("Update QR", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = EmberBorder)

            Text(
                text = "Print or display this QR code on Oak & Smoke tables & counter receipts so anyone can scan and download the app immediately on Android or PC.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

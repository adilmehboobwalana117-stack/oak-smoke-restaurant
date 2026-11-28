package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AuthManager
import com.example.data.CartLine
import com.example.data.MenuCategory
import com.example.data.MenuItem
import com.example.data.OakAndSmokeSeeds
import com.example.data.SeatingZone
import com.example.data.SpiceLevel
import com.example.data.cartItemKey
import com.example.data.formatPkr
import com.example.data.parsedVarieties
import com.example.data.resolveCartLines
import com.example.services.AppDownloadTarget
import com.example.services.AppPwaQrService
import com.example.ui.components.AppScreenshotsGallerySheet
import com.example.ui.components.PrivacyAndScreenshotsCard
import com.example.ui.components.PrivacyPolicySheet
import com.example.ui.components.PwaDownloadAndQrModal
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectOrScanTableSheet
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.components.TopBarQrPwaActionButton
import com.example.ui.components.TopInstalledPwaQrStrip
import com.example.ui.components.resolveDishDrawable
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import kotlinx.coroutines.launch

private data class GuestNavDest(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Guest Customer Shell — strictly restricted to the Customer domain (Menu, Daily Specials,
 * local COD Cart preview, and Dining Seating overview). Zero Firestore reads/writes and zero
 * Staff, Rider, or Owner controls are exposed until the user signs in with Google.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestCustomerShell(
    cartQuantities: Map<String, Int>,
    activeTableLabel: String? = null,
    onSelectTable: (String?) -> Unit = {},
    onIncrementCart: (String) -> Unit,
    onDecrementCart: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf("guest_menu") }
    var isSigningIn by remember { mutableStateOf(false) }
    var lastAuthError by remember { mutableStateOf<String?>(null) }
    var showGuestCheckoutSheet by remember { mutableStateOf(false) }
    var showTableSelectorSheet by remember { mutableStateOf(false) }
    var showTopPwaQrStrip by remember { mutableStateOf(true) }
    var showPwaQrModal by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showScreenshotsGallery by remember { mutableStateOf(false) }

    // Silent auto sign-in for returning Google users (never blocks guest browsing)
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay open in Guest Customer mode */ },
            scope = scope
        )
    }

    fun launchGoogleSignIn() {
        if (isSigningIn) return
        isSigningIn = true
        lastAuthError = null
        AuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isSigningIn = false
                lastAuthError = null
                showGuestCheckoutSheet = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isSigningIn = false
                lastAuthError = msg
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            scope = scope,
            onAuthCancelled = {
                isSigningIn = false
            }
        )
    }

    val destinations = remember {
        listOf(
            GuestNavDest("guest_menu", "Menu", Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu),
            GuestNavDest("guest_tables", "Tables", Icons.Filled.TableRestaurant, Icons.Outlined.TableRestaurant),
            GuestNavDest("guest_account", "Sign In", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val catalogMenu = OakAndSmokeSeeds.starterMenu
    val cartLines = remember(cartQuantities) {
        resolveCartLines(catalogMenu, cartQuantities)
    }

    BackHandler(enabled = selectedTab != "guest_menu") {
        selectedTab = "guest_menu"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp
        val maxContentWidth = if (maxWidth >= 840.dp) 1280.dp else 860.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Oak & Smoke",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (activeTableLabel != null) "Seated at $activeTableLabel · Table QR Mode" else "Hasilpur · Open Public Menu",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeTableLabel != null) CorianderGreen else SmokedAmber
                            )
                        }
                    },
                    actions = {
                        TopBarQrPwaActionButton(
                            onClick = { showPwaQrModal = true }
                        )
                        OutlinedButton(
                            onClick = { launchGoogleSignIn() },
                            enabled = !isSigningIn,
                            border = BorderStroke(1.dp, SmokedAmber),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .testTag("guest_topbar_sign_in_btn")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = SmokedAmber,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Sign in with Google",
                                    tint = SmokedAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Sign In", color = SmokedAmber, style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        destinations.forEach { dest ->
                            val selected = selectedTab == dest.id
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = dest.id },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CharcoalDeep,
                                    selectedTextColor = SmokedAmber,
                                    indicatorColor = SmokedAmber
                                ),
                                modifier = Modifier.testTag("nav_item_${dest.id}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEach { dest ->
                            val selected = selectedTab == dest.id
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = dest.id },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CharcoalDeep,
                                    selectedTextColor = SmokedAmber,
                                    indicatorColor = SmokedAmber
                                ),
                                modifier = Modifier.testTag("nav_rail_${dest.id}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activePublicQrUrl = remember(context) {
                        AppPwaQrService.resolveTargetUrl(context, AppDownloadTarget.PUBLIC_DOWNLOAD)
                    }
                    TopInstalledPwaQrStrip(
                        visible = showTopPwaQrStrip,
                        qrUrl = activePublicQrUrl,
                        onOpenQrModal = { showPwaQrModal = true },
                        onDownloadPwa = { showPwaQrModal = true },
                        onDismiss = { showTopPwaQrStrip = false },
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = maxContentWidth)
                    ) {
                        when (selectedTab) {
                            "guest_menu" -> GuestMenuTab(
                                catalog = catalogMenu,
                                cartLines = cartLines,
                                cartQuantities = cartQuantities,
                                activeTableLabel = activeTableLabel,
                                onOpenTableSelector = { showTableSelectorSheet = true },
                                onClearTable = { onSelectTable(null) },
                                onIncrementCart = onIncrementCart,
                                onDecrementCart = onDecrementCart,
                                onOpenCheckout = { showGuestCheckoutSheet = true },
                                onOpenQrModal = { showPwaQrModal = true },
                                onRequestSignIn = { launchGoogleSignIn() }
                            )
                            "guest_tables" -> GuestTablesPreviewTab(
                                activeTableLabel = activeTableLabel,
                                isSigningIn = isSigningIn,
                                onCheckInTable = { label ->
                                    onSelectTable(label)
                                    selectedTab = "guest_menu"
                                    scope.launch { snackbarHostState.showSnackbar("Checked into $label! Pick dishes to order directly to Kitchen.") }
                                },
                                onRequestSignIn = { launchGoogleSignIn() }
                            )
                            else -> GuestAccountSignInTab(
                                isSigningIn = isSigningIn,
                                authError = lastAuthError,
                                onRequestSignIn = { launchGoogleSignIn() },
                                onOpenPrivacyPolicy = { showPrivacyPolicy = true },
                                onOpenScreenshots = { showScreenshotsGallery = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGuestCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGuestCheckoutSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val subtotal = cartLines.sumOf { it.unitPricePkr * it.quantity }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeTableLabel != null) "$activeTableLabel · Direct Order" else "Guest COD Cart Preview",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    StatusPill(
                        label = if (activeTableLabel != null) "DINE-IN" else "COD ONLY",
                        color = CorianderGreen
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmberBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cartLines.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${line.quantity}x ${line.displayNameWithVariety()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatPkr(line.unitPricePkr * line.quantity),
                                    style = MonoPriceStyle,
                                    color = SmokedAmber
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = EmberBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (activeTableLabel != null) "Pay at Counter / Table" else "Estimated COD Total",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(formatPkr(subtotal), style = MonoPriceStyle, color = if (activeTableLabel != null) CorianderGreen else SmokedAmber)
                }

                Text(
                    text = if (activeTableLabel != null) {
                        "Ordering directly for $activeTableLabel. Sign in with Google in 1 tap to send your order straight to the Oak & Smoke Kitchen — your cart & table stay saved."
                    } else {
                        "Sign in with Google to attach your delivery GPS pin for our Rider, place this Cash on Delivery order, and track live kitchen updates. Your cart items will stay saved."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { launchGoogleSignIn() },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("guest_checkout_google_sign_in_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTableLabel != null) CorianderGreen else SmokedAmber,
                        contentColor = CharcoalDeep
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CharcoalDeep, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (activeTableLabel != null) "Sign in with Google · Send to $activeTableLabel" else "Sign in with Google to Place COD Order",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showTableSelectorSheet) {
        SelectOrScanTableSheet(
            tables = OakAndSmokeSeeds.starterTables,
            currentTableLabel = activeTableLabel,
            onDismiss = { showTableSelectorSheet = false },
            onSelectTable = { label ->
                onSelectTable(label)
                scope.launch { snackbarHostState.showSnackbar("Seated at $label — add dishes to order directly to your table.") }
            },
            onClearTable = { onSelectTable(null) }
        )
    }

    if (showPwaQrModal) {
        PwaDownloadAndQrModal(
            onDismiss = { showPwaQrModal = false },
            onFeedback = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicySheet(onDismiss = { showPrivacyPolicy = false })
    }

    if (showScreenshotsGallery) {
        AppScreenshotsGallerySheet(onDismiss = { showScreenshotsGallery = false })
    }
}

@Composable
private fun GuestMenuTab(
    catalog: List<MenuItem>,
    cartLines: List<CartLine>,
    cartQuantities: Map<String, Int>,
    activeTableLabel: String?,
    onOpenTableSelector: () -> Unit,
    onClearTable: () -> Unit,
    onIncrementCart: (String) -> Unit,
    onDecrementCart: (String) -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenQrModal: () -> Unit,
    onRequestSignIn: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val specials = remember(catalog) { catalog.filter { it.dailySpecial && it.available } }
    val filteredItems by remember(catalog, selectedFilter, searchQuery) {
        derivedStateOf {
            catalog.filter { dish ->
                val matchesCat = when (selectedFilter) {
                    "ALL" -> true
                    "SPECIALS" -> dish.dailySpecial
                    else -> dish.category == selectedFilter
                }
                val matchesQuery = searchQuery.isBlank() ||
                    dish.name.contains(searchQuery, ignoreCase = true) ||
                    dish.description.contains(searchQuery, ignoreCase = true)
                matchesCat && matchesQuery
            }
        }
    }

    val cartItemCount = cartLines.sumOf { it.quantity }
    val cartSubtotal = cartLines.sumOf { it.unitPricePkr * it.quantity }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (cartItemCount > 0) 96.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1795434279848),
                        contentDescription = "Oak & Smoke Restaurant Hasilpur",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to CharcoalDeep.copy(alpha = 0.92f),
                                    0.65f to CharcoalDeep.copy(alpha = 0.55f),
                                    1.0f to CharcoalDeep.copy(alpha = 0.2f)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(label = "OPEN PUBLIC ACCESS · FREE TO BROWSE", color = SmokedAmber)
                            StatusPill(label = "COD ONLY", color = CorianderGreen)
                        }
                        Column {
                            Text(
                                text = "Oak & Smoke",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Hasilpur's Authentic Karahi, Smoked BBQ & Platters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmokedAmber
                            )
                        }
                    }
                }
            }

            // Direct Table QR Check-In Banner
            item {
                if (activeTableLabel != null) {
                    SmokedCard(borderColor = CorianderGreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TableRestaurant,
                                contentDescription = "Active table",
                                tint = CorianderGreen,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Seated at $activeTableLabel",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CorianderGreen
                                )
                                Text(
                                    text = "Table QR mode active. Pick dishes & tap Order to Table to send straight to Kitchen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(
                                    onClick = onOpenTableSelector,
                                    modifier = Modifier.testTag("guest_change_table_btn")
                                ) {
                                    Text("Change", color = SmokedAmber)
                                }
                                TextButton(
                                    onClick = onClearTable,
                                    modifier = Modifier.testTag("guest_clear_table_btn")
                                ) {
                                    Text("Delivery", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    SmokedCard(borderColor = CorianderGreen.copy(alpha = 0.55f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Order from table",
                                tint = CorianderGreen,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Seated at a Table? Order Direct",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CorianderGreen
                                )
                                Text(
                                    text = "Scan your table's QR card or pick Table 1–25 to order directly from your seat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenTableSelector,
                                border = BorderStroke(1.dp, CorianderGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("guest_pick_table_btn")
                            ) {
                                Text("Pick Table", color = CorianderGreen, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Open Public Access Banner with instant QR Share & 1-Tap Sign In
            item {
                SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.55f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Open Public Access",
                                tint = SmokedAmber,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open Public Menu — No Login Needed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SmokedAmber
                                )
                                Text(
                                    text = "Browse all dishes, 4 varieties & 25 tables freely. Share the QR code for easy download, or sign in in 1 tap when placing a live COD order.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenQrModal,
                                border = BorderStroke(1.dp, CorianderGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("public_banner_qr_btn")
                            ) {
                                Text("Scan / Share QR", color = CorianderGreen, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onRequestSignIn,
                                border = BorderStroke(1.dp, SmokedAmber),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("public_banner_sign_in_btn")
                            ) {
                                Text("1-Tap Sign In", color = SmokedAmber, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            if (specials.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Today's Daily Specials",
                        subtitle = "Fresh off the oak charcoal grill"
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(specials, key = { "g_sp_${it.id}" }) { special ->
                            val firstVariety = remember(special.id, special.varietiesDetail, special.pricePkr) {
                                special.parsedVarieties().first()
                            }
                            val defaultKey = cartItemKey(special.id, firstVariety.label)
                            val totalSpecialQty = cartLines.filter { it.item.id == special.id }.sumOf { it.quantity }
                            GuestSpecialCard(
                                dish = special,
                                qty = totalSpecialQty,
                                onAdd = { onIncrementCart(defaultKey) }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Mutton Karahi, Seekh Kebabs, Platters...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search menu") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guest_menu_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("ALL" to "All Menu", "SPECIALS" to "Daily Specials") +
                        MenuCategory.entries.map { it.name to it.displayLabel }
                    items(chips) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                    }
                }
            }

            items(filteredItems, key = { "g_dish_${it.id}" }) { dish ->
                GuestDishCard(
                    dish = dish,
                    cartQuantities = cartQuantities,
                    onIncrementKey = onIncrementCart,
                    onDecrementKey = onDecrementCart
                )
            }
        }

        if (cartItemCount > 0) {
            val isTableMode = activeTableLabel != null
            val barColor = if (isTableMode) CorianderGreen else SmokedAmber
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = barColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CharcoalDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTableMode) Icons.Filled.TableRestaurant else Icons.Filled.ShoppingBag,
                                contentDescription = "Cart items",
                                tint = barColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isTableMode) "$cartItemCount items · $activeTableLabel (Dine-In)" else "$cartItemCount items · Cash on Delivery",
                                style = MaterialTheme.typography.labelMedium,
                                color = CharcoalDeep
                            )
                            Text(
                                text = formatPkr(cartSubtotal),
                                style = MonoPriceStyle,
                                color = CharcoalDeep
                            )
                        }
                    }
                    Button(
                        onClick = onOpenCheckout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CharcoalDeep,
                            contentColor = barColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("guest_open_checkout_btn")
                    ) {
                        Text(if (isTableMode) "Order to Table" else "Checkout COD")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestSpecialCard(
    dish: MenuItem,
    qty: Int,
    onAdd: () -> Unit
) {
    SmokedCard(
        modifier = Modifier.width(240.dp),
        borderColor = SmokedAmber.copy(alpha = 0.55f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            Image(
                painter = painterResource(id = resolveDishDrawable(dish.imageKey)),
                contentDescription = dish.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(50),
                color = TandoorCopper
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Daily Special",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "SPECIAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = dish.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatPkr(dish.pricePkr), style = MonoPriceStyle, color = SmokedAmber)
                Button(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    Text(if (qty > 0) "In Cart ($qty)" else "+ Add")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuestDishCard(
    dish: MenuItem,
    cartQuantities: Map<String, Int>,
    onIncrementKey: (String) -> Unit,
    onDecrementKey: (String) -> Unit
) {
    val varieties = remember(dish.id, dish.varietiesDetail, dish.pricePkr) { dish.parsedVarieties() }
    var selectedVarietyIdx by remember(dish.id, varieties.size) { mutableIntStateOf(0) }
    val activeVariety = varieties.getOrElse(selectedVarietyIdx) { varieties.first() }
    val activeKey = cartItemKey(dish.id, activeVariety.label)
    val quantity = cartQuantities[activeKey] ?: 0

    SmokedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = resolveDishDrawable(dish.imageKey)),
                    contentDescription = dish.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${MenuCategory.fromRaw(dish.category).displayLabel} · ${SpiceLevel.fromRaw(dish.spiceLevel).displayLabel} · ${dish.prepMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmokedAmber
                    )
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dish.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatPkr(activeVariety.pricePkr), style = MonoPriceStyle, color = SmokedAmber)
                        if (quantity == 0) {
                            Button(
                                onClick = { onIncrementKey(activeKey) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep),
                                modifier = Modifier.testTag("guest_add_dish_${dish.id}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add ${dish.name}", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onDecrementKey(activeKey) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease ${dish.name}", tint = SmokedAmber)
                                }
                                Text("$quantity", style = MonoPriceStyle)
                                IconButton(onClick = { onIncrementKey(activeKey) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase ${dish.name}", tint = SmokedAmber)
                                }
                            }
                        }
                    }
                }
            }

            if (varieties.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    varieties.forEachIndexed { idx, variety ->
                        val vQty = cartQuantities[cartItemKey(dish.id, variety.label)] ?: 0
                        FilterChip(
                            selected = selectedVarietyIdx == idx,
                            onClick = { selectedVarietyIdx = idx },
                            label = {
                                val badge = if (vQty > 0) " ($vQty)" else ""
                                Text(
                                    text = "${variety.label} · ${formatPkr(variety.pricePkr)}$badge",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuestTablesPreviewTab(
    activeTableLabel: String?,
    isSigningIn: Boolean,
    onCheckInTable: (String) -> Unit,
    onRequestSignIn: () -> Unit
) {
    val starterTables = OakAndSmokeSeeds.starterTables
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "25 Tables · Tap Any Table to Order Direct",
                subtitle = "Oak & Smoke Restaurant Hasilpur · Scan Table QR or tap your table below"
            )
        }

        items(SeatingZone.entries) { zone ->
            val zoneTables = starterTables.filter { it.zone == zone.name }
            SmokedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(zone.displayLabel, style = MaterialTheme.typography.titleMedium, color = SmokedAmber)
                        StatusPill(label = "${zoneTables.size} Tables", color = CorianderGreen)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        zoneTables.forEach { tbl ->
                            val isSelected = activeTableLabel.equals(tbl.label, ignoreCase = true)
                            Surface(
                                color = if (isSelected) CorianderGreen.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) CorianderGreen else EmberBorder),
                                modifier = Modifier
                                    .clickable { onCheckInTable(tbl.label) }
                                    .testTag("guest_table_chip_${tbl.id}")
                            ) {
                                Text(
                                    text = "${tbl.label} (${tbl.capacity}p)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) CorianderGreen else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SmokedCard(borderColor = SmokedAmber) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Ready to Book Your Table?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Sign in with Google to confirm your reservation slot, receive instant approval alerts, and earn Oak & Smoke Loyalty Points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onRequestSignIn,
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("guest_reserve_google_sign_in_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        Text("Sign in with Google to Reserve Table", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestAccountSignInTab(
    isSigningIn: Boolean,
    authError: String?,
    onRequestSignIn: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenScreenshots: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SmokedCard(borderColor = SmokedAmber) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1-Tap Email Sign-In", style = MaterialTheme.typography.titleLarge)
                        StatusPill(label = "NO CODE NEEDED", color = CorianderGreen)
                    }
                    Text(
                        text = "Tap below to sign in directly using your device's Google Email ID — no OTP or verification code required. Guest browsing stays open anytime for menu & seating.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = EmberBorder)
                    GuestFeatureLine(Icons.Filled.Payments, "Place COD Orders with GPS Pin & Live Tracking")
                    GuestFeatureLine(Icons.Filled.TableRestaurant, "Book Tables & Receive Real-Time Sound Alerts")
                    GuestFeatureLine(Icons.Filled.Loyalty, "Earn & Redeem Loyalty Points on Every Order")
                    GuestFeatureLine(Icons.Filled.Lock, "Staff, Rider & Owner Portals Protected by Verified Email")

                    if (authError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = onRequestSignIn,
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("guest_account_google_sign_in_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CharcoalDeep, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Signing in...", fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = if (authError != null) "Retry Google Email Sign-In" else "Continue with Google Email",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            PrivacyAndScreenshotsCard(
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onOpenScreenshots = onOpenScreenshots
            )
        }
    }
}

@Composable
private fun GuestFeatureLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SmokedAmber, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.example.data.formatPkr
import com.example.services.AppPwaQrService
import com.example.ui.components.PwaDownloadAndQrModal
import com.example.ui.components.SectionHeader
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
    var showGuestCheckoutSheet by remember { mutableStateOf(false) }
    var showTopPwaQrStrip by remember { mutableStateOf(true) }
    var showPwaQrModal by remember { mutableStateOf(false) }

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
        AuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isSigningIn = false
                showGuestCheckoutSheet = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isSigningIn = false
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
        catalogMenu.mapNotNull { dish ->
            val q = cartQuantities[dish.id] ?: 0
            if (q > 0 && dish.available) CartLine(dish, q) else null
        }
    }

    BackHandler(enabled = selectedTab != "guest_menu") {
        selectedTab = "guest_menu"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

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
                                text = "Hasilpur · Guest Customer",
                                style = MaterialTheme.typography.labelSmall,
                                color = SmokedAmber
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
                    TopInstalledPwaQrStrip(
                        visible = showTopPwaQrStrip,
                        onOpenQrModal = { showPwaQrModal = true },
                        onDownloadPwa = {
                            AppPwaQrService.openPwaDownload(context)
                                .onSuccess {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Opening Oak & Smoke Web PWA. Tap 'Install App' in your browser.")
                                    }
                                }
                                .onFailure {
                                    showPwaQrModal = true
                                }
                        },
                        onDismiss = { showTopPwaQrStrip = false },
                        modifier = Modifier.widthIn(max = 860.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 860.dp)
                    ) {
                        when (selectedTab) {
                            "guest_menu" -> GuestMenuTab(
                                catalog = catalogMenu,
                                cartLines = cartLines,
                                cartQuantities = cartQuantities,
                                onIncrementCart = onIncrementCart,
                                onDecrementCart = onDecrementCart,
                                onOpenCheckout = { showGuestCheckoutSheet = true },
                                onRequestSignIn = { launchGoogleSignIn() }
                            )
                            "guest_tables" -> GuestTablesPreviewTab(
                                isSigningIn = isSigningIn,
                                onRequestSignIn = { launchGoogleSignIn() }
                            )
                            else -> GuestAccountSignInTab(
                                isSigningIn = isSigningIn,
                                onRequestSignIn = { launchGoogleSignIn() }
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
            val subtotal = cartLines.sumOf { it.item.pricePkr * it.quantity }
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
                    Text("Guest COD Cart Preview", style = MaterialTheme.typography.headlineSmall)
                    StatusPill(label = "COD ONLY", color = CorianderGreen)
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
                                    text = "${line.quantity}x ${line.item.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatPkr(line.item.pricePkr * line.quantity),
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
                    Text("Estimated COD Total", style = MaterialTheme.typography.titleMedium)
                    Text(formatPkr(subtotal), style = MonoPriceStyle, color = SmokedAmber)
                }

                Text(
                    text = "Sign in with Google to attach your delivery GPS pin for our Rider, place this Cash on Delivery order, and track live kitchen updates. Your cart items will stay saved.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CharcoalDeep, strokeWidth = 2.dp)
                    } else {
                        Text("Sign in with Google to Place COD Order", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showPwaQrModal) {
        PwaDownloadAndQrModal(
            onDismiss = { showPwaQrModal = false },
            onFeedback = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }
}

@Composable
private fun GuestMenuTab(
    catalog: List<MenuItem>,
    cartLines: List<CartLine>,
    cartQuantities: Map<String, Int>,
    onIncrementCart: (String) -> Unit,
    onDecrementCart: (String) -> Unit,
    onOpenCheckout: () -> Unit,
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
    val cartSubtotal = cartLines.sumOf { it.item.pricePkr * it.quantity }

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
                            StatusPill(label = "GUEST MODE · CUSTOMER MENU", color = SmokedAmber)
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

            // Guest Customer Domain Banner
            item {
                SmokedCard(borderColor = SmokedAmber.copy(alpha = 0.55f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Guest Customer Access",
                            tint = SmokedAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browsing as Guest Customer",
                                style = MaterialTheme.typography.titleSmall,
                                color = SmokedAmber
                            )
                            Text(
                                text = "Explore dishes, PKR prices & build your COD cart. Sign in with Google to order with GPS tracking or book tables.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onRequestSignIn,
                            border = BorderStroke(1.dp, SmokedAmber),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Sign In", color = SmokedAmber, style = MaterialTheme.typography.labelSmall)
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
                            GuestSpecialCard(
                                dish = special,
                                qty = cartQuantities[special.id] ?: 0,
                                onAdd = { onIncrementCart(special.id) }
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
                    quantity = cartQuantities[dish.id] ?: 0,
                    onIncrement = { onIncrementCart(dish.id) },
                    onDecrement = { onDecrementCart(dish.id) }
                )
            }
        }

        if (cartItemCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = SmokedAmber,
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
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = "Cart items",
                                tint = SmokedAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$cartItemCount items · Cash on Delivery",
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
                            contentColor = SmokedAmber
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("guest_open_checkout_btn")
                    ) {
                        Text("Checkout COD")
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

@Composable
private fun GuestDishCard(
    dish: MenuItem,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    SmokedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    Text(formatPkr(dish.pricePkr), style = MonoPriceStyle, color = SmokedAmber)
                    if (quantity == 0) {
                        Button(
                            onClick = onIncrement,
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
                            IconButton(onClick = onDecrement, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease ${dish.name}", tint = SmokedAmber)
                            }
                            Text("$quantity", style = MonoPriceStyle)
                            IconButton(onClick = onIncrement, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase ${dish.name}", tint = SmokedAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuestTablesPreviewTab(
    isSigningIn: Boolean,
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
                title = "Dining Zones & Table Reservations",
                subtitle = "Oak & Smoke Restaurant Hasilpur · Seating Preview"
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
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmberBorder)
                            ) {
                                Text(
                                    text = "${tbl.label} (${tbl.capacity} seats)",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
    onRequestSignIn: () -> Unit
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
                        Text("Guest Customer Mode", style = MaterialTheme.typography.titleLarge)
                        StatusPill(label = "CUSTOMER ONLY", color = SmokedAmber)
                    }
                    Text(
                        text = "You are browsing Oak & Smoke Restaurant Hasilpur as a Guest Customer. Guest access is open for browsing the digital menu, daily specials, and dining seating, and is strictly restricted to the Customer domain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = EmberBorder)
                    GuestFeatureLine(Icons.Filled.Payments, "Place COD Orders with GPS Pin & Live Tracking")
                    GuestFeatureLine(Icons.Filled.TableRestaurant, "Book Tables & Receive Real-Time Sound Alerts")
                    GuestFeatureLine(Icons.Filled.Loyalty, "Earn & Redeem Loyalty Points on Every Order")
                    GuestFeatureLine(Icons.Filled.Lock, "Staff, Rider & Owner Portals Require Authorized Sign-In")

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
                            Text("Sign in with Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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

package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CartLine
import com.example.data.MenuCategoryRecord
import com.example.data.MenuItem
import com.example.data.SpiceLevel
import com.example.data.cartItemKey
import com.example.data.formatCoordinates
import com.example.data.formatPkr
import com.example.data.parsedVarieties
import com.example.data.resolveCategoryDisplayLabel
import com.example.services.CapturedLocation
import com.example.services.LocationHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmokedCard
import com.example.ui.components.StatusPill
import com.example.ui.components.resolveDishDrawable
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.SmokedAmber
import com.example.ui.theme.TandoorCopper
import com.example.viewmodel.RestaurantViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMenuScreen(
    viewModel: RestaurantViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()
    val menuCategories by viewModel.menuCategories.collectAsStateWithLifecycle()
    val cartLines by viewModel.cartLines.collectAsStateWithLifecycle()
    val cartQuantities by viewModel.cartQuantities.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val allItems = (menuState as? UiState.Success)?.data ?: emptyList()
    val specials by remember(allItems) {
        derivedStateOf { allItems.filter { it.dailySpecial && it.available } }
    }
    val filteredItems by remember(allItems, selectedFilter, searchQuery) {
        derivedStateOf {
            allItems.filter { dish ->
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
            // Branded Editorial Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1795434279848),
                        contentDescription = "Oak & Smoke Restaurant Hasilpur hero banner",
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
                            StatusPill(label = "COD ONLY · HASILPUR", color = SmokedAmber)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CharcoalDeep.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, SmokedAmber.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Loyalty,
                                        contentDescription = "Loyalty points",
                                        tint = SmokedAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${profile?.loyaltyPoints ?: 0} pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SmokedAmber
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Oak & Smoke",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Authentic Pakistani Karahi, Smoked BBQ & Platters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmokedAmber
                            )
                        }
                    }
                }
            }

            // First-launch helper banner if Owner has not initialized yet
            if (config == null || config?.ownerInitialized == false) {
                item {
                    SmokedCard(borderColor = SmokedAmber) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Setup restaurant",
                                tint = SmokedAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "First-Time Restaurant Setup",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SmokedAmber
                                )
                                Text(
                                    text = "Are you the Owner? Initialize your Master Staff ID & seed the Karahi & BBQ starter menu in Profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = onNavigateToProfile,
                                modifier = Modifier.testTag("go_to_owner_setup_btn")
                            ) {
                                Text("Setup", color = SmokedAmber)
                            }
                        }
                    }
                }
            }

            // Daily Specials Horizontal Showcase
            if (specials.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Today's Daily Specials",
                        subtitle = "Fresh off the oak charcoal grill"
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(specials, key = { "sp_${it.id}" }) { special ->
                            val firstVariety = remember(special.id, special.varietiesDetail, special.pricePkr) {
                                special.parsedVarieties().first()
                            }
                            val defaultKey = cartItemKey(special.id, firstVariety.label)
                            val totalSpecialQty = cartLines.filter { it.item.id == special.id }.sumOf { it.quantity }
                            DailySpecialCard(
                                dish = special,
                                qty = totalSpecialQty,
                                onAdd = { viewModel.incrementCart(defaultKey) }
                            )
                        }
                    }
                }
            }

            // Search & Category Filters
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
                        .testTag("menu_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("ALL" to "All Menu", "SPECIALS" to "Daily Specials") +
                        menuCategories.map { it.code to it.displayLabel }
                    items(chips) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SmokedAmber,
                                selectedLabelColor = CharcoalDeep
                            ),
                            modifier = Modifier.testTag("filter_chip_${key.lowercase()}")
                        )
                    }
                }
            }

            // Menu List
            when (menuState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SmokedAmber)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.RestaurantMenu,
                            title = "Unable to load menu",
                            subtitle = (menuState as UiState.Error).message
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredItems.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.RestaurantMenu,
                                title = "No dishes found",
                                subtitle = if (allItems.isEmpty()) {
                                    "The Oak & Smoke menu is empty. The Owner can seed or add Pakistani Karahi, BBQ, and Platters from the Owner Dashboard."
                                } else {
                                    "Try another category or clear your search filter."
                                }
                            )
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { dish ->
                            MenuDishCard(
                                dish = dish,
                                categoryLabel = resolveCategoryDisplayLabel(dish.category, menuCategories),
                                cartQuantities = cartQuantities,
                                onIncrementKey = { key -> viewModel.incrementCart(key) },
                                onDecrementKey = { key -> viewModel.decrementCart(key) }
                            )
                        }
                    }
                }
            }
        }

        // Sticky Floating COD Cart Bar
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
                        onClick = { showCheckoutSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CharcoalDeep,
                            contentColor = SmokedAmber
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("open_cod_checkout_btn")
                    ) {
                        Text("Checkout COD")
                    }
                }
            }
        }
    }

    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CodCheckoutSheetContent(
                cartLines = cartLines,
                defaultName = profile?.displayName?.ifBlank { viewModel.userDisplayName } ?: viewModel.userDisplayName,
                defaultPhone = profile?.phone.orEmpty(),
                defaultAddress = profile?.deliveryAddress.orEmpty(),
                availablePoints = profile?.loyaltyPoints ?: 0L,
                pointValuePkr = config?.loyaltyPointValuePkr ?: 5L,
                isBusy = isBusy,
                onPlaceOrder = { name, phone, address, loc, redeemPts, notes ->
                    viewModel.placeCodOrder(
                        customerName = name,
                        customerPhone = phone,
                        deliveryAddress = address,
                        customerLat = loc.lat,
                        customerLng = loc.lng,
                        locationLabel = loc.label,
                        hasLocation = true,
                        redeemPoints = redeemPts,
                        notes = notes
                    ) {
                        showCheckoutSheet = false
                        onNavigateToOrders()
                    }
                }
            )
        }
    }
}

@Composable
private fun DailySpecialCard(
    dish: MenuItem,
    qty: Int,
    onAdd: () -> Unit
) {
    SmokedCard(
        modifier = Modifier.width(245.dp),
        borderColor = SmokedAmber.copy(alpha = 0.55f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
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
                Text(
                    text = formatPkr(dish.pricePkr),
                    style = MonoPriceStyle,
                    color = SmokedAmber
                )
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
private fun MenuDishCard(
    dish: MenuItem,
    categoryLabel: String,
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
                        .size(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = SmokedAmber
                        )
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${SpiceLevel.fromRaw(dish.spiceLevel).displayLabel} · ${dish.prepMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
                        Text(
                            text = formatPkr(activeVariety.pricePkr),
                            style = MonoPriceStyle,
                            color = SmokedAmber
                        )

                        if (!dish.available) {
                            StatusPill(label = "Sold Out", color = MaterialTheme.colorScheme.error)
                        } else if (quantity == 0) {
                            Button(
                                onClick = { onIncrementKey(activeKey) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SmokedAmber,
                                    contentColor = CharcoalDeep
                                ),
                                modifier = Modifier.testTag("add_dish_${dish.id}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add ${dish.name}", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onDecrementKey(activeKey) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease ${dish.name}", tint = SmokedAmber)
                                }
                                Text(
                                    text = "$quantity",
                                    style = MonoPriceStyle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onIncrementKey(activeKey) },
                                    modifier = Modifier.size(48.dp)
                                ) {
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

@Composable
private fun CodCheckoutSheetContent(
    cartLines: List<CartLine>,
    defaultName: String,
    defaultPhone: String,
    defaultAddress: String,
    availablePoints: Long,
    pointValuePkr: Long,
    isBusy: Boolean,
    onPlaceOrder: (name: String, phone: String, address: String, location: CapturedLocation, redeemPts: Long, notes: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(defaultName) }
    var phone by remember { mutableStateOf(defaultPhone) }
    var address by remember { mutableStateOf(defaultAddress) }
    var notes by remember { mutableStateOf("") }
    var redeemSlider by remember { mutableFloatStateOf(0f) }

    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    fun triggerLocationCapture() {
        scope.launch {
            isFetchingLocation = true
            locationError = null
            LocationHelper.fetchCurrentLocation(context)
                .onSuccess { loc ->
                    capturedLocation = loc
                    if (address.isBlank() && !loc.label.startsWith("GPS:")) {
                        address = loc.label
                    }
                }
                .onFailure { err ->
                    locationError = err.message ?: "Unable to read GPS location. Turn on Location Services and try again."
                }
            isFetchingLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerLocationCapture()
        } else {
            locationError = "Location permission denied. Rider needs your live GPS location to deliver COD orders."
        }
    }

    val subtotal = cartLines.sumOf { it.unitPricePkr * it.quantity }
    val maxRedeemable = minOf(availablePoints, if (pointValuePkr > 0L) subtotal / pointValuePkr else 0L)
    val redeemedPoints = redeemSlider.roundToLong().coerceIn(0L, maxRedeemable)
    val discountPkr = (redeemedPoints * pointValuePkr).coerceAtMost(subtotal)
    val totalPkr = (subtotal - discountPkr).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cash on Delivery Checkout",
                style = MaterialTheme.typography.headlineSmall
            )
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

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Customer Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Mobile Number (e.g., 0300-1234567)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_phone_input")
        )
        // Mandatory Current GPS Location Card for Rider Navigation
        Surface(
            color = if (capturedLocation != null) CorianderGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                when {
                    capturedLocation != null -> CorianderGreen
                    locationError != null -> CrimsonEmber
                    else -> SmokedAmber.copy(alpha = 0.7f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (capturedLocation != null) Icons.Filled.CheckCircle else Icons.Filled.MyLocation,
                            contentDescription = "Current GPS Location",
                            tint = if (capturedLocation != null) CorianderGreen else SmokedAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Delivery GPS Location",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (capturedLocation != null) CorianderGreen else SmokedAmber
                        )
                    }
                    StatusPill(
                        label = if (capturedLocation != null) "ATTACHED" else "REQUIRED",
                        color = if (capturedLocation != null) CorianderGreen else CrimsonEmber
                    )
                }

                if (capturedLocation != null) {
                    val loc = capturedLocation!!
                    Text(
                        text = loc.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "GPS Pin for Rider: ${formatCoordinates(loc.lat, loc.lng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CorianderGreen
                    )
                } else {
                    Text(
                        text = "Attach your current GPS location so our Oak & Smoke Rider can fetch your exact pin and navigate straight to you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (locationError != null) {
                    Text(
                        text = locationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonEmber
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            triggerLocationCapture()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isFetchingLocation && !isBusy,
                    border = BorderStroke(1.dp, if (capturedLocation != null) CorianderGreen else SmokedAmber),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attach_location_btn")
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SmokedAmber, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Fetching Current GPS...", color = SmokedAmber)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = if (capturedLocation != null) CorianderGreen else SmokedAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (capturedLocation != null) "Refresh Current Location" else "Attach My Current Location",
                            color = if (capturedLocation != null) CorianderGreen else SmokedAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Hasilpur Delivery Address / Landmark") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_address_input")
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Kitchen / Delivery Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (maxRedeemable > 0L) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SmokedAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Redeem Loyalty Points ($availablePoints avail.)",
                            style = MaterialTheme.typography.titleSmall,
                            color = SmokedAmber
                        )
                        Text(
                            text = "-${formatPkr(discountPkr)}",
                            style = MonoPriceStyle,
                            color = CorianderGreen
                        )
                    }
                    Slider(
                        value = redeemSlider,
                        onValueChange = { redeemSlider = it },
                        valueRange = 0f..maxRedeemable.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = SmokedAmber, activeTrackColor = SmokedAmber)
                    )
                    Text(
                        text = "Using $redeemedPoints pts (1 pt = ${formatPkr(pointValuePkr)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = EmberBorder)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
            Text(formatPkr(subtotal), style = MonoPriceStyle)
        }
        if (discountPkr > 0L) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Loyalty Discount", style = MaterialTheme.typography.bodyMedium, color = CorianderGreen)
                Text("-${formatPkr(discountPkr)}", style = MonoPriceStyle, color = CorianderGreen)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Payments, contentDescription = "Cash on Delivery", tint = SmokedAmber)
                Text("Payable Cash on Delivery", style = MaterialTheme.typography.titleMedium)
            }
            Text(formatPkr(totalPkr), style = MonoPriceStyle, color = SmokedAmber)
        }

        Button(
            onClick = {
                capturedLocation?.let { loc ->
                    onPlaceOrder(name, phone, address, loc, redeemedPoints, notes)
                }
            },
            enabled = !isBusy && !isFetchingLocation && capturedLocation != null && phone.trim().length >= 7 && address.trim().length >= 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_cod_order_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SmokedAmber, contentColor = CharcoalDeep)
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CharcoalDeep, strokeWidth = 2.dp)
            } else if (capturedLocation == null) {
                Text("Attach Current Location to Place Order", fontWeight = FontWeight.Bold)
            } else {
                Text("Confirm COD Order · ${formatPkr(totalPkr)}", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

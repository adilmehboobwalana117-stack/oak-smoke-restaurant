package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AdminFinanceRepository
import com.example.data.AuthManager
import com.example.data.FirestoreProvider
import com.example.data.RestaurantRepository
import com.example.data.UserRoleTier
import com.example.services.AppDownloadTarget
import com.example.services.AppPwaQrService
import com.example.services.LocalNotificationHelper
import com.example.services.NetworkConnectivityObserver
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.PwaDownloadAndQrModal
import com.example.ui.components.StatusPill
import com.example.ui.components.TopBarQrPwaActionButton
import com.example.ui.components.TopInstalledPwaQrStrip
import com.example.ui.screens.CustomerMenuScreen
import com.example.ui.screens.CustomerOrdersReservationsScreen
import com.example.ui.screens.GuestCustomerShell
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.ProfileAndAlertsScreen
import com.example.ui.screens.RiderPortalScreen
import com.example.ui.screens.StaffOperationsScreen
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.OakAndSmokeTheme
import com.example.ui.theme.SmokedAmber
import com.example.viewmodel.PortalMode
import com.example.viewmodel.RestaurantViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalNotificationHelper.ensureChannel(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(CharcoalDeep.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(CharcoalDeep.toArgb())
        )
        setContent {
            OakAndSmokeTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var guestCartQuantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val user = currentUser
    if (user == null) {
        GuestCustomerShell(
            cartQuantities = guestCartQuantities,
            onIncrementCart = { itemId ->
                guestCartQuantities = guestCartQuantities.toMutableMap().apply {
                    put(itemId, (get(itemId) ?: 0) + 1)
                }
            },
            onDecrementCart = { itemId ->
                guestCartQuantities = guestCartQuantities.toMutableMap().apply {
                    val next = (get(itemId) ?: 0) - 1
                    if (next <= 0) remove(itemId) else put(itemId, next)
                }
            },
            onAuthSuccess = { /* AuthStateListener drives transition into OakAndSmokeShell */ }
        )
    } else {
        key(user.uid) {
            OakAndSmokeShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Oak & Smoke Guest",
                userEmail = user.email ?: "",
                userEmailVerified = user.isEmailVerified,
                initialGuestCart = guestCartQuantities,
                onGuestCartMerged = { guestCartQuantities = emptyMap() }
            )
        }
    }
}

private data class NavDest(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OakAndSmokeShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    userEmailVerified: Boolean,
    initialGuestCart: Map<String, Int> = emptyMap(),
    onGuestCartMerged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: RestaurantViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirestoreProvider.getInstance(app)
                RestaurantViewModel(
                    repo = RestaurantRepository(db),
                    adminRepo = AdminFinanceRepository(db),
                    connectivityObserver = NetworkConnectivityObserver(app),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    userEmailVerified = userEmailVerified
                )
            }
        }
    )

    val effectiveRole by viewModel.effectiveRole.collectAsStateWithLifecycle()
    val portalMode by viewModel.portalMode.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDestId by remember { mutableStateOf("menu") }
    var customerHubInitialTab by remember { mutableIntStateOf(0) }
    var showTopPwaQrStrip by remember { mutableStateOf(true) }
    var showPwaQrModal by remember { mutableStateOf(false) }
    val seenNotifIds = remember { mutableSetOf<String>() }

    val configPublicUrl = config?.publicDownloadUrl.orEmpty()
    LaunchedEffect(config?.publicDownloadUrl) {
        config?.let { cfg ->
            AppPwaQrService.cachePublicDownloadUrl(context, cfg.publicDownloadUrl)
        }
    }
    val activePublicQrUrl = remember(configPublicUrl, context) {
        AppPwaQrService.resolveTargetUrl(context, AppDownloadTarget.PUBLIC_DOWNLOAD, configPublicUrl)
    }

    // Carry over any dishes the customer added while browsing in Guest Customer mode
    LaunchedEffect(initialGuestCart) {
        if (initialGuestCart.isNotEmpty()) {
            viewModel.mergeGuestCart(initialGuestCart)
            onGuestCartMerged()
        }
    }

    // Switch destination automatically when user elevates/switches portal mode
    LaunchedEffect(portalMode) {
        selectedDestId = when (portalMode) {
            PortalMode.CUSTOMER -> if (selectedDestId in listOf("menu", "orders", "profile")) selectedDestId else "menu"
            PortalMode.RIDER -> "rider"
            PortalMode.STAFF -> "staff"
            PortalMode.OWNER -> "owner"
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // Trigger Android sound & system notifications for brand-new real-time Firestore alerts
    LaunchedEffect(notifications, profile?.notificationsEnabled) {
        if (seenNotifIds.isEmpty()) {
            notifications.forEach { seenNotifIds.add(it.id) }
        } else if (profile?.notificationsEnabled != false) {
            notifications.filter { !it.read && it.id !in seenNotifIds }.forEach { fresh ->
                seenNotifIds.add(fresh.id)
                val playSoundAlert = fresh.type.equals("RIDER_READY", ignoreCase = true) ||
                    fresh.title.contains("READY", ignoreCase = true)
                LocalNotificationHelper.showNotification(
                    context = context,
                    id = fresh.id.hashCode(),
                    title = fresh.title,
                    message = fresh.message,
                    withSoundAlert = playSoundAlert
                )
            }
        }
    }

    val destinations = remember(effectiveRole, unreadCount) {
        buildList {
            add(NavDest("menu", "Menu", Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu))
            add(NavDest("orders", "Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong))
            if (effectiveRole == UserRoleTier.RIDER || effectiveRole == UserRoleTier.MANAGER || effectiveRole == UserRoleTier.OWNER) {
                add(NavDest("rider", "Rider", Icons.Filled.DeliveryDining, Icons.Outlined.DeliveryDining))
            }
            if (effectiveRole == UserRoleTier.STAFF || effectiveRole == UserRoleTier.MANAGER || effectiveRole == UserRoleTier.OWNER) {
                add(NavDest("staff", "Staff", Icons.Filled.Badge, Icons.Outlined.Badge))
            }
            if (effectiveRole == UserRoleTier.OWNER || effectiveRole == UserRoleTier.MANAGER) {
                add(NavDest("owner", "Owner", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
            }
            add(NavDest("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person, badgeCount = unreadCount))
        }
    }

    BackHandler(enabled = selectedDestId != "menu") {
        selectedDestId = "menu"
        viewModel.setPortalMode(PortalMode.CUSTOMER)
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
                                text = "Hasilpur · PKR",
                                style = MaterialTheme.typography.labelSmall,
                                color = SmokedAmber
                            )
                        }
                    },
                    actions = {
                        TopBarQrPwaActionButton(
                            onClick = { showPwaQrModal = true }
                        )
                        StatusPill(
                            label = if (effectiveRole == UserRoleTier.CUSTOMER) "Guest" else effectiveRole.displayLabel,
                            color = if (effectiveRole == UserRoleTier.CUSTOMER) SmokedAmber else CorianderGreen,
                            modifier = Modifier.padding(end = 12.dp)
                        )
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
                            val selected = selectedDestId == dest.id
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedDestId = dest.id },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest.badgeCount > 0) {
                                                Badge(containerColor = SmokedAmber, contentColor = CharcoalDeep) {
                                                    Text(dest.badgeCount.coerceAtMost(99).toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    }
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
                            val selected = selectedDestId == dest.id
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedDestId = dest.id },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest.badgeCount > 0) {
                                                Badge(containerColor = SmokedAmber, contentColor = CharcoalDeep) {
                                                    Text(dest.badgeCount.coerceAtMost(99).toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    }
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
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OfflineStatusBanner(isOnline = isOnline)
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
                        when (selectedDestId) {
                            "menu" -> CustomerMenuScreen(
                                viewModel = viewModel,
                                onNavigateToOrders = {
                                    customerHubInitialTab = 0
                                    selectedDestId = "orders"
                                },
                                onNavigateToProfile = { selectedDestId = "profile" }
                            )
                            "orders" -> CustomerOrdersReservationsScreen(
                                viewModel = viewModel,
                                initialTab = customerHubInitialTab
                            )
                            "rider" -> RiderPortalScreen(viewModel = viewModel)
                            "staff" -> StaffOperationsScreen(viewModel = viewModel)
                            "owner" -> OwnerDashboardScreen(viewModel = viewModel)
                            else -> ProfileAndAlertsScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    AuthManager.signOut(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPwaQrModal) {
        PwaDownloadAndQrModal(
            onDismiss = { showPwaQrModal = false },
            onFeedback = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            configPublicDownloadUrl = configPublicUrl,
            canEditDownloadUrl = effectiveRole == UserRoleTier.OWNER || effectiveRole == UserRoleTier.MANAGER,
            onSaveCustomDownloadUrl = { updatedUrl ->
                viewModel.updatePublicDownloadUrl(updatedUrl)
            }
        )
    }
}

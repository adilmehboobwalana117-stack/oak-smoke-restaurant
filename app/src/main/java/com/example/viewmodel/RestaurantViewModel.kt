package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminFinanceRepository
import com.example.data.CartLine
import com.example.data.DiningTable
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntry
import com.example.data.FeedbackCategory
import com.example.data.FeedbackEntry
import com.example.data.InventoryCategory
import com.example.data.InventoryItem
import com.example.data.MenuCategory
import com.example.data.MenuItem
import com.example.data.NotificationItem
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.OrderType
import com.example.data.ReservationRecord
import com.example.data.ReservationStatus
import com.example.data.RestaurantConfig
import com.example.data.RestaurantRepository
import com.example.data.RoleGrant
import com.example.data.SeatingZone
import com.example.data.SpiceLevel
import com.example.data.StaffMember
import com.example.data.UserProfile
import com.example.data.UserRoleTier
import com.example.services.FinancialReportService
import com.example.services.FinancialReportSnapshot
import com.example.services.MonthlyTrendPoint
import com.example.services.NetworkConnectivityObserver
import com.example.services.ReportPeriod
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PortalMode(val label: String) {
    CUSTOMER("Customer"),
    RIDER("Rider"),
    STAFF("Staff Ops"),
    OWNER("Owner Admin")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class RestaurantViewModel(
    private val repo: RestaurantRepository,
    private val adminRepo: AdminFinanceRepository,
    private val connectivityObserver: NetworkConnectivityObserver,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userEmailVerified: Boolean
) : ViewModel() {

    private companion object {
        const val TAG = "RestaurantVM"
        const val STOP_TIMEOUT_MS = 5000L
        const val BOOTSTRAP_ADMIN_EMAIL = "adilmehboobwalana117@gmail.com"
    }

    private fun <T> Flow<T>.toSafeStateFlow(initial: T, label: String): StateFlow<T> =
        catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Stream error ($label): ${root.message}", root)
            _errorMessage.value = root.message ?: "Unable to sync $label"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "UiState stream error ($label): ${root.message}", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _portalMode = MutableStateFlow(PortalMode.CUSTOMER)
    val portalMode: StateFlow<PortalMode> = _portalMode.asStateFlow()

    private val _cartQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cartQuantities: StateFlow<Map<String, Int>> = _cartQuantities.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.observeIsOnline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val config: StateFlow<RestaurantConfig?> = repo.observeConfig(currentUserId)
        .toSafeStateFlow(null, "restaurantConfig")

    val roleGrant: StateFlow<RoleGrant?> = repo.observeRoleGrant(currentUserId)
        .toSafeStateFlow(null, "roleGrant")

    val userProfile: StateFlow<UserProfile?> = repo.observeUserProfile(currentUserId)
        .toSafeStateFlow(null, "userProfile")

    val isBootstrapAdmin: Boolean =
        userEmailVerified && userEmail.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)

    val effectiveRole: StateFlow<UserRoleTier> = roleGrant
        .map { grant ->
            when {
                isBootstrapAdmin -> UserRoleTier.OWNER
                grant != null -> UserRoleTier.fromRaw(grant.role)
                else -> UserRoleTier.CUSTOMER
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            if (isBootstrapAdmin) UserRoleTier.OWNER else UserRoleTier.CUSTOMER
        )

    val menuState: StateFlow<UiState<List<MenuItem>>> = repo.observeMenuItems(currentUserId)
        .toUiStateFlow("menu")

    val menuItems: StateFlow<List<MenuItem>> = menuState
        .map { (it as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val diningTables: StateFlow<List<DiningTable>> = repo.observeDiningTables(currentUserId)
        .toSafeStateFlow(emptyList(), "diningTables")

    val cartLines: StateFlow<List<CartLine>> = combine(menuItems, _cartQuantities) { items, qtys ->
        items.mapNotNull { dish ->
            val q = qtys[dish.id] ?: 0
            if (q > 0 && dish.available) CartLine(dish, q) else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val customerOrders: StateFlow<List<OrderRecord>> = repo.observeCustomerOrders(currentUserId)
        .toSafeStateFlow(emptyList(), "customerOrders")

    val customerReservations: StateFlow<List<ReservationRecord>> =
        repo.observeCustomerReservations(currentUserId)
            .toSafeStateFlow(emptyList(), "customerReservations")

    val customerFeedback: StateFlow<List<FeedbackEntry>> =
        repo.observeCustomerFeedback(currentUserId)
            .toSafeStateFlow(emptyList(), "customerFeedback")

    // Role-gated Rider, Staff & Owner streams (ONLY attach after role elevation!)
    val allOrders: StateFlow<List<OrderRecord>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRoleTier.CUSTOMER) repo.observeAllOrders(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "allOrders")

    val allReservations: StateFlow<List<ReservationRecord>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRoleTier.CUSTOMER && role != UserRoleTier.RIDER) repo.observeAllReservations(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "allReservations")

    val allFeedback: StateFlow<List<FeedbackEntry>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRoleTier.CUSTOMER && role != UserRoleTier.RIDER) repo.observeAllFeedback(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "allFeedback")

    val inventoryItems: StateFlow<List<InventoryItem>> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRoleTier.CUSTOMER && role != UserRoleTier.RIDER) adminRepo.observeInventory(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "inventory")

    val ownStaffRecord: StateFlow<StaffMember?> = effectiveRole
        .flatMapLatest { role ->
            if (role != UserRoleTier.CUSTOMER) adminRepo.observeOwnStaffRecord(currentUserId)
            else flowOf(null)
        }
        .toSafeStateFlow(null, "ownStaffRecord")

    val allStaffMembers: StateFlow<List<StaffMember>> = effectiveRole
        .flatMapLatest { role ->
            if (role == UserRoleTier.OWNER || role == UserRoleTier.MANAGER) adminRepo.observeAllStaffMembers(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "allStaffMembers")

    val allRiders: StateFlow<List<StaffMember>> = allStaffMembers
        .map { members -> members.filter { it.roleTier == UserRoleTier.RIDER.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val expenses: StateFlow<List<ExpenseEntry>> = effectiveRole
        .flatMapLatest { role ->
            if (role == UserRoleTier.OWNER || role == UserRoleTier.MANAGER) adminRepo.observeExpenses(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "expenses")

    val allUsers: StateFlow<List<UserProfile>> = effectiveRole
        .flatMapLatest { role ->
            if (role == UserRoleTier.OWNER) adminRepo.observeAllUsers(currentUserId)
            else flowOf(emptyList())
        }
        .toSafeStateFlow(emptyList(), "allUsers")

    private val personalNotifs: Flow<List<NotificationItem>> =
        repo.observePersonalNotifications(currentUserId)
    private val broadcastNotifs: Flow<List<NotificationItem>> =
        repo.observeBroadcastNotifications(currentUserId)
    private val staffNotifs: Flow<List<NotificationItem>> = effectiveRole.flatMapLatest { role ->
        if (role == UserRoleTier.STAFF || role == UserRoleTier.MANAGER || role == UserRoleTier.OWNER) {
            repo.observeStaffNotifications(currentUserId)
        } else {
            flowOf(emptyList())
        }
    }
    private val riderNotifs: Flow<List<NotificationItem>> = effectiveRole.flatMapLatest { role ->
        if (role != UserRoleTier.CUSTOMER) repo.observeRiderNotifications(currentUserId)
        else flowOf(emptyList())
    }

    val notifications: StateFlow<List<NotificationItem>> =
        combine(personalNotifs, broadcastNotifs, staffNotifs, riderNotifs) { p, b, s, r ->
            (p + b + s + r)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.toSafeStateFlow(emptyList(), "notifications")

    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val dailyReport: StateFlow<FinancialReportSnapshot> =
        combine(allOrders, expenses, allStaffMembers) { ord, exp, stf ->
            FinancialReportService.buildSnapshot(ReportPeriod.DAILY, ord, exp, stf)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            FinancialReportService.buildSnapshot(ReportPeriod.DAILY, emptyList(), emptyList(), emptyList())
        )

    val monthlyReport: StateFlow<FinancialReportSnapshot> =
        combine(allOrders, expenses, allStaffMembers) { ord, exp, stf ->
            FinancialReportService.buildSnapshot(ReportPeriod.MONTHLY, ord, exp, stf)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            FinancialReportService.buildSnapshot(ReportPeriod.MONTHLY, emptyList(), emptyList(), emptyList())
        )

    val monthlyTrends: StateFlow<List<MonthlyTrendPoint>> =
        combine(allOrders, expenses) { ord, exp ->
            FinancialReportService.buildMonthlyTrends(ord, exp)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            repo.ensureUserProfileExists(userDisplayName, userEmail)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setPortalMode(mode: PortalMode) {
        val role = effectiveRole.value
        when (mode) {
            PortalMode.CUSTOMER -> _portalMode.value = PortalMode.CUSTOMER
            PortalMode.RIDER -> {
                if (role == UserRoleTier.RIDER || role == UserRoleTier.MANAGER || role == UserRoleTier.OWNER) {
                    _portalMode.value = PortalMode.RIDER
                } else {
                    _snackbarMessage.value = "Unlock Rider access with your Rider ID & Passcode first."
                }
            }
            PortalMode.STAFF -> {
                if (role == UserRoleTier.STAFF || role == UserRoleTier.MANAGER || role == UserRoleTier.OWNER) {
                    _portalMode.value = PortalMode.STAFF
                } else {
                    _snackbarMessage.value = "Unlock Staff access with your Staff ID & Passcode first."
                }
            }
            PortalMode.OWNER -> {
                if (role == UserRoleTier.OWNER || role == UserRoleTier.MANAGER) _portalMode.value = PortalMode.OWNER
                else _snackbarMessage.value = "Owner or Manager tier required."
            }
        }
    }

    fun incrementCart(itemId: String) {
        _cartQuantities.value = _cartQuantities.value.toMutableMap().apply {
            put(itemId, (get(itemId) ?: 0) + 1)
        }
    }

    fun decrementCart(itemId: String) {
        _cartQuantities.value = _cartQuantities.value.toMutableMap().apply {
            val next = (get(itemId) ?: 0) - 1
            if (next <= 0) remove(itemId) else put(itemId, next)
        }
    }

    fun clearCart() {
        _cartQuantities.value = emptyMap()
    }

    private fun runMutation(successMsg: String? = null, block: suspend () -> Result<Any>) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = block()
                res.onSuccess {
                    if (!successMsg.isNullOrBlank()) _snackbarMessage.value = successMsg
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Action failed. Please check details."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveOwnProfile(name: String, phone: String, address: String, notificationsEnabled: Boolean) {
        runMutation("Profile saved") {
            repo.updateOwnProfile(name, phone, address, notificationsEnabled)
        }
    }

    fun placeCodOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        customerLat: Double,
        customerLng: Double,
        locationLabel: String,
        hasLocation: Boolean,
        redeemPoints: Long,
        notes: String,
        onPlaced: () -> Unit
    ) {
        val lines = cartLines.value
        val currentPts = userProfile.value?.loyaltyPoints ?: 0L
        val pointValue = config.value?.loyaltyPointValuePkr ?: 5L
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repo.placeCodOrder(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    customerLat = customerLat,
                    customerLng = customerLng,
                    locationLabel = locationLabel,
                    hasLocation = hasLocation,
                    cartLines = lines,
                    redeemPoints = redeemPoints,
                    currentLoyaltyPoints = currentPts,
                    pointValuePkr = pointValue,
                    notes = notes
                )
                res.onSuccess { orderNum ->
                    clearCart()
                    _snackbarMessage.value = "COD Order $orderNum placed with live GPS pinned for our Rider!"
                    onPlaced()
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not place COD order."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelOwnOrder(orderId: String) {
        runMutation("Order cancelled") { repo.cancelOwnOrder(orderId) }
    }

    fun createReservation(
        customerName: String,
        customerPhone: String,
        dateLabel: String,
        timeSlot: String,
        partySize: Long,
        seatingZone: SeatingZone,
        specialNotes: String,
        onBooked: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repo.createReservation(
                    customerName, customerPhone, dateLabel, timeSlot, partySize, seatingZone, specialNotes
                )
                res.onSuccess {
                    _snackbarMessage.value = "Table reservation requested for $dateLabel ($timeSlot)!"
                    onBooked()
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Unable to book table."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelOwnReservation(reservationId: String) {
        runMutation("Reservation cancelled") { repo.cancelOwnReservation(reservationId) }
    }

    fun submitFeedback(rating: Long, category: FeedbackCategory, comment: String, onSubmitted: () -> Unit) {
        val name = userProfile.value?.displayName?.ifBlank { userDisplayName } ?: userDisplayName
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repo.submitFeedback(name, rating, category, comment)
                res.onSuccess {
                    _snackbarMessage.value = "Thank you for reviewing Oak & Smoke Hasilpur!"
                    onSubmitted()
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not submit feedback."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repo.markNotificationRead(id)
        }
    }

    fun initializeOwner(ownerStaffId: String, ownerPasscode: String, seedCatalog: Boolean, onDone: () -> Unit) {
        val ownerName = userProfile.value?.displayName?.ifBlank { userDisplayName } ?: userDisplayName
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = adminRepo.initializeOwnerAndSeed(ownerName, ownerStaffId, ownerPasscode, seedCatalog)
                res.onSuccess {
                    _portalMode.value = PortalMode.OWNER
                    _snackbarMessage.value = "Oak & Smoke Owner Portal initialized!"
                    onDone()
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Owner setup failed."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun unlockStaffPortal(staffId: String, passcode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repo.unlockStaffPortal(staffId, passcode)
                res.onSuccess { role ->
                    _portalMode.value = when (role) {
                        UserRoleTier.OWNER, UserRoleTier.MANAGER -> PortalMode.OWNER
                        UserRoleTier.RIDER -> PortalMode.RIDER
                        else -> PortalMode.STAFF
                    }
                    _snackbarMessage.value = "Unlocked ${role.displayLabel} workspace ($staffId)."
                    onSuccess()
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Invalid Staff/Rider ID or Passcode."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun lockStaffPortal() {
        runMutation("Returned to Customer mode") {
            _portalMode.value = PortalMode.CUSTOMER
            repo.lockStaffPortal()
        }
    }

    // --- STAFF & OWNER OPERATIONS ---
    fun createStaffOrderDirectToKitchen(
        orderType: OrderType,
        tableLabel: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        cartLines: List<CartLine>,
        notes: String,
        onCreated: (OrderRecord) -> Unit
    ) {
        val staffId = roleGrant.value?.staffId?.ifBlank { if (isBootstrapAdmin) "OWNER" else "STAFF" } ?: "STAFF"
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = adminRepo.createStaffDirectToKitchenOrder(
                    orderType = orderType,
                    tableLabel = tableLabel,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    cartLines = cartLines,
                    staffId = staffId,
                    notes = notes
                )
                res.onSuccess { createdOrder ->
                    _snackbarMessage.value = "Order ${createdOrder.orderNumber} sent direct to Kitchen!"
                    onCreated(createdOrder)
                }.onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Unable to send order to kitchen."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun advanceOrderStatus(order: OrderRecord, nextStatus: OrderStatus) {
        val staffId = roleGrant.value?.staffId?.ifBlank { if (isBootstrapAdmin) "OWNER" else "STAFF" } ?: "STAFF"
        val staffDocId = roleGrant.value?.staffDocId ?: ownStaffRecord.value?.id.orEmpty()
        val earnRate = config.value?.loyaltyEarnPer100Pkr ?: 1L
        val riderName = ownStaffRecord.value?.fullName?.ifBlank { userDisplayName } ?: userDisplayName
        val riderPhone = ownStaffRecord.value?.phone.orEmpty()
        val successMsg = if (nextStatus == OrderStatus.READY && order.orderType == OrderType.COD_DELIVERY.name) {
            "Order ${order.orderNumber} READY! Sound alert sent to Rider & Customer."
        } else {
            "Order ${order.orderNumber} -> ${nextStatus.displayLabel}"
        }
        runMutation(successMsg) {
            adminRepo.advanceOrderStatus(
                order = order,
                nextStatus = nextStatus,
                staffId = staffId,
                staffDocId = staffDocId,
                earnPer100Pkr = earnRate,
                riderName = riderName,
                riderPhone = riderPhone
            )
        }
    }

    fun assignRiderToOrder(order: OrderRecord, rider: StaffMember) {
        runMutation("Assigned Rider ${rider.fullName} to ${order.orderNumber} & sent alerts!") {
            adminRepo.assignRiderToOrder(order, rider)
        }
    }

    fun riderClaimAndStartDelivery(order: OrderRecord) {
        advanceOrderStatus(order, OrderStatus.OUT_FOR_DELIVERY)
    }

    fun updateReservationStatus(reservation: ReservationRecord, status: ReservationStatus) {
        runMutation("Reservation -> ${status.displayLabel}") {
            adminRepo.updateReservationStatus(reservation, status)
        }
    }

    fun replyToFeedback(feedback: FeedbackEntry, reply: String) {
        runMutation("Reply sent to ${feedback.customerName}") {
            adminRepo.replyToFeedback(feedback, reply)
        }
    }

    fun adjustInventory(item: InventoryItem, delta: Long) {
        runMutation("Updated ${item.name} stock") {
            adminRepo.adjustInventoryQuantity(item, delta)
        }
    }

    fun addInventoryItem(name: String, category: InventoryCategory, quantity: Long, unit: String, minThreshold: Long, unitCostPkr: Long) {
        runMutation("Added $name to inventory") {
            adminRepo.addInventoryItem(name, category, quantity, unit, minThreshold, unitCostPkr)
        }
    }

    fun saveMenuItem(
        existingId: String?,
        name: String,
        category: MenuCategory,
        description: String,
        pricePkr: Long,
        imageKey: String,
        dailySpecial: Boolean,
        available: Boolean,
        spiceLevel: SpiceLevel,
        prepMinutes: Long,
        broadcastAlert: Boolean
    ) {
        runMutation(if (existingId == null) "Added $name to menu" else "Updated $name") {
            adminRepo.saveMenuItem(
                existingId, name, category, description, pricePkr, imageKey,
                dailySpecial, available, spiceLevel, prepMinutes, broadcastAlert
            )
        }
    }

    fun deleteMenuItem(itemId: String) {
        runMutation("Dish removed from menu") { adminRepo.deleteMenuItem(itemId) }
    }

    fun saveStaffMember(
        existing: StaffMember?,
        staffId: String,
        fullName: String,
        newPasscode: String,
        roleTier: UserRoleTier,
        monthlySalaryPkr: Long,
        shiftSummary: String,
        scheduleDays: String,
        loyaltyPoints: Long,
        active: Boolean,
        phone: String = "",
        vehicleInfo: String = ""
    ) {
        val label = if (roleTier == UserRoleTier.RIDER) "Rider" else "Staff"
        runMutation(if (existing == null) "Created $label $staffId" else "Updated $fullName") {
            adminRepo.createOrUpdateStaffMember(
                existing = existing,
                staffId = staffId,
                fullName = fullName,
                newPasscode = newPasscode,
                roleTier = roleTier,
                monthlySalaryPkr = monthlySalaryPkr,
                shiftSummary = shiftSummary,
                scheduleDays = scheduleDays,
                loyaltyPoints = loyaltyPoints,
                active = active,
                phone = phone,
                vehicleInfo = vehicleInfo
            )
        }
    }

    fun payStaffSalary(staff: StaffMember, logExpense: Boolean) {
        runMutation("Marked ${staff.fullName}'s salary as Paid") {
            adminRepo.recordStaffSalaryPayout(staff, logExpense)
        }
    }

    fun editCustomerLoyalty(customer: UserProfile, newPoints: Long, reason: String) {
        runMutation("Updated ${customer.displayName}'s loyalty points to $newPoints") {
            adminRepo.adminEditCustomerLoyalty(customer, newPoints, reason)
        }
    }

    fun editStaffLoyalty(staff: StaffMember, newPoints: Long, reason: String) {
        runMutation("Updated ${staff.fullName}'s staff loyalty points to $newPoints") {
            adminRepo.adminEditStaffLoyalty(staff, newPoints, reason)
        }
    }

    fun addExpense(title: String, category: ExpenseCategory, amountPkr: Long, notes: String) {
        runMutation("Logged expense: $title") {
            adminRepo.addExpense(title, category, amountPkr, notes)
        }
    }

    fun deleteExpense(expenseId: String) {
        runMutation("Expense deleted") { adminRepo.deleteExpense(expenseId) }
    }

    fun toggleAutoPrint(enabled: Boolean) {
        runMutation(if (enabled) "Auto-print daily summary enabled" else "Auto-print daily summary disabled") {
            adminRepo.toggleAutoPrintSetting(enabled)
        }
    }

    fun saveDiningTable(
        existingId: String?,
        label: String,
        zone: SeatingZone,
        capacity: Long,
        available: Boolean
    ) {
        val nextOrder = if (existingId == null) (diningTables.value.maxOfOrNull { it.displayOrder } ?: 0L) + 1L
        else diningTables.value.firstOrNull { it.id == existingId }?.displayOrder ?: 1L
        runMutation(if (existingId == null) "Added table: ${label.trim()}" else "Updated table: ${label.trim()}") {
            adminRepo.saveDiningTable(existingId, label, zone, capacity, available, nextOrder)
        }
    }

    fun toggleDiningTableAvailability(table: DiningTable, available: Boolean) {
        runMutation("${table.label} marked ${if (available) "Available" else "Unavailable"}") {
            adminRepo.toggleDiningTableAvailability(table, available)
        }
    }

    fun deleteDiningTable(table: DiningTable) {
        runMutation("Removed ${table.label}") {
            adminRepo.deleteDiningTable(table.id)
        }
    }

    fun seedStarterTables() {
        runMutation("Loaded default Oak & Smoke tables & cabins") {
            adminRepo.seedStarterTables()
        }
    }
}

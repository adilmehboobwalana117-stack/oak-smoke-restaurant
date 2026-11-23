package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

class AdminFinanceRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logAndRethrow(op: OperationType, path: String, expectedUid: String): Flow<T> =
        catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val toLog = fsError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    fun observeInventory(uid: String): Flow<List<InventoryItem>> = flow {
        val path = "inventory"
        emitAll(
            db.collection("inventory")
                .snapshots()
                .map { snap ->
                    snap.toObjects(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<InventoryItem> { it.isLowStock() }.thenBy { it.name })
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeAllStaffMembers(uid: String): Flow<List<StaffMember>> = flow {
        val path = "staffMembers"
        emitAll(
            db.collection("staffMembers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.staffId }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeOwnStaffRecord(uid: String): Flow<StaffMember?> = flow {
        val path = "staffMembers"
        emitAll(
            db.collection("staffMembers")
                .whereEqualTo("linkedUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .firstOrNull()
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeExpenses(uid: String): Flow<List<ExpenseEntry>> = flow {
        val path = "expenses"
        emitAll(
            db.collection("expenses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExpenseEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeAllUsers(uid: String): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.loyaltyPoints }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    suspend fun initializeOwnerAndSeed(
        ownerName: String,
        ownerStaffId: String,
        ownerPasscode: String,
        seedStarterCatalog: Boolean
    ): Result<Unit> {
        val path = "restaurantConfig/oak_and_smoke"
        return try {
            val uid = requireUserId()
            val cleanStaffId = ownerStaffId.trim().uppercase(Locale.US)
            require(cleanStaffId.length in 2..32) { "Enter an Owner Staff ID (e.g., OS-001)." }
            require(ownerPasscode.trim().length >= 4) { "Passcode must be at least 4 characters." }

            val credHash = computeStaffCredHash(cleanStaffId, ownerPasscode)
            val batch = db.batch()

            batch.set(
                db.collection("restaurantConfig").document("oak_and_smoke"),
                mapOf(
                    "restaurantName" to "Oak & Smoke Restaurant Hasilpur",
                    "currency" to "PKR",
                    "ownerUid" to uid,
                    "ownerInitialized" to true,
                    "autoPrintDailyReport" to false,
                    "loyaltyEarnPer100Pkr" to 1L,
                    "loyaltyPointValuePkr" to 5L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("roleGrants").document(uid),
                mapOf(
                    "userId" to uid,
                    "role" to UserRoleTier.OWNER.name,
                    "staffDocId" to "owner_master",
                    "staffId" to cleanStaffId,
                    "credHash" to credHash,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("staffCredentials").document(credHash),
                mapOf(
                    "staffDocId" to "owner_master",
                    "staffId" to cleanStaffId,
                    "roleTier" to UserRoleTier.OWNER.name,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("staffMembers").document("owner_master"),
                mapOf(
                    "staffId" to cleanStaffId,
                    "fullName" to ownerName.trim().ifBlank { "Oak & Smoke Owner" }.take(80),
                    "roleTier" to UserRoleTier.OWNER.name,
                    "credHash" to credHash,
                    "linkedUid" to uid,
                    "monthlySalaryPkr" to 0L,
                    "salaryStatus" to "PAID",
                    "loyaltyPoints" to 0L,
                    "shiftSummary" to "Full Restaurant Oversight",
                    "scheduleDays" to "Mon - Sun",
                    "ordersHandledCount" to 0L,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (seedStarterCatalog) {
                OakAndSmokeSeeds.starterMenu.forEach { dish ->
                    batch.set(
                        db.collection("menuItems").document(dish.id),
                        mapOf(
                            "catalogType" to "MENU",
                            "name" to dish.name,
                            "category" to dish.category,
                            "description" to dish.description,
                            "pricePkr" to dish.pricePkr,
                            "imageKey" to dish.imageKey,
                            "dailySpecial" to dish.dailySpecial,
                            "available" to dish.available,
                            "spiceLevel" to dish.spiceLevel,
                            "prepMinutes" to dish.prepMinutes,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                OakAndSmokeSeeds.starterInventory.forEach { stock ->
                    batch.set(
                        db.collection("inventory").document(stock.id),
                        mapOf(
                            "name" to stock.name,
                            "category" to stock.category,
                            "quantity" to stock.quantity,
                            "unit" to stock.unit,
                            "minThreshold" to stock.minThreshold,
                            "unitCostPkr" to stock.unitCostPkr,
                            "updatedByUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                OakAndSmokeSeeds.starterTables.forEach { tbl ->
                    batch.set(
                        db.collection("diningTables").document(tbl.id),
                        mapOf(
                            "catalogType" to "TABLE",
                            "label" to tbl.label,
                            "zone" to tbl.zone,
                            "capacity" to tbl.capacity,
                            "available" to tbl.available,
                            "displayOrder" to tbl.displayOrder,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createStaffDirectToKitchenOrder(
        orderType: OrderType,
        tableLabel: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        cartLines: List<CartLine>,
        staffId: String,
        notes: String
    ): Result<OrderRecord> {
        var path = "orders"
        return try {
            val uid = requireUserId()
            require(cartLines.isNotEmpty()) { "Add at least one dish to send to the kitchen." }
            val cleanTable = tableLabel.trim().take(40)
            if (orderType == OrderType.DINE_IN) {
                require(cleanTable.isNotBlank()) { "Select or enter a Table / Cabin label for Dine-In." }
            }
            val cleanPhone = customerPhone.trim().take(30)
            val cleanAddress = deliveryAddress.trim().take(240)
            if (orderType == OrderType.COD_DELIVERY) {
                require(cleanPhone.length >= 7) { "Enter customer phone for COD Delivery." }
                require(cleanAddress.length >= 4) { "Enter delivery address for COD Delivery." }
            }

            val subtotal = cartLines.sumOf { it.item.pricePkr * it.quantity }
            val itemCount = cartLines.sumOf { it.quantity }.toLong().coerceIn(1L, 100L)
            val summary = cartLines.joinToString(", ") { "${it.quantity}x ${it.item.name}" }.take(1000)
            val detail = encodeOrderLines(cartLines)
            val orderNum = "OS-${Random.nextInt(1000, 9999)}"
            val cleanStaffId = staffId.trim().ifBlank { "STAFF" }.take(32)
            val cleanCustomerName = customerName.trim().ifBlank {
                when (orderType) {
                    OrderType.DINE_IN -> "Dine-In ($cleanTable)"
                    OrderType.TAKEAWAY -> "Takeaway Guest"
                    OrderType.COD_DELIVERY -> "Phone COD Guest"
                }
            }.take(80)
            val paymentMethod = if (orderType == OrderType.COD_DELIVERY) "COD" else "CASH_COUNTER"
            val cleanNotes = notes.trim().take(300)
            val dateKey = todayDateKey()
            val monthKey = currentMonthKey()

            val orderRef = db.collection("orders").document()
            path = orderRef.path
            val batch = db.batch()
            batch.set(
                orderRef,
                mapOf(
                    "orderNumber" to orderNum,
                    "userId" to uid,
                    "orderType" to orderType.name,
                    "tableLabel" to cleanTable,
                    "customerName" to cleanCustomerName,
                    "customerPhone" to cleanPhone,
                    "deliveryAddress" to cleanAddress,
                    "customerLat" to 0.0,
                    "customerLng" to 0.0,
                    "locationLabel" to "",
                    "hasLocation" to false,
                    "assignedRiderId" to "",
                    "assignedRiderName" to "",
                    "assignedRiderPhone" to "",
                    "itemsSummary" to summary,
                    "itemsDetail" to detail,
                    "itemCount" to itemCount,
                    "subtotalPkr" to subtotal,
                    "loyaltyPointsRedeemed" to 0L,
                    "discountPkr" to 0L,
                    "totalPkr" to subtotal,
                    "paymentMethod" to paymentMethod,
                    "status" to OrderStatus.GRILLING.name,
                    "handledByStaffId" to cleanStaffId,
                    "notes" to cleanNotes,
                    "dateKey" to dateKey,
                    "monthKey" to monthKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Kitchen/Staff live notification (order details ONLY — no billing/prices!)
            val kitchenLocation = if (cleanTable.isNotBlank()) " · $cleanTable" else ""
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "targetUid" to "ROLE_STAFF",
                    "audience" to "STAFF",
                    "title" to "KOT Direct to Kitchen: $orderNum",
                    "message" to "${orderType.displayLabel}$kitchenLocation — $summary".take(400),
                    "type" to "ORDER",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(
                OrderRecord(
                    id = orderRef.id,
                    orderNumber = orderNum,
                    userId = uid,
                    orderType = orderType.name,
                    tableLabel = cleanTable,
                    customerName = cleanCustomerName,
                    customerPhone = cleanPhone,
                    deliveryAddress = cleanAddress,
                    itemsSummary = summary,
                    itemsDetail = detail,
                    itemCount = itemCount,
                    subtotalPkr = subtotal,
                    loyaltyPointsRedeemed = 0L,
                    discountPkr = 0L,
                    totalPkr = subtotal,
                    paymentMethod = paymentMethod,
                    status = OrderStatus.GRILLING.name,
                    handledByStaffId = cleanStaffId,
                    notes = cleanNotes,
                    dateKey = dateKey,
                    monthKey = monthKey,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStatus(
        order: OrderRecord,
        nextStatus: OrderStatus,
        staffId: String,
        staffDocId: String,
        earnPer100Pkr: Long,
        riderName: String = "",
        riderPhone: String = ""
    ): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            val updateFields = mutableMapOf<String, Any>(
                "status" to nextStatus.name,
                "handledByStaffId" to staffId.ifBlank { "STAFF" }.take(32),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // If a Rider picks up an unassigned order to start delivery, attach their Rider details
            if (nextStatus == OrderStatus.OUT_FOR_DELIVERY && order.assignedRiderId.isBlank() && riderName.isNotBlank()) {
                updateFields["assignedRiderId"] = staffId.ifBlank { "RIDER" }.take(32)
                updateFields["assignedRiderName"] = riderName.trim().take(80)
                updateFields["assignedRiderPhone"] = riderPhone.trim().take(30)
            }
            db.collection("orders").document(order.id).update(updateFields).await()

            val isCodDelivery = order.orderType == OrderType.COD_DELIVERY.name
            val isCustomerOnlineOrder = isCodDelivery && order.userId != uid
            val earnedPts = if (nextStatus == OrderStatus.DELIVERED && isCustomerOnlineOrder && earnPer100Pkr > 0L) {
                (order.totalPkr / 100L) * earnPer100Pkr
            } else 0L

            val customerNotifType = if (nextStatus == OrderStatus.READY) "RIDER_READY" else "ORDER"
            val customerTitle = when (nextStatus) {
                OrderStatus.READY -> "Order ${order.orderNumber} is READY!"
                OrderStatus.OUT_FOR_DELIVERY -> "Rider On The Way · ${order.orderNumber}"
                else -> "Order ${order.orderNumber}: ${nextStatus.displayLabel}"
            }
            val customerMsg = when {
                nextStatus == OrderStatus.READY && isCodDelivery ->
                    "Your Oak & Smoke order ${order.orderNumber} is hot and READY! Our Rider has been alerted with your pinned GPS location for COD delivery (${formatPkr(order.totalPkr)})."
                nextStatus == OrderStatus.READY ->
                    "Order ${order.orderNumber} is fresh off the grill and READY to serve!"
                nextStatus == OrderStatus.OUT_FOR_DELIVERY -> {
                    val activeRider = order.assignedRiderName.ifBlank { riderName }
                    val rInfo = if (activeRider.isNotBlank()) "Rider $activeRider" else "Our Rider"
                    "$rInfo is on the way with Order ${order.orderNumber} (${formatPkr(order.totalPkr)} COD)."
                }
                earnedPts > 0L ->
                    "Order ${order.orderNumber} is now ${nextStatus.displayLabel}. You earned +$earnedPts Loyalty Points!"
                else ->
                    "Order ${order.orderNumber} status updated to ${nextStatus.displayLabel}."
            }

            // Send live notification to Customer
            db.collection("notifications").document().set(
                mapOf(
                    "targetUid" to order.userId,
                    "audience" to "USER",
                    "title" to customerTitle.take(120),
                    "message" to customerMsg.take(400),
                    "type" to customerNotifType,
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // When a COD Delivery order becomes READY, send immediate SOUND alert to Riders!
            if (nextStatus == OrderStatus.READY && isCodDelivery) {
                val gpsTag = if (order.hasLocation) {
                    "GPS: ${formatCoordinates(order.customerLat, order.customerLng)}"
                } else {
                    "Address: ${order.deliveryAddress}"
                }
                db.collection("notifications").document().set(
                    mapOf(
                        "targetUid" to "ROLE_RIDER",
                        "audience" to "RIDER",
                        "title" to "ORDER READY FOR PICKUP: ${order.orderNumber}",
                        "message" to "${order.customerName} (${formatPkr(order.totalPkr)} COD) · $gpsTag · ${order.deliveryAddress}".take(400),
                        "type" to "RIDER_READY",
                        "read" to false,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Award customer loyalty points & increment staff/rider handled orders on DELIVERED
            if (nextStatus == OrderStatus.DELIVERED) {
                if (earnedPts > 0L && order.userId.isNotBlank()) {
                    try {
                        db.collection("users").document(order.userId).update(
                            mapOf(
                                "loyaltyPoints" to FieldValue.increment(earnedPts),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.UPDATE, "users/${order.userId}")
                    }
                }
                if (staffDocId.isNotBlank()) {
                    try {
                        db.collection("staffMembers").document(staffDocId).update(
                            mapOf(
                                "ordersHandledCount" to FieldValue.increment(1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.UPDATE, "staffMembers/$staffDocId")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun assignRiderToOrder(order: OrderRecord, rider: StaffMember): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("orders").document(order.id),
                mapOf(
                    "assignedRiderId" to rider.staffId.take(32),
                    "assignedRiderName" to rider.fullName.take(80),
                    "assignedRiderPhone" to rider.phone.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val gpsTag = if (order.hasLocation) {
                "GPS pinned (${formatCoordinates(order.customerLat, order.customerLng)})"
            } else {
                "Address: ${order.deliveryAddress}"
            }
            // Sound notification to Rider fleet (or linked rider)
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "targetUid" to rider.linkedUid.ifBlank { "ROLE_RIDER" },
                    "audience" to "RIDER",
                    "title" to "Delivery Assigned to ${rider.fullName}: ${order.orderNumber}",
                    "message" to "Customer: ${order.customerName} (${formatPkr(order.totalPkr)} COD) · $gpsTag · ${order.deliveryAddress}".take(400),
                    "type" to "RIDER_READY",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            // Notify Customer that a Rider has been assigned
            val riderContact = if (rider.phone.isNotBlank()) " (${rider.phone})" else ""
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "targetUid" to order.userId,
                    "audience" to "USER",
                    "title" to "Rider Assigned: ${order.orderNumber}",
                    "message" to "Rider ${rider.fullName}$riderContact has been assigned to deliver your order to your pinned location.".take(400),
                    "type" to "RIDER_READY",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateReservationStatus(reservation: ReservationRecord, newStatus: ReservationStatus): Result<Unit> {
        val path = "reservations/${reservation.id}"
        return try {
            val uid = requireUserId()
            db.collection("reservations").document(reservation.id).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("notifications").document().set(
                mapOf(
                    "targetUid" to reservation.userId,
                    "audience" to "USER",
                    "title" to "Table Booking ${newStatus.displayLabel}",
                    "message" to "Your table for ${reservation.partySize} on ${reservation.dateLabel} (${reservation.timeSlot}) is ${newStatus.displayLabel}.",
                    "type" to "RESERVATION",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun replyToFeedback(feedback: FeedbackEntry, reply: String): Result<Unit> {
        val path = "feedback/${feedback.id}"
        return try {
            val uid = requireUserId()
            val cleanReply = reply.trim()
            require(cleanReply.isNotEmpty()) { "Enter a reply message." }
            db.collection("feedback").document(feedback.id).update(
                mapOf(
                    "staffReply" to cleanReply.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("notifications").document().set(
                mapOf(
                    "targetUid" to feedback.userId,
                    "audience" to "USER",
                    "title" to "Oak & Smoke Replied to Your Review",
                    "message" to cleanReply.take(380),
                    "type" to "ORDER",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMenuItem(
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
        broadcastSpecialAlert: Boolean
    ): Result<Unit> {
        var path = "menuItems"
        return try {
            val uid = requireUserId()
            require(name.trim().length in 2..80) { "Dish name must be 2-80 characters." }
            require(pricePkr in 50L..250000L) { "Price must be between Rs. 50 and Rs. 250,000." }
            if (existingId.isNullOrBlank()) {
                val ref = db.collection("menuItems").document()
                path = ref.path
                ref.set(
                    mapOf(
                        "catalogType" to "MENU",
                        "name" to name.trim().take(80),
                        "category" to category.name,
                        "description" to description.trim().take(300),
                        "pricePkr" to pricePkr,
                        "imageKey" to imageKey.ifBlank { "karahi" }.take(60),
                        "dailySpecial" to dailySpecial,
                        "available" to available,
                        "spiceLevel" to spiceLevel.name,
                        "prepMinutes" to prepMinutes.coerceIn(5L, 180L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                path = "menuItems/$existingId"
                db.collection("menuItems").document(existingId).update(
                    mapOf(
                        "name" to name.trim().take(80),
                        "category" to category.name,
                        "description" to description.trim().take(300),
                        "pricePkr" to pricePkr,
                        "imageKey" to imageKey.ifBlank { "karahi" }.take(60),
                        "dailySpecial" to dailySpecial,
                        "available" to available,
                        "spiceLevel" to spiceLevel.name,
                        "prepMinutes" to prepMinutes.coerceIn(5L, 180L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            if (dailySpecial && broadcastSpecialAlert) {
                db.collection("notifications").document().set(
                    mapOf(
                        "targetUid" to "BROADCAST_ALL",
                        "audience" to "BROADCAST",
                        "title" to "Today's Special: ${name.trim().take(60)}",
                        "message" to "Fresh from Oak & Smoke Hasilpur for ${formatPkr(pricePkr)}! ${description.trim().take(200)}",
                        "type" to "SPECIAL",
                        "read" to false,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> {
        val path = "menuItems/$itemId"
        return try {
            requireUserId()
            db.collection("menuItems").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustInventoryQuantity(item: InventoryItem, delta: Long): Result<Unit> {
        val path = "inventory/${item.id}"
        return try {
            val uid = requireUserId()
            val newQty = (item.quantity + delta).coerceIn(0L, 100000L)
            db.collection("inventory").document(item.id).update(
                mapOf(
                    "quantity" to newQty,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addInventoryItem(
        name: String,
        category: InventoryCategory,
        quantity: Long,
        unit: String,
        minThreshold: Long,
        unitCostPkr: Long
    ): Result<Unit> {
        var path = "inventory"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Enter stock item name." }
            val ref = db.collection("inventory").document()
            path = ref.path
            ref.set(
                mapOf(
                    "name" to name.trim().take(80),
                    "category" to category.name,
                    "quantity" to quantity.coerceIn(0L, 100000L),
                    "unit" to unit.trim().ifBlank { "kg" }.take(24),
                    "minThreshold" to minThreshold.coerceIn(0L, 10000L),
                    "unitCostPkr" to unitCostPkr.coerceIn(0L, 500000L),
                    "updatedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateStaffMember(
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
    ): Result<Unit> {
        var path = "staffMembers"
        return try {
            val uid = requireUserId()
            val cleanStaffId = staffId.trim().uppercase(Locale.US)
            require(cleanStaffId.length in 2..32) { "Staff/Rider ID must be 2-32 characters (e.g. OS-102 or RD-101)." }
            require(fullName.trim().length in 2..80) { "Full name is required." }
            val validTier = if (roleTier == UserRoleTier.CUSTOMER) UserRoleTier.STAFF else roleTier
            val cleanPhone = phone.trim().take(30)
            val cleanVehicle = vehicleInfo.trim().take(80)

            val batch = db.batch()
            if (existing == null) {
                require(newPasscode.trim().length >= 4) { "Passcode must be at least 4 characters." }
                val docRef = db.collection("staffMembers").document()
                val credHash = computeStaffCredHash(cleanStaffId, newPasscode)
                path = docRef.path
                batch.set(
                    docRef,
                    mapOf(
                        "staffId" to cleanStaffId,
                        "fullName" to fullName.trim().take(80),
                        "roleTier" to validTier.name,
                        "credHash" to credHash,
                        "linkedUid" to "",
                        "monthlySalaryPkr" to monthlySalaryPkr.coerceIn(0L, 5000000L),
                        "salaryStatus" to "PENDING",
                        "loyaltyPoints" to loyaltyPoints.coerceIn(0L, 1000000L),
                        "shiftSummary" to shiftSummary.trim().ifBlank { "Evening 4 PM - 12 AM" }.take(120),
                        "scheduleDays" to scheduleDays.trim().ifBlank { "Mon - Sat" }.take(160),
                        "ordersHandledCount" to 0L,
                        "phone" to cleanPhone,
                        "vehicleInfo" to cleanVehicle,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    db.collection("staffCredentials").document(credHash),
                    mapOf(
                        "staffDocId" to docRef.id,
                        "staffId" to cleanStaffId,
                        "roleTier" to validTier.name,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                path = "staffMembers/${existing.id}"
                if (cleanStaffId != existing.staffId && newPasscode.trim().length < 4) {
                    return Result.failure(
                        IllegalArgumentException("Enter a new passcode (min 4 chars) when changing Staff/Rider ID.")
                    )
                }
                val newHash = if (newPasscode.trim().length >= 4) {
                    computeStaffCredHash(cleanStaffId, newPasscode)
                } else existing.credHash

                batch.update(
                    db.collection("staffMembers").document(existing.id),
                    mapOf(
                        "staffId" to cleanStaffId,
                        "fullName" to fullName.trim().take(80),
                        "roleTier" to validTier.name,
                        "credHash" to newHash,
                        "monthlySalaryPkr" to monthlySalaryPkr.coerceIn(0L, 5000000L),
                        "shiftSummary" to shiftSummary.trim().take(120),
                        "scheduleDays" to scheduleDays.trim().take(160),
                        "loyaltyPoints" to loyaltyPoints.coerceIn(0L, 1000000L),
                        "phone" to cleanPhone,
                        "vehicleInfo" to cleanVehicle,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (newHash != existing.credHash) {
                    if (existing.credHash.isNotBlank()) {
                        batch.delete(db.collection("staffCredentials").document(existing.credHash))
                    }
                    batch.set(
                        db.collection("staffCredentials").document(newHash),
                        mapOf(
                            "staffDocId" to existing.id,
                            "staffId" to cleanStaffId,
                            "roleTier" to validTier.name,
                            "active" to active,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (existing.credHash.isNotBlank()) {
                    batch.update(
                        db.collection("staffCredentials").document(existing.credHash),
                        mapOf(
                            "roleTier" to validTier.name,
                            "active" to active,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (existing.linkedUid.isNotBlank()) {
                    if (!active) {
                        batch.delete(db.collection("roleGrants").document(existing.linkedUid))
                    }
                    batch.set(
                        db.collection("notifications").document(),
                        mapOf(
                            "targetUid" to existing.linkedUid,
                            "audience" to "USER",
                            "title" to "Staff Schedule & Profile Updated",
                            "message" to "Shift: ${shiftSummary.trim()} | Loyalty: $loyaltyPoints pts | Salary: ${formatPkr(monthlySalaryPkr)}",
                            "type" to "STAFF",
                            "read" to false,
                            "createdByUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordStaffSalaryPayout(staff: StaffMember, alsoLogExpense: Boolean): Result<Unit> {
        val path = "staffMembers/${staff.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("staffMembers").document(staff.id),
                mapOf(
                    "salaryStatus" to "PAID",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (alsoLogExpense && staff.monthlySalaryPkr > 0L) {
                batch.set(
                    db.collection("expenses").document(),
                    mapOf(
                        "title" to "Salary: ${staff.fullName} (${staff.staffId})".take(100),
                        "category" to ExpenseCategory.SALARIES.name,
                        "amountPkr" to staff.monthlySalaryPkr,
                        "dateKey" to todayDateKey(),
                        "monthKey" to currentMonthKey(),
                        "notes" to "Monthly salary payout logged from Oak & Smoke Payroll.",
                        "recordedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (staff.linkedUid.isNotBlank()) {
                batch.set(
                    db.collection("notifications").document(),
                    mapOf(
                        "targetUid" to staff.linkedUid,
                        "audience" to "USER",
                        "title" to "Salary Marked Paid",
                        "message" to "Your monthly salary of ${formatPkr(staff.monthlySalaryPkr)} has been marked Paid.",
                        "type" to "STAFF",
                        "read" to false,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminEditCustomerLoyalty(customer: UserProfile, newPoints: Long, reason: String): Result<Unit> {
        val path = "users/${customer.userId}"
        return try {
            val uid = requireUserId()
            val safePts = newPoints.coerceIn(0L, 1000000L)
            val batch = db.batch()
            batch.update(
                db.collection("users").document(customer.userId),
                mapOf(
                    "loyaltyPoints" to safePts,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "targetUid" to customer.userId,
                    "audience" to "USER",
                    "title" to "Loyalty Balance Updated",
                    "message" to "Your Oak & Smoke loyalty balance is now $safePts pts. ${reason.trim().ifBlank { "Updated by Management." }}",
                    "type" to "LOYALTY",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminEditStaffLoyalty(staff: StaffMember, newPoints: Long, reason: String): Result<Unit> {
        val path = "staffMembers/${staff.id}"
        return try {
            val uid = requireUserId()
            val safePts = newPoints.coerceIn(0L, 1000000L)
            val batch = db.batch()
            batch.update(
                db.collection("staffMembers").document(staff.id),
                mapOf(
                    "loyaltyPoints" to safePts,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (staff.linkedUid.isNotBlank()) {
                batch.set(
                    db.collection("notifications").document(),
                    mapOf(
                        "targetUid" to staff.linkedUid,
                        "audience" to "USER",
                        "title" to "Staff Loyalty Points Updated",
                        "message" to "Your staff reward balance is now $safePts pts. ${reason.trim().ifBlank { "Updated by Owner." }}",
                        "type" to "LOYALTY",
                        "read" to false,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        title: String,
        category: ExpenseCategory,
        amountPkr: Long,
        notes: String
    ): Result<Unit> {
        var path = "expenses"
        return try {
            val uid = requireUserId()
            require(title.trim().length >= 2) { "Enter expense title." }
            require(amountPkr in 1L..10000000L) { "Enter a valid PKR amount." }
            val ref = db.collection("expenses").document()
            path = ref.path
            ref.set(
                mapOf(
                    "title" to title.trim().take(100),
                    "category" to category.name,
                    "amountPkr" to amountPkr,
                    "dateKey" to todayDateKey(),
                    "monthKey" to currentMonthKey(),
                    "notes" to notes.trim().take(300),
                    "recordedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        val path = "expenses/$expenseId"
        return try {
            requireUserId()
            db.collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleAutoPrintSetting(enabled: Boolean): Result<Unit> {
        val path = "restaurantConfig/oak_and_smoke"
        return try {
            requireUserId()
            db.collection("restaurantConfig").document("oak_and_smoke").update(
                mapOf(
                    "autoPrintDailyReport" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveDiningTable(
        existingId: String?,
        label: String,
        zone: SeatingZone,
        capacity: Long,
        available: Boolean,
        displayOrder: Long
    ): Result<Unit> {
        var path = "diningTables"
        return try {
            requireUserId()
            val cleanLabel = label.trim()
            require(cleanLabel.length in 2..40) { "Table name must be 2–40 characters (e.g. Table 9 or VIP Cabin 3)." }
            val safeCap = capacity.coerceIn(1L, 30L)
            val safeOrder = displayOrder.coerceIn(0L, 1000L)
            if (existingId.isNullOrBlank()) {
                val ref = db.collection("diningTables").document()
                path = ref.path
                ref.set(
                    mapOf(
                        "catalogType" to "TABLE",
                        "label" to cleanLabel,
                        "zone" to zone.name,
                        "capacity" to safeCap,
                        "available" to available,
                        "displayOrder" to safeOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                path = "diningTables/$existingId"
                db.collection("diningTables").document(existingId).update(
                    mapOf(
                        "label" to cleanLabel,
                        "zone" to zone.name,
                        "capacity" to safeCap,
                        "available" to available,
                        "displayOrder" to safeOrder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleDiningTableAvailability(table: DiningTable, available: Boolean): Result<Unit> {
        val path = "diningTables/${table.id}"
        return try {
            requireUserId()
            db.collection("diningTables").document(table.id).update(
                mapOf(
                    "available" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDiningTable(tableId: String): Result<Unit> {
        val path = "diningTables/$tableId"
        return try {
            requireUserId()
            db.collection("diningTables").document(tableId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterTables(): Result<Unit> {
        val path = "diningTables"
        return try {
            requireUserId()
            val batch = db.batch()
            OakAndSmokeSeeds.starterTables.forEach { tbl ->
                batch.set(
                    db.collection("diningTables").document(tbl.id),
                    mapOf(
                        "catalogType" to "TABLE",
                        "label" to tbl.label,
                        "zone" to tbl.zone,
                        "capacity" to tbl.capacity,
                        "available" to tbl.available,
                        "displayOrder" to tbl.displayOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

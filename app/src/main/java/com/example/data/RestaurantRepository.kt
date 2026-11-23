package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
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

object FirestoreProvider {
    @Volatile
    private var configured = false

    fun getInstance(context: Context): FirebaseFirestore {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        if (!configured) {
            synchronized(this) {
                if (!configured) {
                    try {
                        db.firestoreSettings = firestoreSettings {
                            setLocalCacheSettings(
                                PersistentCacheSettings.newBuilder()
                                    .setSizeBytes(50L * 1024L * 1024L)
                                    .build()
                            )
                        }
                    } catch (_: IllegalStateException) {
                        // Settings already applied for this Firestore instance
                    }
                    configured = true
                }
            }
        }
        return db
    }
}

class RestaurantRepository(private val db: FirebaseFirestore) {
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

    fun observeConfig(uid: String): Flow<RestaurantConfig?> = flow {
        val path = "restaurantConfig/oak_and_smoke"
        emitAll(
            db.collection("restaurantConfig").document("oak_and_smoke")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(RestaurantConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    fun observeRoleGrant(uid: String): Flow<RoleGrant?> = flow {
        val path = "roleGrants/$uid"
        emitAll(
            db.collection("roleGrants").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(RoleGrant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    fun observeMenuItems(uid: String): Flow<List<MenuItem>> = flow {
        val path = "menuItems"
        emitAll(
            db.collection("menuItems")
                .whereEqualTo("catalogType", "MENU")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<MenuItem> { it.dailySpecial }.thenBy { it.name })
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeDiningTables(uid: String): Flow<List<DiningTable>> = flow {
        val path = "diningTables"
        emitAll(
            db.collection("diningTables")
                .whereEqualTo("catalogType", "TABLE")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DiningTable::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<DiningTable> { it.displayOrder }.thenBy { it.label })
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeCustomerOrders(uid: String): Flow<List<OrderRecord>> = flow {
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(OrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeAllOrders(uid: String): Flow<List<OrderRecord>> = flow {
        val path = "orders"
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(OrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeCustomerReservations(uid: String): Flow<List<ReservationRecord>> = flow {
        val path = "reservations"
        emitAll(
            db.collection("reservations")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReservationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeAllReservations(uid: String): Flow<List<ReservationRecord>> = flow {
        val path = "reservations"
        emitAll(
            db.collection("reservations")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReservationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observePersonalNotifications(uid: String): Flow<List<NotificationItem>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("targetUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeBroadcastNotifications(uid: String): Flow<List<NotificationItem>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("audience", "BROADCAST")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeStaffNotifications(uid: String): Flow<List<NotificationItem>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("audience", "STAFF")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeRiderNotifications(uid: String): Flow<List<NotificationItem>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("audience", "RIDER")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeCustomerFeedback(uid: String): Flow<List<FeedbackEntry>> = flow {
        val path = "feedback"
        emitAll(
            db.collection("feedback")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FeedbackEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeAllFeedback(uid: String): Flow<List<FeedbackEntry>> = flow {
        val path = "feedback"
        emitAll(
            db.collection("feedback")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FeedbackEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    suspend fun ensureUserProfileExists(displayName: String, email: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Oak & Smoke Guest" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(160),
                    "phone" to "",
                    "deliveryAddress" to "",
                    "roleLabel" to "CUSTOMER",
                    "loyaltyPoints" to 0L,
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnProfile(
        displayName: String,
        phone: String,
        deliveryAddress: String,
        notificationsEnabled: Boolean
    ): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim()
            require(cleanName.length in 1..80) { "Please enter your name (1-80 characters)." }
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "phone" to phone.trim().take(30),
                    "deliveryAddress" to deliveryAddress.trim().take(240),
                    "notificationsEnabled" to notificationsEnabled,
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

    suspend fun placeCodOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        customerLat: Double,
        customerLng: Double,
        locationLabel: String,
        hasLocation: Boolean,
        cartLines: List<CartLine>,
        redeemPoints: Long,
        currentLoyaltyPoints: Long,
        pointValuePkr: Long,
        notes: String
    ): Result<String> {
        var path = "orders"
        return try {
            val uid = requireUserId()
            require(cartLines.isNotEmpty()) { "Your cart is empty." }
            require(customerPhone.trim().length >= 7) { "Enter a valid phone number (at least 7 digits)." }
            require(deliveryAddress.trim().length >= 4) { "Enter your Hasilpur delivery address." }
            require(hasLocation && customerLat in -90.0..90.0 && customerLng in -180.0..180.0) {
                "Attach your current GPS location so our Rider can navigate directly to you."
            }

            val subtotal = cartLines.sumOf { it.item.pricePkr * it.quantity }
            val safeRedeem = redeemPoints.coerceIn(0L, currentLoyaltyPoints)
            val discount = (safeRedeem * pointValuePkr).coerceAtMost(subtotal)
            val total = (subtotal - discount).coerceAtLeast(0L)
            val itemCount = cartLines.sumOf { it.quantity }.toLong().coerceIn(1L, 100L)
            val summary = cartLines.joinToString(", ") { "${it.quantity}x ${it.item.name}" }.take(1000)
            val orderNum = "OS-${Random.nextInt(1000, 9999)}"

            val orderRef = db.collection("orders").document()
            path = orderRef.path
            val batch = db.batch()
            batch.set(
                orderRef,
                mapOf(
                    "orderNumber" to orderNum,
                    "userId" to uid,
                    "orderType" to OrderType.COD_DELIVERY.name,
                    "tableLabel" to "",
                    "customerName" to customerName.trim().ifBlank { "Guest" }.take(80),
                    "customerPhone" to customerPhone.trim().take(30),
                    "deliveryAddress" to deliveryAddress.trim().take(240),
                    "customerLat" to customerLat,
                    "customerLng" to customerLng,
                    "locationLabel" to locationLabel.trim().take(200),
                    "hasLocation" to true,
                    "assignedRiderId" to "",
                    "assignedRiderName" to "",
                    "assignedRiderPhone" to "",
                    "itemsSummary" to summary,
                    "itemsDetail" to encodeOrderLines(cartLines),
                    "itemCount" to itemCount,
                    "subtotalPkr" to subtotal,
                    "loyaltyPointsRedeemed" to safeRedeem,
                    "discountPkr" to discount,
                    "totalPkr" to total,
                    "paymentMethod" to "COD",
                    "status" to OrderStatus.PLACED.name,
                    "handledByStaffId" to "",
                    "notes" to notes.trim().take(300),
                    "dateKey" to todayDateKey(),
                    "monthKey" to currentMonthKey(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (safeRedeem > 0L) {
                batch.update(
                    db.collection("users").document(uid),
                    mapOf(
                        "loyaltyPoints" to (currentLoyaltyPoints - safeRedeem).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Notify Staff of new COD order
            val staffAlertRef = db.collection("notifications").document()
            batch.set(
                staffAlertRef,
                mapOf(
                    "targetUid" to "ROLE_STAFF",
                    "audience" to "STAFF",
                    "title" to "New COD Order $orderNum",
                    "message" to "${customerName.trim().ifBlank { "Guest" }} ordered $itemCount items (${formatPkr(total)} COD).",
                    "type" to "ORDER",
                    "read" to false,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(orderNum)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelOwnOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
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

    suspend fun createReservation(
        customerName: String,
        customerPhone: String,
        dateLabel: String,
        timeSlot: String,
        partySize: Long,
        seatingZone: SeatingZone,
        specialNotes: String
    ): Result<Unit> {
        var path = "reservations"
        return try {
            val uid = requireUserId()
            require(customerPhone.trim().length >= 7) { "Enter a valid phone number." }
            require(dateLabel.trim().length >= 4) { "Select a reservation date." }
            val ref = db.collection("reservations").document()
            path = ref.path
            val batch = db.batch()
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "customerName" to customerName.trim().ifBlank { "Guest" }.take(80),
                    "customerPhone" to customerPhone.trim().take(30),
                    "dateLabel" to dateLabel.trim().take(32),
                    "timeSlot" to timeSlot.trim().take(32),
                    "partySize" to partySize.coerceIn(1L, 30L),
                    "seatingZone" to seatingZone.name,
                    "specialNotes" to specialNotes.trim().take(300),
                    "status" to ReservationStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val alertRef = db.collection("notifications").document()
            batch.set(
                alertRef,
                mapOf(
                    "targetUid" to "ROLE_STAFF",
                    "audience" to "STAFF",
                    "title" to "New Table Booking ($partySize Guests)",
                    "message" to "${customerName.trim().ifBlank { "Guest" }} requested ${seatingZone.displayLabel} on $dateLabel at $timeSlot.",
                    "type" to "RESERVATION",
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
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelOwnReservation(reservationId: String): Result<Unit> {
        val path = "reservations/$reservationId"
        return try {
            requireUserId()
            db.collection("reservations").document(reservationId).update(
                mapOf(
                    "status" to ReservationStatus.CANCELLED.name,
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

    suspend fun submitFeedback(
        customerName: String,
        rating: Long,
        category: FeedbackCategory,
        comment: String
    ): Result<Unit> {
        var path = "feedback"
        return try {
            val uid = requireUserId()
            val cleanComment = comment.trim()
            require(cleanComment.length >= 2) { "Please write a short comment (at least 2 characters)." }
            val ref = db.collection("feedback").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "customerName" to customerName.trim().ifBlank { "Guest" }.take(80),
                    "rating" to rating.coerceIn(1L, 5L),
                    "category" to category.name,
                    "comment" to cleanComment.take(500),
                    "staffReply" to "",
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

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId).update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun unlockStaffPortal(staffId: String, passcode: String): Result<UserRoleTier> {
        var path = "staffCredentials"
        return try {
            val uid = requireUserId()
            val cleanId = staffId.trim().uppercase(Locale.US)
            require(cleanId.length >= 2 && passcode.trim().length >= 4) {
                "Enter your Staff ID and at least 4-character Passcode."
            }
            val credHash = computeStaffCredHash(cleanId, passcode)
            path = "staffCredentials/$credHash"
            val credSnap = db.collection("staffCredentials").document(credHash).get().await()
            if (!credSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invalid Staff ID or Passcode."))
            }
            val active = credSnap.getBoolean("active") ?: false
            if (!active) {
                return Result.failure(IllegalStateException("This Staff ID is currently deactivated by the Owner."))
            }
            val roleStr = credSnap.getString("roleTier") ?: "STAFF"
            val staffDocId = credSnap.getString("staffDocId") ?: ""
            val verifiedStaffId = credSnap.getString("staffId") ?: cleanId

            path = "roleGrants/$uid"
            db.collection("roleGrants").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "role" to roleStr,
                    "staffDocId" to staffDocId,
                    "staffId" to verifiedStaffId,
                    "credHash" to credHash,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Link this UID to staffMembers so staff member can view their own shifts/salary/stats
            if (staffDocId.isNotBlank()) {
                try {
                    db.collection("staffMembers").document(staffDocId).update(
                        mapOf(
                            "linkedUid" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {
                    // Non-blocking if already linked or owner-managed
                }
            }
            Result.success(UserRoleTier.fromRaw(roleStr))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun lockStaffPortal(): Result<Unit> {
        val path = "roleGrants/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            db.collection("roleGrants").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Deletes the signed-in user's personal profile document, optional role grant,
     * and attempts to remove their Firebase Auth account (Google Play Account Deletion Policy).
     */
    suspend fun deleteOwnAccountData(): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            try {
                db.collection("roleGrants").document(uid).delete().await()
            } catch (_: Exception) {
                // Role grant may not exist for regular customers
            }
            db.collection("users").document(uid).delete().await()
            try {
                auth.currentUser?.delete()?.await()
            } catch (_: Exception) {
                // If Firebase requires recent sign-in for auth deletion, profile data is already purged
                // and caller signs out immediately.
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}

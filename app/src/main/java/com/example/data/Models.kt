package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UserRoleTier(val displayLabel: String) {
    CUSTOMER("Customer"),
    RIDER("Rider"),
    STAFF("Staff"),
    MANAGER("Manager"),
    OWNER("Owner");

    companion object {
        fun fromRaw(raw: String?): UserRoleTier =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class MenuCategory(val displayLabel: String) {
    KARAHI("Karahi"),
    BBQ("Smoked BBQ"),
    PLATTERS("Platters"),
    NAAN_SIDES("Naan & Sides"),
    BEVERAGES("Beverages");

    companion object {
        fun fromRaw(raw: String?): MenuCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: KARAHI
    }
}

enum class SpiceLevel(val displayLabel: String) {
    MILD("Mild"),
    MEDIUM("Medium"),
    SPICY("Spicy");

    companion object {
        fun fromRaw(raw: String?): SpiceLevel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class OrderType(val displayLabel: String) {
    COD_DELIVERY("COD Delivery"),
    DINE_IN("Dine-In"),
    TAKEAWAY("Takeaway");

    companion object {
        fun fromRaw(raw: String?): OrderType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: COD_DELIVERY
    }
}

enum class OrderStatus(val displayLabel: String, val stepIndex: Int) {
    PLACED("Order Placed", 0),
    CONFIRMED("Confirmed", 1),
    GRILLING("In Kitchen", 2),
    READY("Ready to Serve", 3),
    OUT_FOR_DELIVERY("Out for Delivery", 4),
    DELIVERED("Completed & Paid", 5),
    CANCELLED("Cancelled", -1);

    fun nextStaffStatus(orderType: OrderType = OrderType.COD_DELIVERY): OrderStatus? = when (this) {
        PLACED -> CONFIRMED
        CONFIRMED -> GRILLING
        GRILLING -> READY
        READY -> if (orderType == OrderType.COD_DELIVERY) OUT_FOR_DELIVERY else DELIVERED
        OUT_FOR_DELIVERY -> DELIVERED
        DELIVERED, CANCELLED -> null
    }

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PLACED
    }
}

enum class ReservationStatus(val displayLabel: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    SEATED("Seated"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): ReservationStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

enum class SeatingZone(val displayLabel: String) {
    CHARCOAL_HALL("Charcoal Main Hall"),
    FAMILY_CABIN("Family Cabin"),
    OUTDOOR_LAWN("Outdoor Grill Lawn");

    companion object {
        fun fromRaw(raw: String?): SeatingZone =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CHARCOAL_HALL
    }
}

enum class InventoryCategory(val displayLabel: String) {
    MEAT("Meat & Poultry"),
    FUEL_CHARCOAL("Oak & Charcoal"),
    SPICES_OIL("Spices & Oil"),
    BREAD_DAIRY("Flour & Dairy"),
    PACKAGING("Packaging"),
    BEVERAGES("Beverages");

    companion object {
        fun fromRaw(raw: String?): InventoryCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEAT
    }
}

enum class ExpenseCategory(val displayLabel: String) {
    INGREDIENTS("Ingredients"),
    CHARCOAL_FUEL("Oak & Charcoal"),
    UTILITIES("Utilities"),
    SALARIES("Staff Salaries"),
    MAINTENANCE("Maintenance"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): ExpenseCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: INGREDIENTS
    }
}

enum class FeedbackCategory(val displayLabel: String) {
    FOOD("Food & Taste"),
    SERVICE("Service"),
    DELIVERY("COD Delivery"),
    AMBIANCE("Ambiance");

    companion object {
        fun fromRaw(raw: String?): FeedbackCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FOOD
    }
}

@Keep
@IgnoreExtraProperties
data class RestaurantConfig(
    @DocumentId val id: String = "",
    val restaurantName: String = "Oak & Smoke Restaurant Hasilpur",
    val currency: String = "PKR",
    val ownerUid: String = "",
    val ownerInitialized: Boolean = false,
    val autoPrintDailyReport: Boolean = false,
    val loyaltyEarnPer100Pkr: Long = 1L,
    val loyaltyPointValuePkr: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DiningTable(
    @DocumentId val id: String = "",
    val catalogType: String = "TABLE",
    val label: String = "",
    val zone: String = "CHARCOAL_HALL",
    val capacity: Long = 4L,
    val available: Boolean = true,
    val displayOrder: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoleGrant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "STAFF",
    val staffDocId: String = "",
    val staffId: String = "",
    val credHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val deliveryAddress: String = "",
    val roleLabel: String = "CUSTOMER",
    val loyaltyPoints: Long = 0L,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val catalogType: String = "MENU",
    val name: String = "",
    val category: String = "KARAHI",
    val description: String = "",
    val pricePkr: Long = 0L,
    val varietiesDetail: String = "",
    val imageKey: String = "karahi",
    val dailySpecial: Boolean = false,
    val available: Boolean = true,
    val spiceLevel: String = "MEDIUM",
    val prepMinutes: Long = 20L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderRecord(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val orderType: String = "COD_DELIVERY",
    val tableLabel: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val itemsSummary: String = "",
    val itemsDetail: String = "",
    val itemCount: Long = 1L,
    val subtotalPkr: Long = 0L,
    val loyaltyPointsRedeemed: Long = 0L,
    val discountPkr: Long = 0L,
    val totalPkr: Long = 0L,
    val paymentMethod: String = "COD",
    val status: String = "PLACED",
    val handledByStaffId: String = "",
    val notes: String = "",
    val dateKey: String = "",
    val monthKey: String = "",
    val customerLat: Double = 0.0,
    val customerLng: Double = 0.0,
    val locationLabel: String = "",
    val hasLocation: Boolean = false,
    val assignedRiderId: String = "",
    val assignedRiderName: String = "",
    val assignedRiderPhone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReservationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val dateLabel: String = "",
    val timeSlot: String = "",
    val partySize: Long = 2L,
    val seatingZone: String = "CHARCOAL_HALL",
    val specialNotes: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "MEAT",
    val quantity: Long = 0L,
    val unit: String = "kg",
    val minThreshold: Long = 5L,
    val unitCostPkr: Long = 0L,
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isLowStock(): Boolean = quantity <= minThreshold
}

@Keep
@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val staffId: String = "",
    val fullName: String = "",
    val roleTier: String = "STAFF",
    val credHash: String = "",
    val linkedUid: String = "",
    val monthlySalaryPkr: Long = 0L,
    val salaryStatus: String = "PENDING",
    val loyaltyPoints: Long = 0L,
    val shiftSummary: String = "",
    val scheduleDays: String = "",
    val ordersHandledCount: Long = 0L,
    val phone: String = "",
    val vehicleInfo: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExpenseEntry(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "INGREDIENTS",
    val amountPkr: Long = 0L,
    val dateKey: String = "",
    val monthKey: String = "",
    val notes: String = "",
    val recordedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeedbackEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val rating: Long = 5L,
    val category: String = "FOOD",
    val comment: String = "",
    val staffReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val targetUid: String = "",
    val audience: String = "USER",
    val title: String = "",
    val message: String = "",
    val type: String = "ORDER",
    val read: Boolean = false,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DishVariety(
    val label: String,
    val pricePkr: Long
)

fun encodeDishVarieties(varieties: List<DishVariety>): String =
    varieties
        .filter { it.label.isNotBlank() && it.pricePkr in 50L..250000L }
        .take(4)
        .joinToString("\n") { v ->
            val cleanLabel = v.label.replace("||", " ").replace("\n", " ").trim().take(40)
            "$cleanLabel||${v.pricePkr}"
        }
        .take(600)

fun MenuItem.parsedVarieties(): List<DishVariety> {
    if (varietiesDetail.isNotBlank()) {
        val parsed = varietiesDetail.lines().mapNotNull { row ->
            val parts = row.split("||")
            if (parts.size >= 2) {
                val lbl = parts[0].trim()
                val rate = parts[1].trim().toLongOrNull()
                if (lbl.isNotBlank() && rate != null && rate in 50L..250000L) {
                    DishVariety(lbl, rate)
                } else null
            } else null
        }.take(4)
        if (parsed.isNotEmpty()) return parsed
    }
    val base = pricePkr.coerceIn(50L, 250000L)
    return listOf(DishVariety("Standard", base))
}

fun defaultFourVarietiesForPrice(basePricePkr: Long): List<DishVariety> {
    val safeBase = basePricePkr.coerceIn(50L, 250000L)
    val v2 = ((safeBase * 14L / 10L) / 10L * 10L).coerceIn(50L, 250000L)
    val v3 = ((safeBase * 19L / 10L) / 10L * 10L).coerceIn(50L, 250000L)
    val v4 = ((safeBase * 26L / 10L) / 10L * 10L).coerceIn(50L, 250000L)
    return listOf(
        DishVariety("Regular", safeBase),
        DishVariety("Half", v2),
        DishVariety("Full", v3),
        DishVariety("Family", v4)
    )
}

fun cartItemKey(dishId: String, varietyLabel: String = ""): String =
    if (varietyLabel.isBlank()) dishId else "$dishId::${varietyLabel.trim()}"

data class CartLine(
    val item: MenuItem,
    val quantity: Int,
    val varietyLabel: String = "",
    val unitPricePkr: Long = item.pricePkr
) {
    val lineTotalPkr: Long get() = unitPricePkr * quantity
    fun displayNameWithVariety(): String =
        if (varietyLabel.isBlank()) item.name else "${item.name} ($varietyLabel)"
}

fun resolveCartLines(items: List<MenuItem>, quantities: Map<String, Int>): List<CartLine> {
    if (quantities.isEmpty()) return emptyList()
    val itemMap = items.associateBy { it.id }
    val result = mutableListOf<CartLine>()
    quantities.forEach { (key, qty) ->
        if (qty <= 0) return@forEach
        val sepIdx = key.indexOf("::")
        val dishId = if (sepIdx >= 0) key.substring(0, sepIdx) else key
        val requestedLabel = if (sepIdx >= 0) key.substring(sepIdx + 2) else ""
        val dish = itemMap[dishId] ?: return@forEach
        if (!dish.available) return@forEach
        val varieties = dish.parsedVarieties()
        val matched = if (requestedLabel.isNotBlank()) {
            varieties.firstOrNull { it.label.equals(requestedLabel, ignoreCase = true) }
        } else {
            varieties.firstOrNull()
        }
        val finalLabel = when {
            matched != null && varieties.size > 1 -> matched.label
            requestedLabel.isNotBlank() -> requestedLabel
            else -> ""
        }
        val finalPrice = matched?.pricePkr ?: dish.pricePkr
        result.add(
            CartLine(
                item = dish,
                quantity = qty,
                varietyLabel = finalLabel,
                unitPricePkr = finalPrice
            )
        )
    }
    return result
}

data class OrderLineItem(
    val qty: Int,
    val name: String,
    val unitPricePkr: Long,
    val lineTotalPkr: Long
)

fun encodeOrderLines(lines: List<CartLine>): String =
    lines.joinToString("\n") { line ->
        val cleanName = line.displayNameWithVariety().replace("||", " ").replace("\n", " ").trim()
        val lineTotal = line.unitPricePkr * line.quantity
        "${line.quantity}||$cleanName||${line.unitPricePkr}||$lineTotal"
    }.take(2000)

fun OrderRecord.parsedLineItems(): List<OrderLineItem> {
    if (itemsDetail.isNotBlank()) {
        val parsed = itemsDetail.lines().mapNotNull { row ->
            val parts = row.split("||")
            if (parts.size >= 2) {
                val qty = parts[0].trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
                val name = parts[1].trim()
                val unitPrice = parts.getOrNull(2)?.trim()?.toLongOrNull() ?: 0L
                val lineTotal = parts.getOrNull(3)?.trim()?.toLongOrNull() ?: (unitPrice * qty)
                if (name.isNotBlank()) OrderLineItem(qty, name, unitPrice, lineTotal) else null
            } else null
        }
        if (parsed.isNotEmpty()) return parsed
    }
    if (itemsSummary.isBlank()) return emptyList()
    return itemsSummary.split(", ").mapNotNull { segment ->
        val trimmed = segment.trim()
        if (trimmed.isBlank()) return@mapNotNull null
        val xIdx = trimmed.indexOf('x')
        val qty = if (xIdx > 0) trimmed.substring(0, xIdx).trim().toIntOrNull() else null
        if (qty != null && qty > 0) {
            OrderLineItem(qty, trimmed.substring(xIdx + 1).trim(), 0L, 0L)
        } else {
            OrderLineItem(1, trimmed, 0L, 0L)
        }
    }
}

fun formatPkr(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "Rs. ${formatter.format(amount)}"
}

fun todayDateKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun currentMonthKey(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

fun formatTimestampShort(ts: Timestamp?): String {
    val date = ts?.toDate() ?: return "Just now"
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(date)
}

fun computeStaffCredHash(staffId: String, passcode: String): String {
    val normalized = "${staffId.trim().uppercase(Locale.US)}:${passcode.trim()}"
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

fun formatCoordinates(lat: Double, lng: Double): String =
    String.format(Locale.US, "%.5f, %.5f", lat, lng)

object OakAndSmokeSeeds {
    val starterMenu: List<MenuItem> = listOf(
        MenuItem(
            id = "mutton_shahi_karahi",
            name = "Oak-Smoked Mutton Karahi",
            category = MenuCategory.KARAHI.name,
            description = "Fresh Hasilpur mutton simmered in vine tomatoes, desi ghee, crushed black pepper, and julienned ginger.",
            pricePkr = 1950L,
            varietiesDetail = "Half (500g)||1950\nFull (1 Kg)||3600\nDesi Ghee Full||4100\nFamily Handi (1.5 Kg)||5200",
            imageKey = "karahi",
            dailySpecial = true,
            available = true,
            spiceLevel = SpiceLevel.MEDIUM.name,
            prepMinutes = 35L
        ),
        MenuItem(
            id = "chicken_makhni_karahi",
            name = "Charcoal Chicken Makhni Karahi",
            category = MenuCategory.KARAHI.name,
            description = "Tender boneless chicken finished with smoky butter, green chilies, and hand-ground garam masala.",
            pricePkr = 1250L,
            varietiesDetail = "Half (500g)||1250\nFull (1 Kg)||2250\nBoneless Handi||2550\nFamily Pot (1.5 Kg)||3300",
            imageKey = "karahi",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.MEDIUM.name,
            prepMinutes = 25L
        ),
        MenuItem(
            id = "hasilpur_special_platter",
            name = "Oak & Smoke Royal BBQ Platter",
            category = MenuCategory.PLATTERS.name,
            description = "Chicken Tikka, Beef Seekh Kebabs, Malai Boti, Reshmi Kabab, Garlic Naan, Mint Raita & Salad.",
            pricePkr = 2450L,
            varietiesDetail = "Duo (2 Persons)||2450\nStandard (4 Persons)||4450\nLarge (6 Persons)||6450\nRoyal Feast (8 Persons)||8400",
            imageKey = "platter",
            dailySpecial = true,
            available = true,
            spiceLevel = SpiceLevel.MEDIUM.name,
            prepMinutes = 30L
        ),
        MenuItem(
            id = "family_karahi_bbq_feast",
            name = "Family Karahi & Grill Feast",
            category = MenuCategory.PLATTERS.name,
            description = "Mutton Karahi, Chicken Karahi, Seekh Kebabs, Malai Boti, Roghni Naan & Chilled Drinks.",
            pricePkr = 4600L,
            varietiesDetail = "4 Persons||4600\n6 Persons||7800\n8 Persons||9900\nGrand 12 Persons||14200",
            imageKey = "platter",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.SPICY.name,
            prepMinutes = 40L
        ),
        MenuItem(
            id = "beef_seekh_kebabs",
            name = "Smoked Beef Seekh Kebabs",
            category = MenuCategory.BBQ.name,
            description = "Hand-minced spiced beef skewers seared over glowing oak charcoal with roasted chili chutney.",
            pricePkr = 520L,
            varietiesDetail = "2 Skewers||520\n4 Skewers||980\n6 Skewers||1420\nDozen Box (12)||2750",
            imageKey = "kebabs",
            dailySpecial = true,
            available = true,
            spiceLevel = SpiceLevel.SPICY.name,
            prepMinutes = 18L
        ),
        MenuItem(
            id = "chicken_malai_boti",
            name = "Creamy Chicken Malai Boti",
            category = MenuCategory.BBQ.name,
            description = "Velvety yogurt-and-cream marinated chicken cubes flame-kissed for a delicate smoky char.",
            pricePkr = 650L,
            varietiesDetail = "Half Plate (6 Pcs)||650\nFull Plate (12 Pcs)||1150\nCheese Malai||1380\nFamily Tray (24 Pcs)||2150",
            imageKey = "kebabs",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.MILD.name,
            prepMinutes = 20L
        ),
        MenuItem(
            id = "charcoal_chicken_tikka",
            name = "Hasilpur Charcoal Chicken Tikka",
            category = MenuCategory.BBQ.name,
            description = "Quarter leg or chest piece marinated 12 hours in traditional red spices and basted with lemon butter.",
            pricePkr = 620L,
            varietiesDetail = "Leg Piece||620\nChest Piece||680\nHalf Chicken (2 Pcs)||1240\nFull Sajji Tikka||2380",
            imageKey = "kebabs",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.SPICY.name,
            prepMinutes = 20L
        ),
        MenuItem(
            id = "roghni_garlic_naan",
            name = "Tandoori Garlic Butter Naan",
            category = MenuCategory.NAAN_SIDES.name,
            description = "Crisp clay-oven flatbread brushed with roasted garlic, fresh coriander, and desi butter.",
            pricePkr = 100L,
            varietiesDetail = "Plain Roghni||100\nGarlic Butter||140\nCheese Naan||280\nFamily Basket (6)||750",
            imageKey = "platter",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.MILD.name,
            prepMinutes = 8L
        ),
        MenuItem(
            id = "mint_margarita",
            name = "Fresh Mint Margarita",
            category = MenuCategory.BEVERAGES.name,
            description = "Chilled crushed mint, fresh lime, black salt, and sparkling soda.",
            pricePkr = 260L,
            varietiesDetail = "Regular Glass||260\nLarge Glass||360\nStrawberry Mint||420\nTable Pitcher (1.5L)||980",
            imageKey = "hero",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.MILD.name,
            prepMinutes = 5L
        ),
        MenuItem(
            id = "kashmiri_chai",
            name = "Smoked Pistachio Kashmiri Chai",
            category = MenuCategory.BEVERAGES.name,
            description = "Slow-brewed pink tea topped with crushed pistachios and cardamom.",
            pricePkr = 240L,
            varietiesDetail = "Regular Cup||240\nSpecial Dry Fruit||320\nKulhad Mug||380\nKettle (4 Cups)||950",
            imageKey = "hero",
            dailySpecial = false,
            available = true,
            spiceLevel = SpiceLevel.MILD.name,
            prepMinutes = 10L
        )
    )

    val starterInventory: List<InventoryItem> = listOf(
        InventoryItem(id = "inv_mutton", name = "Fresh Local Mutton", category = InventoryCategory.MEAT.name, quantity = 28L, unit = "kg", minThreshold = 10L, unitCostPkr = 2200L),
        InventoryItem(id = "inv_chicken", name = "Fresh Chicken (Cut & Boneless)", category = InventoryCategory.MEAT.name, quantity = 45L, unit = "kg", minThreshold = 15L, unitCostPkr = 680L),
        InventoryItem(id = "inv_oak_charcoal", name = "Seasoned Oak & Kikar Charcoal", category = InventoryCategory.FUEL_CHARCOAL.name, quantity = 60L, unit = "kg", minThreshold = 20L, unitCostPkr = 160L),
        InventoryItem(id = "inv_tomatoes", name = "Fresh Karahi Tomatoes & Ginger", category = InventoryCategory.SPICES_OIL.name, quantity = 22L, unit = "kg", minThreshold = 8L, unitCostPkr = 240L),
        InventoryItem(id = "inv_desi_ghee", name = "Cooking Oil & Desi Ghee", category = InventoryCategory.SPICES_OIL.name, quantity = 18L, unit = "L", minThreshold = 6L, unitCostPkr = 580L),
        InventoryItem(id = "inv_naan_flour", name = "Fine Tandoori Flour", category = InventoryCategory.BREAD_DAIRY.name, quantity = 35L, unit = "kg", minThreshold = 12L, unitCostPkr = 150L),
        InventoryItem(id = "inv_takeaway_boxes", name = "Insulated COD Food Boxes", category = InventoryCategory.PACKAGING.name, quantity = 120L, unit = "pcs", minThreshold = 40L, unitCostPkr = 45L)
    )

    // 25 Dining Tables across Charcoal Main Hall (1-15), Family Cabins (16-20), and Outdoor Grill Lawn (21-25)
    val starterTables: List<DiningTable> = buildList {
        for (i in 1..15) {
            val seats = if (i <= 8) 4L else 6L
            add(
                DiningTable(
                    id = "tbl_$i",
                    label = "Table $i",
                    zone = SeatingZone.CHARCOAL_HALL.name,
                    capacity = seats,
                    available = true,
                    displayOrder = i.toLong()
                )
            )
        }
        val cabinCapacities = listOf(6L, 8L, 8L, 10L, 12L)
        cabinCapacities.forEachIndexed { idx, seats ->
            val tableNo = 16 + idx
            val cabinNo = idx + 1
            add(
                DiningTable(
                    id = "tbl_$tableNo",
                    label = "Table $tableNo · Family Cabin $cabinNo",
                    zone = SeatingZone.FAMILY_CABIN.name,
                    capacity = seats,
                    available = true,
                    displayOrder = tableNo.toLong()
                )
            )
        }
        val lawnCapacities = listOf(4L, 6L, 6L, 8L, 8L)
        lawnCapacities.forEachIndexed { idx, seats ->
            val tableNo = 21 + idx
            val lawnNo = idx + 1
            add(
                DiningTable(
                    id = "tbl_$tableNo",
                    label = "Table $tableNo · Outdoor Lawn $lawnNo",
                    zone = SeatingZone.OUTDOOR_LAWN.name,
                    capacity = seats,
                    available = true,
                    displayOrder = tableNo.toLong()
                )
            )
        }
    }
}

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_uid_1";
const STAFF_UID = "staff_uid_2";
const CUSTOMER_UID = "customer_uid_3";
const OTHER_UID = "other_uid_4";
const RIDER_UID = "rider_uid_5";
const BOOTSTRAP_EMAIL = "adilmehboobwalana117@gmail.com";
const OWNER_CRED_HASH = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90";
const STAFF_CRED_HASH = "f0e1d2c3b4a5968778695a4b3c2d1e0ff0e1d2c3b4a5968778695a4b3c2d1e0f";
const RIDER_CRED_HASH = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function ts(offsetMs = -2000) {
  return new Date(Date.now() + offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedOwnerAndStaff() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = ts(-10000);
    await db.collection("restaurantConfig").doc("oak_and_smoke").set({
      restaurantName: "Oak & Smoke Restaurant Hasilpur",
      currency: "PKR",
      ownerUid: OWNER_UID,
      ownerInitialized: true,
      autoPrintDailyReport: false,
      loyaltyEarnPer100Pkr: 1,
      loyaltyPointValuePkr: 5,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staffCredentials").doc(OWNER_CRED_HASH).set({
      staffDocId: "owner_master",
      staffId: "OS-001",
      roleTier: "OWNER",
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staffCredentials").doc(STAFF_CRED_HASH).set({
      staffDocId: "staff_grill_1",
      staffId: "OS-102",
      roleTier: "STAFF",
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("roleGrants").doc(OWNER_UID).set({
      userId: OWNER_UID,
      role: "OWNER",
      staffDocId: "owner_master",
      staffId: "OS-001",
      credHash: OWNER_CRED_HASH,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("roleGrants").doc(STAFF_UID).set({
      userId: STAFF_UID,
      role: "STAFF",
      staffDocId: "staff_grill_1",
      staffId: "OS-102",
      credHash: STAFF_CRED_HASH,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staffMembers").doc("staff_grill_1").set({
      staffId: "OS-102",
      fullName: "Bilal Grill Master",
      roleTier: "STAFF",
      credHash: STAFF_CRED_HASH,
      linkedUid: STAFF_UID,
      monthlySalaryPkr: 45000,
      salaryStatus: "PENDING",
      loyaltyPoints: 120,
      shiftSummary: "Evening 4 PM - 12 AM",
      scheduleDays: "Mon, Tue, Wed, Thu, Fri, Sat",
      ordersHandledCount: 5,
      active: true,
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("menuItems").where("catalogType", "==", "MENU").get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("expenses").get());
});

test("First-time atomic Owner initialization succeeds once and cannot be hijacked later", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = ts(-3000);
  const batch = ownerDb.batch();

  batch.set(ownerDb.collection("restaurantConfig").doc("oak_and_smoke"), {
    restaurantName: "Oak & Smoke Restaurant Hasilpur",
    currency: "PKR",
    ownerUid: OWNER_UID,
    ownerInitialized: true,
    autoPrintDailyReport: false,
    loyaltyEarnPer100Pkr: 1,
    loyaltyPointValuePkr: 5,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(ownerDb.collection("roleGrants").doc(OWNER_UID), {
    userId: OWNER_UID,
    role: "OWNER",
    staffDocId: "owner_master",
    staffId: "OS-001",
    credHash: OWNER_CRED_HASH,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(ownerDb.collection("staffCredentials").doc(OWNER_CRED_HASH), {
    staffDocId: "owner_master",
    staffId: "OS-001",
    roleTier: "OWNER",
    active: true,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(ownerDb.collection("staffMembers").doc("owner_master"), {
    staffId: "OS-001",
    fullName: "Oak & Smoke Owner",
    roleTier: "OWNER",
    credHash: OWNER_CRED_HASH,
    linkedUid: OWNER_UID,
    monthlySalaryPkr: 0,
    salaryStatus: "PAID",
    loyaltyPoints: 0,
    shiftSummary: "Full Operations",
    scheduleDays: "Mon - Sun",
    ordersHandledCount: 0,
    active: true,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(ownerDb.collection("menuItems").doc("mutton_karahi"), {
    catalogType: "MENU",
    name: "Special Mutton Karahi",
    category: "KARAHI",
    description: "Slow-cooked Hasilpur mutton karahi over oak embers.",
    pricePkr: 3200,
    imageKey: "karahi",
    dailySpecial: true,
    available: true,
    spiceLevel: "MEDIUM",
    prepMinutes: 30,
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(batch.commit());

  // Second user cannot self-grant OWNER after initialization
  const attackerDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(
    attackerDb.collection("roleGrants").doc(OTHER_UID).set({
      userId: OTHER_UID,
      role: "OWNER",
      staffDocId: "fake_doc",
      staffId: "OS-999",
      credHash: "deadbeef1234567890",
      createdAt: ts(-1000),
      updatedAt: ts(-1000),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  await seedOwnerAndStaff();
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: BOOTSTRAP_EMAIL, email_verified: false })
    .firestore();
  await assertFails(spoofDb.collection("expenses").get());
  await assertFails(spoofDb.collection("users").get());

  const verifiedAdminDb = testEnv
    .authenticatedContext("real_admin_uid", { email: BOOTSTRAP_EMAIL, email_verified: true })
    .firestore();
  await assertSucceeds(verifiedAdminDb.collection("expenses").get());
});

test("Staff ID + Passcode credential proof elevates Staff role securely", async () => {
  await seedOwnerAndStaff();
  const newStaffDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  // Valid credHash lookup and matching roleGrant succeeds
  await assertSucceeds(newStaffDb.collection("staffCredentials").doc(STAFF_CRED_HASH).get());
  await assertSucceeds(
    newStaffDb.collection("roleGrants").doc(OTHER_UID).set({
      userId: OTHER_UID,
      role: "STAFF",
      staffDocId: "staff_grill_1",
      staffId: "OS-102",
      credHash: STAFF_CRED_HASH,
      createdAt: ts(-1000),
      updatedAt: ts(-1000),
    })
  );

  // Attempting to claim OWNER using a STAFF credential hash fails
  const escalateDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertFails(
    escalateDb.collection("roleGrants").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      role: "OWNER",
      staffDocId: "staff_grill_1",
      staffId: "OS-102",
      credHash: STAFF_CRED_HASH,
      createdAt: ts(-1000),
      updatedAt: ts(-1000),
    })
  );
});

test("User profiles enforce PII isolation, no self-minting loyalty points, and Owner edits", async () => {
  await seedOwnerAndStaff();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = ts(-5000);

  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Hamza Khan",
      email: "hamza@example.com",
      phone: "03001234567",
      deliveryAddress: "Model Town, Hasilpur",
      roleLabel: "CUSTOMER",
      loyaltyPoints: 0,
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user PII read rejected
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).get());

  // Customer cannot self-mint loyalty points or add ghost field
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      loyaltyPoints: 500,
      updatedAt: ts(-1000),
    })
  );
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      displayName: "Hamza",
      isVerified: true,
      updatedAt: ts(-1000),
    })
  );

  // Owner can edit customer loyalty points
  await assertSucceeds(
    ownerDb.collection("users").doc(CUSTOMER_UID).update({
      loyaltyPoints: 250,
      updatedAt: ts(-1000),
    })
  );

  // Customer can redeem (decrease) their own loyalty points at checkout
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      loyaltyPoints: 200,
      updatedAt: ts(-500),
    })
  );
});

test("COD Orders enforce customer isolation, Staff status updates, and terminal state lock", async () => {
  await seedOwnerAndStaff();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = ts(-5000);

  // Customer creates valid COD order with mandatory GPS location attached
  await assertSucceeds(
    custDb.collection("orders").doc("order_101").set({
      orderNumber: "OS-1001",
      userId: CUSTOMER_UID,
      customerName: "Hamza Khan",
      customerPhone: "03001234567",
      deliveryAddress: "Main Bazar Hasilpur",
      customerLat: 29.6968,
      customerLng: 72.5535,
      locationLabel: "Main Bazar, Hasilpur",
      hasLocation: true,
      itemsSummary: "1x Mutton Karahi, 2x Garlic Naan",
      itemCount: 3,
      subtotalPkr: 3400,
      loyaltyPointsRedeemed: 0,
      discountPkr: 0,
      totalPkr: 3400,
      paymentMethod: "COD",
      status: "PLACED",
      handledByStaffId: "",
      notes: "Extra ginger",
      dateKey: "2026-11-23",
      monthKey: "2026-11",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer COD order WITHOUT GPS location attached MUST FAIL
  await assertFails(
    custDb.collection("orders").doc("order_no_loc").set({
      orderNumber: "OS-1099",
      userId: CUSTOMER_UID,
      customerName: "Hamza Khan",
      customerPhone: "03001234567",
      deliveryAddress: "Main Bazar Hasilpur",
      hasLocation: false,
      itemsSummary: "1x Mutton Karahi",
      itemCount: 1,
      subtotalPkr: 3600,
      loyaltyPointsRedeemed: 0,
      discountPkr: 0,
      totalPkr: 3600,
      paymentMethod: "COD",
      status: "PLACED",
      handledByStaffId: "",
      notes: "",
      dateKey: "2026-11-23",
      monthKey: "2026-11",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds for owner of order; unfiltered fails for customer
  await assertSucceeds(custDb.collection("orders").where("userId", "==", CUSTOMER_UID).get());
  await assertFails(custDb.collection("orders").get());
  await assertFails(otherDb.collection("orders").doc("order_101").get());

  // Staff updates status through pipeline to DELIVERED
  await assertSucceeds(
    staffDb.collection("orders").doc("order_101").update({
      status: "GRILLING",
      handledByStaffId: "OS-102",
      updatedAt: ts(-2000),
    })
  );
  await assertSucceeds(
    staffDb.collection("orders").doc("order_101").update({
      status: "DELIVERED",
      handledByStaffId: "OS-102",
      updatedAt: ts(-1500),
    })
  );

  // Terminal state lock: Staff cannot mutate DELIVERED order, but Owner can override
  await assertFails(
    staffDb.collection("orders").doc("order_101").update({
      status: "CANCELLED",
      handledByStaffId: "OS-102",
      updatedAt: ts(-1000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("orders").doc("order_101").update({
      notes: "Confirmed COD cash received",
      updatedAt: ts(-500),
    })
  );

  // Staff can take a Dine-In order and send directly to Kitchen (status: GRILLING)
  await assertSucceeds(
    staffDb.collection("orders").doc("order_pos_201").set({
      orderNumber: "OS-2001",
      userId: STAFF_UID,
      customerName: "Walk-in Guest",
      customerPhone: "",
      deliveryAddress: "Dine-In",
      orderType: "DINE_IN",
      tableLabel: "Table 4",
      itemsSummary: "1x Mutton Karahi, 3x Garlic Naan",
      itemsDetail: "1||Mutton Karahi||3600||3600\n3||Garlic Naan||140||420",
      itemCount: 4,
      subtotalPkr: 4020,
      loyaltyPointsRedeemed: 0,
      discountPkr: 0,
      totalPkr: 4020,
      paymentMethod: "CASH_COUNTER",
      status: "GRILLING",
      handledByStaffId: "OS-102",
      notes: "Less spicy",
      dateKey: "2026-11-23",
      monthKey: "2026-11",
      createdAt: ts(-1000),
      updatedAt: ts(-1000),
    })
  );

  // Kitchen / Staff marks order READY
  await assertSucceeds(
    staffDb.collection("orders").doc("order_pos_201").update({
      status: "READY",
      handledByStaffId: "OS-102",
      updatedAt: ts(-500),
    })
  );

  // Customer cannot bypass PLACED status or send direct to GRILLING
  await assertFails(
    custDb.collection("orders").doc("order_hack_202").set({
      orderNumber: "OS-2002",
      userId: CUSTOMER_UID,
      customerName: "Hamza",
      customerPhone: "03001234567",
      deliveryAddress: "Hasilpur",
      orderType: "DINE_IN",
      tableLabel: "Table 1",
      itemsSummary: "1x Tikka",
      itemsDetail: "1||Tikka||620||620",
      itemCount: 1,
      subtotalPkr: 620,
      loyaltyPointsRedeemed: 0,
      discountPkr: 0,
      totalPkr: 620,
      paymentMethod: "CASH_COUNTER",
      status: "GRILLING",
      handledByStaffId: "",
      notes: "",
      dateKey: "2026-11-23",
      monthKey: "2026-11",
      createdAt: ts(-500),
      updatedAt: ts(-500),
    })
  );
});

test("Menu, Inventory, Expenses, Staff Salaries, Feedback & Notifications respect tiered RBAC", async () => {
  await seedOwnerAndStaff();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = ts(-4000);

  // Owner adds menu item; customer reads filtered menu
  await assertSucceeds(
    ownerDb.collection("menuItems").doc("bbq_platter").set({
      catalogType: "MENU",
      name: "Oak & Smoke Royal BBQ Platter",
      category: "PLATTERS",
      description: "Chicken Tikka, Malai Boti, Seekh Kebabs, Garlic Naan & Raita.",
      pricePkr: 4200,
      imageKey: "platter",
      dailySpecial: true,
      available: true,
      spiceLevel: "MEDIUM",
      prepMinutes: 25,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(custDb.collection("menuItems").where("catalogType", "==", "MENU").get());
  await assertFails(custDb.collection("menuItems").doc("bbq_platter").update({ pricePkr: 100, updatedAt: ts(-1000) }));

  // Inventory: Owner creates, Staff adjusts quantity, Customer denied
  await assertSucceeds(
    ownerDb.collection("inventory").doc("oak_charcoal").set({
      name: "Seasoned Oak Charcoal",
      category: "FUEL_CHARCOAL",
      quantity: 40,
      unit: "kg",
      minThreshold: 15,
      unitCostPkr: 180,
      updatedByUid: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    staffDb.collection("inventory").doc("oak_charcoal").update({
      quantity: 35,
      updatedByUid: STAFF_UID,
      updatedAt: ts(-1000),
    })
  );
  await assertFails(custDb.collection("inventory").get());

  // Expenses: Owner allowed, Staff & Customer denied
  await assertSucceeds(
    ownerDb.collection("expenses").doc("exp_1").set({
      title: "Fresh Mutton Supply",
      category: "INGREDIENTS",
      amountPkr: 18500,
      dateKey: "2026-11-23",
      monthKey: "2026-11",
      notes: "Morning butcher delivery",
      recordedByUid: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(staffDb.collection("expenses").get());
  await assertFails(custDb.collection("expenses").get());

  // Staff Member: Staff can read own profile, cannot edit own salary; Owner can edit salary & loyalty
  await assertSucceeds(staffDb.collection("staffMembers").where("linkedUid", "==", STAFF_UID).get());
  await assertFails(
    staffDb.collection("staffMembers").doc("staff_grill_1").update({
      monthlySalaryPkr: 90000,
      updatedAt: ts(-1000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("staffMembers").doc("staff_grill_1").update({
      monthlySalaryPkr: 50000,
      loyaltyPoints: 200,
      salaryStatus: "PAID",
      updatedAt: ts(-1000),
    })
  );

  // Feedback: Customer submits, Staff replies
  await assertSucceeds(
    custDb.collection("feedback").doc("fb_1").set({
      userId: CUSTOMER_UID,
      customerName: "Hamza Khan",
      rating: 5,
      category: "FOOD",
      comment: "Best smoked mutton karahi in Hasilpur!",
      staffReply: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    staffDb.collection("feedback").doc("fb_1").update({
      staffReply: "Thank you Hamza! See you again at Oak & Smoke.",
      updatedAt: ts(-500),
    })
  );

  // Notifications: Customer reads own & broadcast; Staff sends order update
  await assertSucceeds(
    staffDb.collection("notifications").doc("notif_1").set({
      targetUid: CUSTOMER_UID,
      audience: "USER",
      title: "Order On The Grill",
      message: "Your Mutton Karahi is now sizzling over oak embers.",
      type: "ORDER",
      read: false,
      createdByUid: STAFF_UID,
      createdAt: ts(-1000),
    })
  );
  await assertSucceeds(custDb.collection("notifications").where("targetUid", "==", CUSTOMER_UID).get());
  await assertSucceeds(custDb.collection("notifications").doc("notif_1").update({ read: true }));
});

test("Dining Tables allow Owner/Manager full management, Staff availability toggle, and Customer read-only", async () => {
  await seedOwnerAndStaff();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = ts(-3000);

  // Owner creates a new table
  await assertSucceeds(
    ownerDb.collection("diningTables").doc("table_9").set({
      catalogType: "TABLE",
      label: "Table 9",
      zone: "CHARCOAL_HALL",
      capacity: 6,
      available: true,
      displayOrder: 9,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer and Staff can list tables with catalogType == TABLE
  await assertSucceeds(custDb.collection("diningTables").where("catalogType", "==", "TABLE").get());
  await assertSucceeds(staffDb.collection("diningTables").where("catalogType", "==", "TABLE").get());

  // Customer cannot create or edit tables
  await assertFails(
    custDb.collection("diningTables").doc("table_hack").set({
      catalogType: "TABLE",
      label: "Hack Table",
      zone: "CHARCOAL_HALL",
      capacity: 4,
      available: true,
      displayOrder: 10,
      createdAt: ts(-1000),
      updatedAt: ts(-1000),
    })
  );
  await assertFails(
    custDb.collection("diningTables").doc("table_9").update({
      available: false,
      updatedAt: ts(-1000),
    })
  );

  // Staff can toggle availability, but cannot rename or delete
  await assertSucceeds(
    staffDb.collection("diningTables").doc("table_9").update({
      available: false,
      updatedAt: ts(-1000),
    })
  );
  await assertFails(
    staffDb.collection("diningTables").doc("table_9").update({
      label: "Renamed by Staff",
      updatedAt: ts(-500),
    })
  );
  await assertFails(staffDb.collection("diningTables").doc("table_9").delete());

  // Owner can edit and delete tables
  await assertSucceeds(
    ownerDb.collection("diningTables").doc("table_9").update({
      label: "VIP Table 9",
      capacity: 8,
      available: true,
      updatedAt: ts(-500),
    })
  );
  await assertSucceeds(ownerDb.collection("diningTables").doc("table_9").delete());
});

test("Rider role can fetch customer GPS orders, receive RIDER_READY alerts, deliver COD orders, and stays isolated from Admin data", async () => {
  await seedOwnerAndStaff();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const riderDb = testEnv.authenticatedContext(RIDER_UID).firestore();
  const now = ts(-3000);

  // Owner registers a Rider in staffMembers + staffCredentials
  await assertSucceeds(
    ownerDb.collection("staffMembers").doc("rider_1").set({
      staffId: "RD-101",
      fullName: "Usman Rider",
      roleTier: "RIDER",
      phone: "0301-9876543",
      vehicleInfo: "Honda CD-70 · LHR-4412",
      credHash: RIDER_CRED_HASH,
      linkedUid: "",
      monthlySalaryPkr: 32000,
      salaryStatus: "PENDING",
      loyaltyPoints: 0,
      shiftSummary: "5 PM - 1 AM",
      scheduleDays: "Mon - Sun",
      ordersHandledCount: 0,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    ownerDb.collection("staffCredentials").doc(RIDER_CRED_HASH).set({
      staffDocId: "rider_1",
      staffId: "RD-101",
      roleTier: "RIDER",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Rider elevates using valid Rider ID + Passcode hash
  await assertSucceeds(
    riderDb.collection("roleGrants").doc(RIDER_UID).set({
      userId: RIDER_UID,
      role: "RIDER",
      staffDocId: "rider_1",
      staffId: "RD-101",
      credHash: RIDER_CRED_HASH,
      createdAt: ts(-2000),
      updatedAt: ts(-2000),
    })
  );

  // Customer places COD order with GPS coordinates
  await assertSucceeds(
    custDb.collection("orders").doc("order_rider_1").set({
      orderNumber: "OS-7701",
      userId: CUSTOMER_UID,
      orderType: "COD_DELIVERY",
      tableLabel: "",
      customerName: "Zainab Ali",
      customerPhone: "03009998877",
      deliveryAddress: "Satellite Town, Hasilpur",
      customerLat: 29.7012,
      customerLng: 72.5610,
      locationLabel: "Satellite Town, Hasilpur",
      hasLocation: true,
      itemsSummary: "1x Royal BBQ Platter",
      itemsDetail: "1||Royal BBQ Platter||4450||4450",
      itemCount: 1,
      subtotalPkr: 4450,
      loyaltyPointsRedeemed: 0,
      discountPkr: 0,
      totalPkr: 4450,
      paymentMethod: "COD",
      status: "PLACED",
      handledByStaffId: "",
      notes: "Ring doorbell",
      dateKey: "2026-11-23",
      monthKey: "2026-11",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Staff marks order READY and sends RIDER_READY alert to riders + customer
  await assertSucceeds(
    staffDb.collection("orders").doc("order_rider_1").update({
      status: "READY",
      handledByStaffId: "OS-102",
      assignedRiderId: "RD-101",
      assignedRiderName: "Usman Rider",
      assignedRiderPhone: "0301-9876543",
      updatedAt: ts(-1500),
    })
  );
  await assertSucceeds(
    staffDb.collection("notifications").doc("notif_rider_ready").set({
      targetUid: "ROLE_RIDER",
      audience: "RIDER",
      title: "ORDER READY: OS-7701",
      message: "Pickup for Zainab Ali at Satellite Town, Hasilpur (GPS attached).",
      type: "RIDER_READY",
      read: false,
      createdByUid: STAFF_UID,
      createdAt: ts(-1400),
    })
  );

  // Rider reads RIDER notifications and fetches order with GPS location
  await assertSucceeds(riderDb.collection("notifications").where("audience", "==", "RIDER").get());
  await assertSucceeds(riderDb.collection("orders").doc("order_rider_1").get());

  // Rider advances order READY -> OUT_FOR_DELIVERY -> DELIVERED
  await assertSucceeds(
    riderDb.collection("orders").doc("order_rider_1").update({
      status: "OUT_FOR_DELIVERY",
      handledByStaffId: "RD-101",
      assignedRiderId: "RD-101",
      assignedRiderName: "Usman Rider",
      assignedRiderPhone: "0301-9876543",
      updatedAt: ts(-1000),
    })
  );
  await assertSucceeds(
    riderDb.collection("orders").doc("order_rider_1").update({
      status: "DELIVERED",
      handledByStaffId: "RD-101",
      updatedAt: ts(-500),
    })
  );

  // Rider cannot read expenses, inventory, or edit menu items
  await assertFails(riderDb.collection("expenses").get());
  await assertFails(riderDb.collection("inventory").get());
});



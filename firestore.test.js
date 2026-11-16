const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const nowTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validGoalPayload(uid, overrides = {}) {
  return {
    userId: uid,
    name: "Fondo de emergencia",
    targetAmountCents: 100000,
    currentAmountCents: 25000,
    categoryIcon: "emergency",
    colorKey: "emerald",
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
    ...overrides,
  };
}

function validTransactionPayload(uid, goalId, overrides = {}) {
  return {
    userId: uid,
    goalId,
    goalName: "Fondo de emergencia",
    type: "deposit",
    amountCents: 5000,
    note: "Ahorro quincenal",
    createdAt: nowTimestamp(),
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or write goals or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/goals`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1").set(validGoalPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/transactions`).get());
});

test("Owner: can create, read, update balance, edit details, and delete own goal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalRef = aliceDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1");

  await assertSucceeds(goalRef.set(validGoalPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/goals`).get());

  // Update balance action
  await assertSucceeds(
    goalRef.update({
      currentAmountCents: 50000,
      updatedAt: nowTimestamp(),
    })
  );

  // Edit details action
  await assertSucceeds(
    goalRef.update({
      name: "Fondo de emergencia ampliado",
      targetAmountCents: 150000,
      categoryIcon: "home",
      colorKey: "amber",
      updatedAt: nowTimestamp(),
    })
  );

  await assertSucceeds(goalRef.delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's goals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1").set(validGoalPayload(ALICE_UID))
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/goals`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/goals`).doc("goal_2").set(validGoalPayload(BOB_UID))
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1").update({
      currentAmountCents: 999999,
      updatedAt: nowTimestamp(),
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1").delete());
});

test("Shadow Update & Validation: rejects ghost fields, negative amounts, invalid enums, and spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalsCol = aliceDb.collection(`users/${ALICE_UID}/goals`);

  // Ghost field on create
  await assertFails(
    goalsCol.doc("g_ghost").set(validGoalPayload(ALICE_UID, { isAdmin: true }))
  );

  // Spoofed userId
  await assertFails(
    goalsCol.doc("g_spoof").set(validGoalPayload(BOB_UID))
  );

  // Negative target or current amount
  await assertFails(
    goalsCol.doc("g_neg_target").set(validGoalPayload(ALICE_UID, { targetAmountCents: 0 }))
  );
  await assertFails(
    goalsCol.doc("g_neg_curr").set(validGoalPayload(ALICE_UID, { currentAmountCents: -10 }))
  );

  // Invalid categoryIcon
  await assertFails(
    goalsCol.doc("g_bad_icon").set(validGoalPayload(ALICE_UID, { categoryIcon: "crypto" }))
  );

  // Invalid doc ID
  await assertFails(
    goalsCol.doc("bad id with spaces!").set(validGoalPayload(ALICE_UID))
  );
});

test("Transactions: requires existing goal, enforces immutability on update, and isolates per user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Fails if goal does not exist yet
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/transactions`)
      .doc("tx_orphan")
      .set(validTransactionPayload(ALICE_UID, "missing_goal"))
  );

  // Create goal first
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1").set(validGoalPayload(ALICE_UID))
  );

  // Valid transaction succeeds
  const txRef = aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_1");
  await assertSucceeds(txRef.set(validTransactionPayload(ALICE_UID, "goal_1")));

  // Update is forbidden (immutable ledger)
  await assertFails(txRef.update({ amountCents: 999999 }));

  // Bob cannot read or delete Alice's transaction
  await assertFails(bobDb.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").delete());

  // Alice can delete her transaction
  await assertSucceeds(txRef.delete());
});

test("Batch: creating goal and initial deposit transaction together succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const goalRef = aliceDb.collection(`users/${ALICE_UID}/goals`).doc("batch_goal_1");
  const txRef = aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("batch_tx_1");
  batch.set(goalRef, validGoalPayload(ALICE_UID));
  batch.set(txRef, validTransactionPayload(ALICE_UID, "batch_goal_1"));
  await assertSucceeds(batch.commit());
});

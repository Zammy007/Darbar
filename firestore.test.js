const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validDocPayload(uid, overrides = {}) {
  const ts = pastDate(10000);
  return {
    subjectId: "physics",
    title: "Newtonian Mechanics Notes",
    description: "Chapter 1 formulas and solved examples",
    fileName: "mechanics_ch1.pdf",
    fileExtension: "pdf",
    mimeType: "application/pdf",
    fileSizeBytes: 245760,
    chunkCount: 1,
    uploadComplete: false,
    visibility: "public",
    uploaderId: uid,
    uploaderName: "Alice",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validQuestions10() {
  return Array.from({ length: 10 }, (_, idx) => ({
    question: `General Knowledge Question ${idx + 1}?`,
    options: ["Option A", "Option B", "Option C", "Option D"],
    correctIndex: idx % 4,
    explanation: `Short factual explanation for question ${idx + 1}.`,
    topic: "Science",
  }));
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("documents")
      .where("visibility", "==", "public")
      .where("subjectId", "==", "physics")
      .get()
  );
  await assertFails(
    unauthDb.collection("documents").doc("doc_unauth").set(validDocPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("daily_quizzes").doc("2026-11-25").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("reviewed_materials").get()
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("StudyDocument: Kotlin create, chunk upload, finalize update, and cross-user view succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // 1. Alice creates document metadata (without optional description, matching null-filtered Kotlin Map)
  const docWithoutDesc = validDocPayload(ALICE_UID);
  delete docWithoutDesc.description;
  await assertSucceeds(
    aliceDb.collection("documents").doc("doc_1").set(docWithoutDesc)
  );

  // 2. Alice writes file chunk under parent document
  await assertSucceeds(
    aliceDb
      .collection("documents")
      .doc("doc_1")
      .collection("chunks")
      .doc("chunk_0")
      .set({
        documentId: "doc_1",
        uploaderId: ALICE_UID,
        visibility: "public",
        chunkIndex: 0,
        dataBase64: "JVBERi0xLjQK",
        createdAt: pastDate(3000),
      })
  );

  // 3. Alice finalizes uploadComplete = true (partial update preserving createdAt)
  await assertSucceeds(
    aliceDb.collection("documents").doc("doc_1").update({
      uploadComplete: true,
      updatedAt: pastDate(1000),
    })
  );

  // 4. Bob (another student) queries documents by visibility + subjectId (exact Kotlin query)
  await assertSucceeds(
    bobDb
      .collection("documents")
      .where("visibility", "==", "public")
      .where("subjectId", "==", "physics")
      .get()
  );

  // 5. Bob reads chunks to download/preview (exact Kotlin chunk query)
  await assertSucceeds(
    bobDb
      .collection("documents")
      .doc("doc_1")
      .collection("chunks")
      .where("visibility", "==", "public")
      .where("documentId", "==", "doc_1")
      .get()
  );

  // 6. Bob queries all 6 subjects' public documents for the Home progress tracker
  await assertSucceeds(
    bobDb
      .collection("documents")
      .where("visibility", "==", "public")
      .where("subjectId", "in", [
        "bengali",
        "english",
        "physics",
        "mathematics",
        "chemistry",
        "biology",
      ])
      .get()
  );
});

test("StudyDocument: rejects ghost fields, @DocumentId 'id', invalid subject/ext, future timestamp, and cross-user tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Reject @DocumentId 'id' field in write payload
  await assertFails(
    aliceDb
      .collection("documents")
      .doc("doc_bad_id")
      .set(validDocPayload(ALICE_UID, { id: "doc_bad_id" }))
  );

  // Reject ghost field (Shadow Update Test)
  await assertFails(
    aliceDb
      .collection("documents")
      .doc("doc_ghost")
      .set(validDocPayload(ALICE_UID, { isVerified: true }))
  );

  // Reject unsupported subject
  await assertFails(
    aliceDb
      .collection("documents")
      .doc("doc_bad_subj")
      .set(validDocPayload(ALICE_UID, { subjectId: "astrology" }))
  );

  // Reject disallowed file extension
  await assertFails(
    aliceDb
      .collection("documents")
      .doc("doc_bad_ext")
      .set(validDocPayload(ALICE_UID, { fileExtension: "apk" }))
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("documents")
      .doc("doc_future")
      .set(validDocPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Reject unfiltered collection list query
  await assertFails(aliceDb.collection("documents").get());

  // Seed valid Alice document
  await assertSucceeds(
    aliceDb.collection("documents").doc("doc_owned").set(validDocPayload(ALICE_UID))
  );

  // Bob cannot update or delete Alice's document or inject chunks into Alice's document
  await assertFails(
    bobDb.collection("documents").doc("doc_owned").update({
      title: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("documents").doc("doc_owned").delete());
  await assertFails(
    bobDb
      .collection("documents")
      .doc("doc_owned")
      .collection("chunks")
      .doc("chunk_evil")
      .set({
        documentId: "doc_owned",
        uploaderId: BOB_UID,
        visibility: "public",
        chunkIndex: 1,
        dataBase64: "AAAA",
        createdAt: pastDate(1000),
      })
  );

  // Alice cannot mutate immutable createdAt
  await assertFails(
    aliceDb.collection("documents").doc("doc_owned").update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});

test("DailyQuiz: authenticated creation and shared get succeed; list/update/delete/invalid fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const quizPayload = {
    dateKey: "2026-11-25",
    createdBy: ALICE_UID,
    visibility: "public",
    source: "gemini",
    questions: validQuestions10(),
    createdAt: pastDate(5000),
  };

  // Alice creates today's quiz
  await assertSucceeds(
    aliceDb.collection("daily_quizzes").doc("2026-11-25").set(quizPayload)
  );

  // Bob reads today's quiz by exact dateKey
  await assertSucceeds(
    bobDb.collection("daily_quizzes").doc("2026-11-25").get()
  );

  // Nobody can list all daily_quizzes or update/delete today's quiz
  await assertFails(bobDb.collection("daily_quizzes").get());
  await assertFails(
    aliceDb.collection("daily_quizzes").doc("2026-11-25").update({ source: "gemini" })
  );
  await assertFails(
    aliceDb.collection("daily_quizzes").doc("2026-11-25").delete()
  );

  // Reject wrong question count (not 10)
  await assertFails(
    bobDb
      .collection("daily_quizzes")
      .doc("2026-11-26")
      .set({
        ...quizPayload,
        dateKey: "2026-11-26",
        createdBy: BOB_UID,
        questions: validQuestions10().slice(0, 5),
      })
  );
});

test("QuizAttempt: strictly isolated per user with valid create/update/list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_attempts")
      .doc("2026-11-25")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-25",
        score: 8,
        totalQuestions: 10,
        selectedAnswers: [0, 1, 2, 3, 0, 1, 2, 3, 0, 1],
        completedAt: ts,
        updatedAt: ts,
      })
  );

  // Alice can list her own attempts and update score on retake
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get()
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_attempts")
      .doc("2026-11-25")
      .update({
        score: 9,
        totalQuestions: 10,
        selectedAnswers: [0, 1, 2, 3, 0, 1, 2, 3, 0, 2],
        updatedAt: pastDate(1000),
      })
  );

  // Bob cannot read, list, or write Alice's quiz attempts
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_attempts")
      .doc("2026-11-25")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_attempts")
      .doc("2026-11-26")
      .set({
        userId: BOB_UID,
        dateKey: "2026-11-26",
        score: 10,
        totalQuestions: 10,
        selectedAnswers: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        completedAt: ts,
        updatedAt: ts,
      })
  );
});

test("ReviewedMaterial: strictly isolated per user with valid create/update/list/delete and validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  const reviewRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("reviewed_materials")
    .doc("doc_phys_1");

  // 1. Alice marks a physics document as reviewed
  await assertSucceeds(
    reviewRef.set({
      userId: ALICE_UID,
      documentId: "doc_phys_1",
      subjectId: "physics",
      reviewedAt: ts,
    })
  );

  // 2. Alice can list and idempotently update reviewedAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reviewed_materials").get()
  );
  await assertSucceeds(
    reviewRef.set({
      userId: ALICE_UID,
      documentId: "doc_phys_1",
      subjectId: "physics",
      reviewedAt: pastDate(1000),
    })
  );

  // 3. Reject invalid subjectId, ghost fields, future timestamp, and subjectId mutation
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reviewed_materials")
      .doc("doc_bad_subj")
      .set({
        userId: ALICE_UID,
        documentId: "doc_bad_subj",
        subjectId: "astrology",
        reviewedAt: ts,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reviewed_materials")
      .doc("doc_ghost")
      .set({
        userId: ALICE_UID,
        documentId: "doc_ghost",
        subjectId: "chemistry",
        reviewedAt: ts,
        extraFlag: true,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reviewed_materials")
      .doc("doc_future")
      .set({
        userId: ALICE_UID,
        documentId: "doc_future",
        subjectId: "biology",
        reviewedAt: new Date(Date.now() + 60000),
      })
  );
  await assertFails(
    reviewRef.update({
      subjectId: "chemistry",
      reviewedAt: pastDate(500),
    })
  );

  // 4. Bob cannot read, list, write, or delete Alice's reviewed progress
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reviewed_materials")
      .doc("doc_phys_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reviewed_materials").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reviewed_materials")
      .doc("doc_phys_1")
      .delete()
  );

  // 5. Alice can unmark (delete) her own reviewed material
  await assertSucceeds(reviewRef.delete());
});

test("StudentProfile: private per-user profile create, update, get, and strict isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  const aliceProfileRef = aliceDb.collection("users").doc(ALICE_UID);

  // 1. Alice creates her student profile (name + class)
  await assertSucceeds(
    aliceProfileRef.set({
      userId: ALICE_UID,
      studentName: "Aarav Sen",
      studentClass: "Class 10",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 2. Alice can read and update her own name and class
  await assertSucceeds(aliceProfileRef.get());
  await assertSucceeds(
    aliceProfileRef.update({
      studentName: "Aarav Sen Gupta",
      studentClass: "Class 11",
      updatedAt: pastDate(1000),
    })
  );

  // 3. Reject empty name, ghost fields, immutable createdAt change, and collection listing
  await assertFails(
    aliceProfileRef.update({
      studentName: "",
      studentClass: "Class 11",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceProfileRef.update({
      studentName: "Aarav",
      role: "admin",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceProfileRef.update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertFails(aliceDb.collection("users").get());

  // 4. Bob cannot read, update, or overwrite Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      studentName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
});


const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

const serviceAccount = require("./serviceAccountKey.json");

const app = initializeApp({
    credential: cert(serviceAccount)
});

const db = getFirestore(app);

// IMPORTANT:
// false = preview only
// true  = perform permanent deletion
const EXECUTE_DELETE = true;

async function cleanupSecondFloor() {
    const secondFloor = db
        .collection("libraries")
        .doc("central_library")
        .collection("floors")
        .doc("second");

    const sectionsCollection = secondFloor.collection("sections");

    // Verify the two sections we intend to retain.
    for (const sectionId of ["section_1", "section_2"]) {
        const sectionRef = sectionsCollection.doc(sectionId);
        const sectionDoc = await sectionRef.get();

        if (!sectionDoc.exists) {
            throw new Error(`Missing ${sectionId}. Cleanup stopped.`);
        }

        const tables = await sectionRef.collection("tables").get();
        const tableIds = tables.docs.map(doc => doc.id);

        if (!tableIds.includes("table_1") ||
            !tableIds.includes("table_2")) {
            throw new Error(
                `${sectionId} does not contain both table_1 and table_2. Cleanup stopped.`
            );
        }

        console.log(`${sectionId}:`, tableIds.join(", "));
    }

    const targets = [];

    // Delete section_3 and section_4, including nested data.
    for (const sectionId of ["section_3", "section_4"]) {
        const ref = sectionsCollection.doc(sectionId);
        const snapshot = await ref.get();

        if (snapshot.exists) {
            targets.push(ref);
        }
    }

    // Delete table_3 through table_6 in section_1 and section_2.
    for (const sectionId of ["section_1", "section_2"]) {
        for (let number = 3; number <= 6; number++) {
            const tableId = `table_${number}`;

            const ref = sectionsCollection
                .doc(sectionId)
                .collection("tables")
                .doc(tableId);

            const snapshot = await ref.get();

            if (snapshot.exists) {
                targets.push(ref);
            }
        }
    }

    console.log("\nDocuments selected for deletion:");

    targets.forEach(ref => {
        console.log(" -", ref.path);
    });

    if (!EXECUTE_DELETE) {
        console.log("\nPREVIEW ONLY. Nothing was deleted.");
        console.log("Set EXECUTE_DELETE = true after reviewing the paths.");
        return;
    }

    console.log("\nStarting recursive deletion...");

    for (const ref of targets) {
        await db.recursiveDelete(ref);
        console.log("Deleted:", ref.path);
    }

    // Update retained section metadata.
    for (const sectionId of ["section_1", "section_2"]) {
        await sectionsCollection.doc(sectionId).update({
            tableCount: 2,
            seatCount: 40
        });
    }

    // Ground Floor: 300 seats
    // Second Floor: 80 seats
    await db.collection("libraries")
        .doc("central_library")
        .update({
            totalSeats: 380
        });

    console.log("\nSecond Floor cleanup completed.");
    console.log("Final capacity: 2 sections, 4 tables, 80 seats.");
}

cleanupSecondFloor()
    .then(() => process.exit(0))
    .catch(error => {
        console.error("Cleanup failed:", error);
        process.exit(1);
    });

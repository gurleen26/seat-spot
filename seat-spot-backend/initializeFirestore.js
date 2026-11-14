const { initializeApp, cert, deleteApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

const serviceAccount = require("./serviceAccountKey.json");

const app = initializeApp({
    credential: cert(serviceAccount)
});

const db = getFirestore(app);

const libraryRef = db.collection("libraries").doc("central_library");

const documents = [];

function addDocument(ref, data) {
    documents.push({ ref, data });
}

// --------------------------------------------------
// 1. LIBRARY DOCUMENT
// --------------------------------------------------

addDocument(libraryRef, {
    libraryId: "central_library",
    name: "Central Library",
    location: "",
    totalFloors: 3,
    totalSeats: 780,
    totalComputers: 90,
    totalDiscussionRooms: 2,
    isActive: true
});

// --------------------------------------------------
// 2. FLOOR DOCUMENTS
// --------------------------------------------------

const floors = [
    {
        id: "ground",
        name: "Ground Floor",
        floorNumber: 0,
        description: "Reading areas and discussion rooms"
    },
    {
        id: "first",
        name: "First Floor",
        floorNumber: 1,
        description: "Computer laboratories"
    },
    {
        id: "second",
        name: "Second Floor",
        floorNumber: 2,
        description: "Individual study tables"
    }
];

floors.forEach(floor => {
    addDocument(
        libraryRef.collection("floors").doc(floor.id),
        {
            floorId: floor.id,
            name: floor.name,
            floorNumber: floor.floorNumber,
            description: floor.description,
            isActive: true
        }
    );
});

// --------------------------------------------------
// 3. GROUND FLOOR: 3 AREAS, 15 SECTIONS, 300 SEATS
// --------------------------------------------------

const groundFloorRef = libraryRef
    .collection("floors")
    .doc("ground");

const areaNames = ["A", "B", "C"];

areaNames.forEach(areaName => {

    const areaId = `area_${areaName.toLowerCase()}`;

    const areaRef = groundFloorRef
        .collection("areas")
        .doc(areaId);

    addDocument(areaRef, {
        areaId: areaId,
        areaName: `Area ${areaName}`,
        isActive: true
    });

    for (let sectionNumber = 1; sectionNumber <= 5; sectionNumber++) {

        const sectionId = `${areaName}${sectionNumber}`;

        const sectionRef = areaRef
            .collection("sections")
            .doc(sectionId);

        addDocument(sectionRef, {
            sectionId: sectionId,
            sectionName: `Section ${sectionId}`,
            seatCount: 20,
            isActive: true
        });

        for (let seatNumber = 1; seatNumber <= 20; seatNumber++) {

            const seatId =
                `${sectionId}_S${String(seatNumber).padStart(2, "0")}`;

            addDocument(
                sectionRef.collection("seats").doc(seatId),
                {
                    seatId: seatId,
                    seatNumber: seatNumber,
                    floorId: "ground",
                    areaId: areaId,
                    sectionId: sectionId,
                    tableId: "",
                    seatType: "GENERAL",
                    features: [],
                    isActive: true
                }
            );
        }
    }
});

// --------------------------------------------------
// 4. FIRST FLOOR: 3 LABS, 90 COMPUTERS
// --------------------------------------------------

const firstFloorRef = libraryRef
    .collection("floors")
    .doc("first");

for (let labNumber = 1; labNumber <= 3; labNumber++) {

    const labId = `lab_${labNumber}`;

    const labRef = firstFloorRef
        .collection("labs")
        .doc(labId);

    const isAppleLab = labNumber === 3;

    addDocument(labRef, {
        labId: labId,
        labName: `Computer Lab ${labNumber}`,
        floorId: "first",
        computerCount: 30,
        computerType: isAppleLab ? "APPLE" : "WINDOWS",
        operatingSystem: isAppleLab ? "macOS" : "Windows",
        isActive: true
    });

    for (let pcNumber = 1; pcNumber <= 30; pcNumber++) {

        const computerId =
            `${labId}_pc_${String(pcNumber).padStart(2, "0")}`;

        addDocument(
            labRef.collection("computers").doc(computerId),
            {
                computerId: computerId,
                computerNumber: pcNumber,
                floorId: "first",
                labId: labId,
                computerType: isAppleLab ? "APPLE" : "WINDOWS",
                operatingSystem: isAppleLab ? "macOS" : "Windows",
                specifications: "",
                isActive: true
            }
        );
    }
}

// --------------------------------------------------
// 5. SECOND FLOOR: 4 SECTIONS, 24 TABLES, 480 SEATS
// --------------------------------------------------

const secondFloorRef = libraryRef
    .collection("floors")
    .doc("second");

for (let sectionNumber = 1; sectionNumber <= 4; sectionNumber++) {

    const sectionId = `section_${sectionNumber}`;

    const sectionRef = secondFloorRef
        .collection("sections")
        .doc(sectionId);

    addDocument(sectionRef, {
        sectionId: sectionId,
        sectionName: `Section ${sectionNumber}`,
        tableCount: 6,
        seatCount: 120,
        isActive: true
    });

    for (let tableNumber = 1; tableNumber <= 6; tableNumber++) {

        const tableId = `table_${tableNumber}`;

        const tableRef = sectionRef
            .collection("tables")
            .doc(tableId);

        addDocument(tableRef, {
            tableId: tableId,
            tableName: `Table ${tableNumber}`,
            sectionId: sectionId,
            floorId: "second",
            seatCount: 20,
            isActive: true
        });

        for (let seatNumber = 1; seatNumber <= 20; seatNumber++) {

            const seatId =
                `S${sectionNumber}_T${tableNumber}_S${String(seatNumber).padStart(2, "0")}`;

            addDocument(
                tableRef.collection("seats").doc(seatId),
                {
                    seatId: seatId,
                    seatNumber: seatNumber,
                    floorId: "second",
                    areaId: "",
                    sectionId: sectionId,
                    tableId: tableId,
                    seatType: "GENERAL",
                    features: [],
                    isActive: true
                }
            );
        }
    }
}

// --------------------------------------------------
// 6. TWO DISCUSSION ROOMS
// Their actual floor/area locations are not confirmed.
// --------------------------------------------------

const roomsRef = libraryRef.collection("discussionRooms");

for (let roomNumber = 1; roomNumber <= 2; roomNumber++) {

    const roomId = `discussion_room_${roomNumber}`;

    addDocument(roomsRef.doc(roomId), {
        roomId: roomId,
        roomName: `Discussion Room ${roomNumber}`,
        floorId: "",
        areaId: "",
        sectionId: "",
        capacity: 8,
        defaultDurationMinutes: 60,
        features: [],
        isActive: true
    });
}

// --------------------------------------------------
// 7. SAFE, RE-RUNNABLE DATABASE INITIALIZATION
// --------------------------------------------------

async function initializeFirestore() {

    console.log("Starting SeatSpot library initialization...");
    console.log(`Prepared ${documents.length} documents.`);

    let createdCount = 0;
    let skippedCount = 0;

    try {

        // Check existing documents in groups of 100.
        for (let i = 0; i < documents.length; i += 100) {

            const chunk = documents.slice(i, i + 100);

            const snapshots = await db.getAll(
                ...chunk.map(item => item.ref)
            );

            const missingDocuments = [];

            snapshots.forEach((snapshot, index) => {

                if (snapshot.exists) {
                    skippedCount++;
                } else {
                    missingDocuments.push(chunk[index]);
                }
            });

            // Firestore batch limit is 500 writes.
            // Keep our batches below that limit.
            for (let j = 0; j < missingDocuments.length; j += 400) {

                const writeChunk = missingDocuments.slice(j, j + 400);

                const batch = db.batch();

                writeChunk.forEach(item => {
                    batch.create(item.ref, item.data);
                });

                await batch.commit();

                createdCount += writeChunk.length;

                console.log(
                    `Created ${createdCount} documents so far...`
                );
            }
        }

        console.log("\nSeatSpot initialization completed!");
        console.log(`Documents created: ${createdCount}`);
        console.log(`Existing documents skipped: ${skippedCount}`);
        console.log("Reservations were not modified.");

    } catch (error) {

        console.error("\nInitialization failed:", error);
        process.exitCode = 1;

    } finally {

        await deleteApp(app);
    }
}

initializeFirestore();

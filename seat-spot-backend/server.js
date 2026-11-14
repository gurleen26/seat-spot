require("dotenv").config();

const registerAiRoutes = require("./aiBookingRoutes");

const express = require("express");
const cors = require("cors");

const {
    initializeApp,
    cert
} = require("firebase-admin/app");

const {
    getFirestore,
    Timestamp,
    FieldValue
} = require("firebase-admin/firestore");

const {
    getAuth
} = require("firebase-admin/auth");

const serviceAccount = require("./serviceAccountKey.json");

const firebaseApp = initializeApp({
    credential: cert(serviceAccount)
});

const db = getFirestore(firebaseApp);
const auth = getAuth(firebaseApp);

const app = express();

app.use(cors());
app.use(express.json({ limit: "20kb" }));

// -------------------------------
// PUBLIC ENDPOINTS
// -------------------------------

app.get("/", (req, res) => {
    res.json({
        message: "SeatSpot Backend is running!",
        status: "OK"
    });
});

app.get("/health", (req, res) => {
    res.json({
        success: true,
        service: "SeatSpot API"
    });
});

// -------------------------------
// FIREBASE AUTHENTICATION
// -------------------------------

async function verifyFirebaseToken(req, res, next) {
    try {
        const authHeader = req.headers.authorization;

        if (!authHeader || !authHeader.startsWith("Bearer ")) {
            return res.status(401).json({
                success: false,
                message: "Authentication token is required."
            });
        }

        const idToken = authHeader.substring(7);

        const decodedToken = await auth.verifyIdToken(idToken);

        req.user = {
            uid: decodedToken.uid,
            email: decodedToken.email || null
        };

        next();

    } catch (error) {
        console.error("Authentication error:", error.message);

        return res.status(401).json({
            success: false,
            message: "Invalid or expired authentication token."
        });
    }
}

registerAiRoutes(app, {
    db,
    verifyFirebaseToken
});

// -------------------------------
// PROTECTED USER ENDPOINT
// -------------------------------

app.get("/api/me", verifyFirebaseToken, (req, res) => {
    res.json({
        success: true,
        message: "Authentication successful!",
        user: req.user
    });
});


app.get("/api/reservations", verifyFirebaseToken, async (req, res) => {
    try {
        // Fetch only reservations belonging to the signed-in user.
        const snapshot = await db.collection("reservations")
            .where("userId", "==", req.user.uid)
            .get();

        const reservations = snapshot.docs.map((doc) => {
            const data = doc.data();

            return {
                reservationId: doc.id,
                userId: data.userId || "",
                libraryId: data.libraryId || "",
                resourceType: data.resourceType || "",
                resourceId: data.resourceId || "",
                floorId: data.floorId || "",
                areaId: data.areaId || "",
                sectionId: data.sectionId || "",
                tableId: data.tableId || "",
                labId: data.labId || "",
                date: data.date || "",
                startAt: data.startAt?.toDate().toISOString() || null,
                endAt: data.endAt?.toDate().toISOString() || null,
                durationMinutes: data.durationMinutes || 0,
                memberCount: data.memberCount || 1,
                status: data.status || "PENDING",
                createdAt: data.createdAt?.toDate().toISOString() || null
            };
        });

        // Sort newest reservations first.
        reservations.sort((a, b) => {
            return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
        });

        res.status(200).json({
            success: true,
            count: reservations.length,
            reservations: reservations
        });

    } catch (error) {
        console.error("Fetch reservations error:", error);

        res.status(500).json({
            success: false,
            message: "Unable to fetch reservations."
        });
    }
});

// -------------------------------
// CREATE SEAT / COMPUTER RESERVATION
// -------------------------------

app.post(
    "/api/reservations",
    verifyFirebaseToken,
    async (req, res) => {
        try {
            const {
                resourceType = "SEAT",
                resourceId,
                floorId = "",
                areaId,
                sectionId,
                tableId,
                labId,
                memberCount = 1,
                date,
                startTime,
                endTime
            } = req.body;

            // 1. Validate common required fields

            if (
                !resourceId ||
                (!floorId && resourceType !== "DISCUSSION_ROOM") ||
                !date ||
                !startTime ||
                !endTime
            ) { 
                return res.status(400).json({
                    success: false,
                    message: "All reservation fields are required."
                });
            }

            const validId = /^[A-Za-z0-9_-]+$/;

            if (!validId.test(resourceId)) {
                return res.status(400).json({
                    success: false,
                    message: "Invalid resource identifier."
                });
            }

            // 2. Validate resource-specific location

            let resourceRef;
            let lockId;
            let reservationsQuery;
            let reservationLocation;

            if (resourceType === "SEAT") {

                if (
                    floorId !== "ground" ||
                    !areaId ||
                    !sectionId ||
                    !validId.test(areaId) ||
                    !validId.test(sectionId)
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Invalid ground-floor seat location."
                    });
                }

                resourceRef = db
                    .collection("libraries")
                    .doc("central_library")
                    .collection("floors")
                    .doc("ground")
                    .collection("areas")
                    .doc(areaId)
                    .collection("sections")
                    .doc(sectionId)
                    .collection("seats")
                    .doc(resourceId);

                lockId = `ground_${areaId}_${sectionId}_${resourceId}_${date}`;

                reservationsQuery = db
                    .collection("reservations")
                    .where("floorId", "==", "ground")
                    .where("areaId", "==", areaId)
                    .where("sectionId", "==", sectionId)
                    .where("resourceId", "==", resourceId)
                    .where("date", "==", date);

                reservationLocation = {
                    areaId,
                    sectionId,
                    labId: "",
                    tableId: ""
                };

            } else if (resourceType === "COMPUTER") {

                if (
                    floorId !== "first" ||
                    !labId ||
                    !validId.test(labId)
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Invalid first-floor computer location."
                    });
                }

                resourceRef = db
                    .collection("libraries")
                    .doc("central_library")
                    .collection("floors")
                    .doc("first")
                    .collection("labs")
                    .doc(labId)
                    .collection("computers")
                    .doc(resourceId);

                lockId = `first_${labId}_${resourceId}_${date}`;

                reservationsQuery = db
                    .collection("reservations")
                    .where("floorId", "==", "first")
                    .where("labId", "==", labId)
                    .where("resourceId", "==", resourceId)
                    .where("date", "==", date);

                reservationLocation = {
                    areaId: "",
                    sectionId: "",
                    labId,
                    tableId: ""
                };

            } else if (resourceType === "SECOND_FLOOR_SEAT") {

                if (
                    floorId !== "second" ||
                    !sectionId ||
                    !tableId ||
                    !validId.test(sectionId) ||
                    !validId.test(tableId)
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Invalid second-floor seat location."
                    });
                }

                resourceRef = db
                    .collection("libraries")
                    .doc("central_library")
                    .collection("floors")
                    .doc("second")
                    .collection("sections")
                    .doc(sectionId)
                    .collection("tables")
                    .doc(tableId)
                    .collection("seats")
                    .doc(resourceId);

                lockId = `second_${sectionId}_${tableId}_${resourceId}_${date}`;

                reservationsQuery = db
                    .collection("reservations")
                    .where("floorId", "==", "second")
                    .where("sectionId", "==", sectionId)
                    .where("tableId", "==", tableId)
                    .where("resourceId", "==", resourceId)
                    .where("date", "==", date);

                reservationLocation = {
                    areaId: "",
                    sectionId,
                    labId: "",
                    tableId
                };
            } else if (resourceType === "DISCUSSION_ROOM") {
                if (
                    !Number.isInteger(memberCount) ||
                    memberCount < 2 ||
                    memberCount > 6
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Group size must be between 2 and 6."
                    });
                }

                resourceRef = db
                    .collection("libraries")
                    .doc("central_library")
                    .collection("discussionRooms")
                    .doc(resourceId);

                lockId = `discussion_${resourceId}_${date}`;

                reservationsQuery = db
                    .collection("reservations")
                    .where("date", "==", date);

                reservationLocation = {
                    areaId: "",
                    sectionId: "",
                    labId: "",
                    tableId: ""
                };
            } else {
                return res.status(400).json({
                    success: false,
                    message: "Unsupported resource type."
                });
            }

            // 3. Validate date and time formats

            if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
                return res.status(400).json({
                    success: false,
                    message: "Date must use YYYY-MM-DD format."
                });
            }

            const [year, month, day] = date.split("-").map(Number);

            const calendarDate = new Date(
                Date.UTC(year, month - 1, day)
            );

            if (
                calendarDate.getUTCFullYear() !== year ||
                calendarDate.getUTCMonth() !== month - 1 ||
                calendarDate.getUTCDate() !== day
            ) {
                return res.status(400).json({
                    success: false,
                    message: "Invalid calendar date."
                });
            }

            const validTime = /^([01]\d|2[0-3]):[0-5]\d$/;

            if (
                !validTime.test(startTime) ||
                !validTime.test(endTime)
            ) {
                return res.status(400).json({
                    success: false,
                    message: "Time must use HH:mm format."
                });
            }

            // Interpret booking times as Indian Standard Time.

            const startAt = new Date(
                `${date}T${startTime}:00+05:30`
            );

            const endAt = new Date(
                `${date}T${endTime}:00+05:30`
            );

            if (
                Number.isNaN(startAt.getTime()) ||
                Number.isNaN(endAt.getTime()) ||
                startAt >= endAt
            ) {
                return res.status(400).json({
                    success: false,
                    message: "End time must be later than start time."
                });
            }

            if (startAt <= new Date()) {
                return res.status(400).json({
                    success: false,
                    message: "Reservations must start in the future."
                });
            }

            if (resourceType === "DISCUSSION_ROOM") {
                const durationMinutes =
                    (endAt.getTime() - startAt.getTime()) / 60000;

                if (![60, 90, 120].includes(durationMinutes)) {
                    return res.status(400).json({
                        success: false,
                        message: "Discussion rooms can be booked for 60, 90, or 120 minutes."
                    });
                }
            }

            // 4. Prepare reservation and lock references

            const reservationRef = db
                .collection("reservations")
                .doc();

            const lockRef = db
                .collection("reservationLocks")
                .doc(lockId);

            // 5. Atomic reservation transaction

            await db.runTransaction(async (transaction) => {

                // Complete all reads before writing.

                const resourceSnapshot =
                    await transaction.get(resourceRef);

                if (!resourceSnapshot.exists) {
                    const error = new Error(
                        resourceType === "DISCUSSION_ROOM"
                            ? "Discussion room does not exist."
                            : resourceType === "COMPUTER"
                                ? "Computer does not exist."
                                : "Seat does not exist."
                    );
                    error.statusCode = 404;
                    throw error;
                }

                const resourceData = resourceSnapshot.data();

                if (resourceData.isActive !== true) {
                    const error = new Error(
                        resourceType === "DISCUSSION_ROOM"
                        ? "This discussion room is inactive."
                        : resourceType === "COMPUTER"
                            ? "This computer is inactive."
                            : "This seat is inactive."
                    );
                    error.statusCode = 409;
                    throw error;
                }

                const lockSnapshot = await transaction.get(lockRef);

                const existingReservations =
                    await transaction.get(reservationsQuery);

                // 6. Check overlapping reservations.

                const hasConflict = existingReservations.docs.some(
                    (document) => {
                        const booking = document.data();

                        // For discussion rooms, check conflicts only within the selected room.
                        if (
                            resourceType === "DISCUSSION_ROOM" &&
                            (
                                booking.resourceType !== "DISCUSSION_ROOM" ||
                                booking.resourceId !== resourceId
                            )
                        ) {
                            return false;
                        }

                        if (
                            booking.status !== "PENDING" &&
                            booking.status !== "CONFIRMED"
                        ) {
                            return false;
                        }

                        if (!booking.startAt || !booking.endAt) {
                            return false;
                        }

                        const existingStart =
                            booking.startAt.toMillis();

                        const existingEnd =
                            booking.endAt.toMillis();

                        return (
                            startAt.getTime() < existingEnd &&
                            endAt.getTime() > existingStart
                        );
                    }
                );

                if (hasConflict) {
                    const error = new Error(
                        resourceType === "DISCUSSION_ROOM"
                            ? "This discussion room is already reserved during the selected time."
                            : resourceType === "COMPUTER"
                                ? "This computer is already reserved during the selected time."
                                : "This seat is already reserved during the selected time."
                    );

                    error.statusCode = 409;
                    throw error;
                }

                // 7. Create reservation.

                const lockRevision = lockSnapshot.exists
                    ? (lockSnapshot.data().revision || 0) + 1
                    : 1;

                const reservationData = {
                    reservationId: reservationRef.id,
                    userId: req.user.uid,
                    libraryId: "central_library",

                    resourceType,
                    resourceId,

                    floorId,
                    ...reservationLocation,

                    date,

                    startAt: Timestamp.fromDate(startAt),
                    endAt: Timestamp.fromDate(endAt),

                    durationMinutes:
                        (endAt.getTime() - startAt.getTime()) / 60000,

                    memberCount: resourceType === "DISCUSSION_ROOM"
                        ? memberCount
                        : 1,

                    memberIds: [req.user.uid],

                    status: "CONFIRMED",
                    createdAt: FieldValue.serverTimestamp()
                };

                transaction.create(
                    reservationRef,
                    reservationData
                );

                // Update the lock to serialize booking attempts
                // for this resource and date.

                transaction.set(lockRef, {
                    resourceType,
                    resourceId,
                    date,
                    revision: lockRevision,
                    updatedAt: FieldValue.serverTimestamp()
                });
            });

            // 8. Send successful response.

            return res.status(201).json({
                success: true,
                message: resourceType === "DISCUSSION_ROOM"
                ? "Discussion room reserved successfully!"
                : resourceType === "COMPUTER"
                    ? "Computer reserved successfully!"
                    : "Seat reserved successfully!",
                reservation: {
                    reservationId: reservationRef.id,
                    resourceType,
                    resourceId,
                    date,
                    startTime,
                    endTime,
                    status: "CONFIRMED"
                }
            });

        } catch (error) {
            console.error("Reservation error:", error);

            if (error.statusCode) {
                return res.status(error.statusCode).json({
                    success: false,
                    message: error.message
                });
            }

            return res.status(500).json({
                success: false,
                message: "Unable to create reservation. Please try again."
            });
        }
    }
);


// -------------------------------
// CHECK SEAT / COMPUTER AVAILABILITY
// -------------------------------

app.get(
    "/api/availability",
    verifyFirebaseToken,
    async (req, res) => {
        try {
            const {
                date,
                floorId,
                areaId,
                sectionId,
                tableId,
                labId,
                resourceType,
                roomId
            } = req.query;
            console.log("AVAILABILITY REQUEST:", {
                    date,
                    floorId,
                    areaId,
                    sectionId,
                    tableId,
                    labId
                });

            // 1. Validate common parameters
            if (!date || (!floorId && resourceType !== "DISCUSSION_ROOM")) {
                return res.status(400).json({
                    success: false,
                    message: "Date and floorId are required."
                });
            }

            // 2. Validate date format
            if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
                return res.status(400).json({
                    success: false,
                    message: "Date must use YYYY-MM-DD format."
                });
            }

            const [year, month, day] = date.split("-").map(Number);
            const calendarDate = new Date(
                Date.UTC(year, month - 1, day)
            );

            if (
                calendarDate.getUTCFullYear() !== year ||
                calendarDate.getUTCMonth() !== month - 1 ||
                calendarDate.getUTCDate() !== day
            ) {
                return res.status(400).json({
                    success: false,
                    message: "Invalid calendar date."
                });
            }


            // DISCUSSION ROOM AVAILABILITY
        if (resourceType === "DISCUSSION_ROOM") {

            if (!roomId || !/^[A-Za-z0-9_-]+$/.test(roomId)) {
                return res.status(400).json({
                    success: false,
                    message: "Valid roomId is required."
                });
            }

            const roomRef = db
                .collection("libraries")
                .doc("central_library")
                .collection("discussionRooms")
                .doc(roomId);

            const roomSnapshot = await roomRef.get();

            if (!roomSnapshot.exists || roomSnapshot.data().isActive !== true) {
                return res.status(404).json({
                    success: false,
                    message: "Discussion room not found or inactive."
                });
            }

            const snapshot = await db.collection("reservations")
                .where("date", "==", date)
                .get();

            const reservedSlots = snapshot.docs
                .map(doc => doc.data())
                .filter(booking =>
                    booking.resourceType === "DISCUSSION_ROOM" &&
                    booking.resourceId === roomId &&
                    ["PENDING", "CONFIRMED"].includes(booking.status) &&
                    booking.startAt &&
                    booking.endAt
                )
                .map(booking => ({
                    startAt: booking.startAt.toDate().toISOString(),
                    endAt: booking.endAt.toDate().toISOString()
                }));

            return res.status(200).json({
                success: true,
                date,
                roomId,
                reservedSlots
            });
        }

            const validId = /^[A-Za-z0-9_-]+$/;

            if (!validId.test(floorId)) {
                return res.status(400).json({
                    success: false,
                    message: "Invalid floorId."
                });
            }

            // 3. Validate floor-specific parameters
            if (floorId === "ground") {
                if (
                    !areaId ||
                    !sectionId ||
                    !validId.test(areaId) ||
                    !validId.test(sectionId)
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Ground floor requires valid areaId and sectionId."
                    });
                }
            } else if (floorId === "first") {
                if (!labId || !validId.test(labId)) {
                    return res.status(400).json({
                        success: false,
                        message: "First floor requires a valid labId."
                    });
                }
            } else if (floorId === "second") {
                if (
                    !sectionId ||
                    !tableId ||
                    !validId.test(sectionId) ||
                    !validId.test(tableId)
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Second floor requires valid sectionId and tableId."
                    });
                }
            } else {
                return res.status(400).json({
                    success: false,
                    message: "Availability is not supported for this floor yet."
                });
            }

            // 4. Fetch reservations for the selected date
            const snapshot = await db.collection("reservations")
                .where("date", "==", date)
                .get();

            // 5. Filter reservations by floor and location
            const reservedSeats = snapshot.docs
                .map((doc) => doc.data())
                .filter((booking) => {
                    const isActive = [
                        "PENDING",
                        "CONFIRMED"
                    ].includes(booking.status);

                    const hasValidTimes =
                        booking.startAt &&
                        booking.endAt;

                    if (!isActive || !hasValidTimes) {
                        return false;
                    }

                    if (floorId === "ground") {
                        return (
                            booking.resourceType === "SEAT" &&
                            booking.floorId === "ground" &&
                            booking.areaId === areaId &&
                            booking.sectionId === sectionId
                        );
                    }

                    if (floorId === "first") {
                        return (
                            booking.resourceType === "COMPUTER" &&
                            booking.floorId === "first" &&
                            booking.labId === labId
                        );
                    }

                    if (floorId === "second") {
                        return (
                            booking.resourceType === "SECOND_FLOOR_SEAT" &&
                            booking.floorId === "second" &&
                            booking.sectionId === sectionId &&
                            booking.tableId === tableId
                        );
                    }
                    return false;
                })
                .map((booking) => ({
                    resourceId: booking.resourceId,
                    startAt: booking.startAt.toDate().toISOString(),
                    endAt: booking.endAt.toDate().toISOString()
                }));

            // 6. Send response
            return res.status(200).json({
                success: true,
                date,
                reservedSeats
            });

        } catch (error) {
            console.error("Availability error:", error);

            return res.status(500).json({
                success: false,
                message: "Unable to fetch resource availability."
            });
        }
    }
);


// -------------------------------
// CANCEL SEAT RESERVATION
// -------------------------------

app.delete(
    "/api/reservations/:reservationId",
    verifyFirebaseToken,
    async (req, res) => {
        try {
            const { reservationId } = req.params;

            if (!reservationId || !/^[A-Za-z0-9_-]+$/.test(reservationId)) {
                return res.status(400).json({
                    success: false,
                    message: "Invalid reservation ID."
                });
            }

            const reservationRef = db
                .collection("reservations")
                .doc(reservationId);

            let cancelledReservation;

            await db.runTransaction(async (transaction) => {
                // Read reservation first.
                const reservationSnapshot =
                    await transaction.get(reservationRef);

                if (!reservationSnapshot.exists) {
                    const error = new Error("Reservation not found.");
                    error.statusCode = 404;
                    throw error;
                }

                const booking = reservationSnapshot.data();

                // A user can cancel only their own reservation.
                if (booking.userId !== req.user.uid) {
                    const error = new Error("Reservation not found.");
                    error.statusCode = 404;
                    throw error;
                }

                // Only active reservations can be cancelled.
                if (
                    booking.status !== "PENDING" &&
                    booking.status !== "CONFIRMED"
                ) {
                    const error = new Error(
                        "This reservation cannot be cancelled."
                    );
                    error.statusCode = 409;
                    throw error;
                }

                // Do not allow cancellation after the booking starts.
                if (
                    !booking.startAt ||
                    booking.startAt.toMillis() <= Date.now()
                ) {
                    const error = new Error(
                        "A reservation cannot be cancelled after its start time."
                    );
                    error.statusCode = 409;
                    throw error;
                }

                // Use the same lock as the booking endpoint.
                
                const lockId =
                     booking.resourceType === "DISCUSSION_ROOM"
                ? `discussion_${booking.resourceId}_${booking.date}`
            : booking.resourceType === "COMPUTER"
            ? `first_${booking.labId}_${booking.resourceId}_${booking.date}`
            : booking.resourceType === "SECOND_FLOOR_SEAT"
                ? `second_${booking.sectionId}_${booking.tableId}_${booking.resourceId}_${booking.date}`
                : `ground_${booking.areaId}_${booking.sectionId}_${booking.resourceId}_${booking.date}`;


                const lockRef = db
                    .collection("reservationLocks")
                    .doc(lockId);

                const lockSnapshot = await transaction.get(lockRef);

                const lockRevision = lockSnapshot.exists
                    ? (lockSnapshot.data().revision || 0) + 1
                    : 1;

                // Mark cancelled; do not delete the booking.
                transaction.update(reservationRef, {
                    status: "CANCELLED",
                    cancelledAt: FieldValue.serverTimestamp(),
                    updatedAt: FieldValue.serverTimestamp()
                });

                // Serialize cancellation with new booking attempts.
                transaction.set(
                    lockRef,
                    {
                        resourceId: booking.resourceId,
                        date: booking.date,
                        revision: lockRevision,
                        updatedAt: FieldValue.serverTimestamp()
                    },
                    { merge: true }
                );

                cancelledReservation = {
                    reservationId: reservationSnapshot.id,
                    resourceId: booking.resourceId,
                    status: "CANCELLED"
                };
            });

            return res.status(200).json({
                success: true,
                message: "Reservation cancelled successfully.",
                reservation: cancelledReservation
            });

        } catch (error) {
            console.error("Cancellation error:", error);

            if (error.statusCode) {
                return res.status(error.statusCode).json({
                    success: false,
                    message: error.message
                });
            }

            return res.status(500).json({
                success: false,
                message: "Unable to cancel reservation. Please try again."
            });
        }
    }
);

// -------------------------------
// START SERVER
// -------------------------------

const PORT = process.env.PORT || 3000;

app.listen(PORT, "0.0.0.0", () => {
    console.log(`SeatSpot server running on port ${PORT}`);
});


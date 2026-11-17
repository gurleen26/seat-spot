// aiRoutes.js
const { GoogleGenAI } = require("@google/genai");
const rateLimit = require("express-rate-limit");

const API_KEY = process.env.GEMINI_API_KEY;
const MODEL = process.env.GEMINI_MODEL || "gemini-3.5-flash-lite";

const ai = API_KEY ? new GoogleGenAI({ apiKey: API_KEY }) : null;

const MAX_DAYS_AHEAD = 14;
const MAX_HOURS = 6;
const RESOURCE_CACHE_MS = 5 * 60 * 1000;

// -------------------------------------------------
// Rate limit: 10 requests per minute per user
// -------------------------------------------------

const limiter = rateLimit({
    windowMs: 60 * 1000,
    limit: 10,
    keyGenerator: (req) => (req.user && req.user.uid) || "anonymous",
    standardHeaders: "draft-8",
    legacyHeaders: false,
    message: {
        success: false,
        message: "Too many requests. Please try again after a minute."
    }
});

// -------------------------------------------------
// Date and time helpers
// -------------------------------------------------

function getISTDate() {
    const parts = new Intl.DateTimeFormat("en-CA", {
        timeZone: "Asia/Kolkata",
        year: "numeric",
        month: "2-digit",
        day: "2-digit"
    }).formatToParts(new Date());

    const values = {};
    for (const part of parts) {
        values[part.type] = part.value;
    }

    return `${values.year}-${values.month}-${values.day}`;
}

function getISTWeekday() {
    return new Intl.DateTimeFormat("en-US", {
        timeZone: "Asia/Kolkata",
        weekday: "long"
    }).format(new Date());
}

function addDaysToDate(date, days) {
    const [year, month, day] = date.split("-").map(Number);
    const result = new Date(Date.UTC(year, month - 1, day + days));

    return [
        result.getUTCFullYear(),
        String(result.getUTCMonth() + 1).padStart(2, "0"),
        String(result.getUTCDate()).padStart(2, "0")
    ].join("-");
}

function isValidDate(date) {
    if (typeof date !== "string") return false;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return false;

    const [year, month, day] = date.split("-").map(Number);
    const parsed = new Date(Date.UTC(year, month - 1, day));

    return (
        parsed.getUTCFullYear() === year &&
        parsed.getUTCMonth() === month - 1 &&
        parsed.getUTCDate() === day
    );
}

function isValidTime(time) {
    return typeof time === "string" &&
        /^([01]\d|2[0-3]):[0-5]\d$/.test(time);
}

function getTimestamp(date, time) {
    return new Date(`${date}T${time}:00+05:30`).getTime();
}

function friendlyDate(date) {
    return new Date(`${date}T00:00:00+05:30`).toLocaleDateString("en-GB", {
        timeZone: "Asia/Kolkata",
        weekday: "short",
        day: "numeric",
        month: "short"
    });
}

// -------------------------------------------------
// Small helpers
// -------------------------------------------------

function shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
}

function pretty(id) {
    return String(id)
        .replace(/_/g, " ")
        .replace(/\b\w/g, (c) => c.toUpperCase());
}

// -------------------------------------------------
// Gemini: understand the user's sentence
// -------------------------------------------------

async function parseBookingRequest(text, history = []) {
    const today = getISTDate();
    const weekday = getISTWeekday();
const conversationText = history
    .map((item) => {
        const role = item.role === "assistant" ? "Assistant" : "User";
        return `${role}: ${item.text}`;
    })
    .join("\n");
    

const prompt = `
You are SeatSpot's intelligent library booking assistant.

CURRENT DATE: ${today} (${weekday})
TIMEZONE: Asia/Kolkata

You are handling a multi-turn conversation. You MUST remember
details provided in previous messages.

PREVIOUS CONVERSATION:
${conversationText || "No previous conversation."}

LATEST USER MESSAGE:
"${text}"

CONVERSATION RULES:
1. Combine previous messages with the latest user message.
2. Never forget previously provided booking details.
3. Interpret short replies using the conversation context.
4. If the user says "4", understand it as group size 4 if that
   was the question you previously asked.
5. If the user says "tomorrow 9 AM", retain the previously
   specified resource type and group size.
6. If the user changes a detail, use the new detail.
7. Ask only for missing required information.
8. Never ask the user to repeat information already provided.
9. Reply in the same language as the latest user message.
10. Do not randomly switch between English, Hindi and Hinglish.

BOOKING EXTRACTION:

kind must be one of:
SEAT, COMPUTER, DISCUSSION_ROOM, UNKNOWN.

- SEAT: study seat, desk or table.
- COMPUTER: computer, PC, Mac or laptop.
- DISCUSSION_ROOM: discussion room or group room.

DATE AND TIME:
- "today" or "aaj" means today.
- "tomorrow" or "kal" means tomorrow.
- "parso" means the day after tomorrow.
- Use YYYY-MM-DD for dates.
- Use 24-hour HH:mm for times.
- "subah 9" means 09:00.
- "shaam 4" means 16:00.
- Library hours are 08:00 to 22:00.

DISCUSSION ROOM:
- groupSize must be between 2 and 6.
- durationMinutes must be 60, 90 or 120.
- If duration is not specified, use 60 minutes.
- Calculate endTime using startTime + durationMinutes.
- A discussion-room request requires a valid date and startTime.
- If group size is missing, ask for it.
- Do not forget the group size from previous messages.

SEAT AND COMPUTER:
- Require date, startTime and endTime.
- Do not invent missing dates or times.
- Ask one short clarification question when information is missing.

PREFERENCE:
- Preserve any floor, area, lab or computer-type preference.
- Use an empty string if no preference is given.

RESPONSE FORMAT:
Return ONLY valid JSON in this structure:

{
  "understood": true,
  "kind": "DISCUSSION_ROOM",
  "date": "YYYY-MM-DD",
  "startTime": "09:00",
  "endTime": "10:00",
  "preference": "",
  "groupSize": 4,
  "durationMinutes": 60,
  "clarification": ""
}

When information is missing:
- Set understood to false.
- Set clarification to one short question.
- Do not invent missing information.

For SEAT and COMPUTER, use groupSize 0 and durationMinutes 0.
For UNKNOWN, use empty strings for unavailable date/time fields.

Return valid JSON only. No Markdown or additional explanation.
`;




    const result = await ai.models.generateContent({
        model: MODEL,
        contents: prompt,
        config: {
            responseMimeType: "application/json"
        }
    });

    if (!result.text) {
        throw new Error("Empty response received from Gemini.");
    }

    return JSON.parse(result.text);
}

// -------------------------------------------------
// Load all bookable seats and computers from Firestore
// -------------------------------------------------

async function loadResources(db) {
    const resources = [];

    const libraryRef = db.collection("libraries").doc("central_library");

    // Ground floor seats
    const areasSnapshot = await libraryRef
        .collection("floors")
        .doc("ground")
        .collection("areas")
        .get();

    for (const areaDoc of areasSnapshot.docs) {
        const areaName = areaDoc.data().areaName || pretty(areaDoc.id);

        const sectionsSnapshot = await areaDoc.ref
            .collection("sections")
            .get();

        for (const sectionDoc of sectionsSnapshot.docs) {
            const sectionName =
                sectionDoc.data().sectionName || `Section ${sectionDoc.id}`;

            const seatsSnapshot = await sectionDoc.ref
                .collection("seats")
                .get();

            for (const seatDoc of seatsSnapshot.docs) {
                const data = seatDoc.data();

                if (data.isActive !== true) continue;

                const seatNumber = Number(data.seatNumber || data.number || 0);

                resources.push({
                    resourceType: "SEAT",
                    resourceId: seatDoc.id,
                    floorId: "ground",
                    areaId: areaDoc.id,
                    sectionId: sectionDoc.id,
                    tableId: "",
                    labId: "",
                    seatNumber,
                    label: `Ground Floor · ${areaName} · ${sectionName} · Seat ${seatNumber}`
                });
            }
        }
    }

    // First floor computers
    const labsSnapshot = await libraryRef
        .collection("floors")
        .doc("first")
        .collection("labs")
        .get();

    for (const labDoc of labsSnapshot.docs) {
        const labName = labDoc.data().labName || pretty(labDoc.id);

        const computersSnapshot = await labDoc.ref
            .collection("computers")
            .get();

        for (const computerDoc of computersSnapshot.docs) {
            const data = computerDoc.data();

            if (data.isActive !== true) continue;

            const computerNumber = Number(
                data.computerNumber || data.number || 0
            );
            const rawType = String(data.computerType || "").toUpperCase();

            const computerType =
                rawType.includes("APPLE") ||
                rawType.includes("MAC")
                    ? "APPLE"
                    : rawType.includes("WINDOWS") ||
                    rawType.includes("WIN")
                        ? "WINDOWS"
                        : labDoc.id === "lab_3"
                            ? "APPLE"
                            : "UNKNOWN";
            const macNote = computerType === "APPLE" ? " (Mac)" : "";

            resources.push({
                resourceType: "COMPUTER",
                resourceId: computerDoc.id,
                floorId: "first",
                areaId: labDoc.id,
                sectionId: "",
                tableId: "",
                labId: labDoc.id,
                seatNumber: computerNumber,
                computerType,
                label: `First Floor · ${labName} · PC ${computerNumber}${macNote}`
            });
        }
    }

    // Second floor seats
    const secondSectionsSnapshot = await libraryRef
        .collection("floors")
        .doc("second")
        .collection("sections")
        .get();

    for (const sectionDoc of secondSectionsSnapshot.docs) {
        const sectionName =
            sectionDoc.data().sectionName || pretty(sectionDoc.id);

        const tablesSnapshot = await sectionDoc.ref
            .collection("tables")
            .get();

        for (const tableDoc of tablesSnapshot.docs) {
            const tableName = tableDoc.data().tableName || pretty(tableDoc.id);

            const seatsSnapshot = await tableDoc.ref
                .collection("seats")
                .get();

            for (const seatDoc of seatsSnapshot.docs) {
                const data = seatDoc.data();

                if (data.isActive !== true) continue;

                const seatNumber = Number(data.seatNumber || data.number || 0);

                resources.push({
                    resourceType: "SECOND_FLOOR_SEAT",
                    resourceId: seatDoc.id,
                    floorId: "second",
                    areaId: "",
                    sectionId: sectionDoc.id,
                    tableId: tableDoc.id,
                    labId: "",
                    seatNumber,
                    label: `Second Floor · ${sectionName} · ${tableName} · Seat ${seatNumber}`
                });
            }
        }
    }


    // Discussion rooms
    const roomsSnapshot = await libraryRef
        .collection("discussionRooms")
        .get();

    for (const roomDoc of roomsSnapshot.docs) {
        const data = roomDoc.data();

        if (data.active !== true) continue;

        resources.push({
            resourceType: "DISCUSSION_ROOM",
            resourceId: roomDoc.id,
            floorId: "",
            areaId: "",
            sectionId: "",
            tableId: "",
            labId: "",
            seatNumber: 0,
            capacity: Number(data.capacity || 0),
            defaultDurationMinutes: Number(data.defaultDurationMinutes || 60),
            label: data.roomName || data.name || pretty(roomDoc.id)
        });
    }


    return resources;
}

// Cache the resource list for a few minutes (it rarely changes)
let resourceCache = { at: 0, list: null };

async function getResources(db) {
    if (
        resourceCache.list &&
        Date.now() - resourceCache.at < RESOURCE_CACHE_MS
    ) {
        return resourceCache.list;
    }

    const list = await loadResources(db);

    if (list.length > 0) {
        resourceCache = { at: Date.now(), list };
    }

    return list;
}

// -------------------------------------------------
// Availability and preference filters
// -------------------------------------------------

function isResourceAvailable(resource, bookings, startAt, endAt) {
    return !bookings.some((booking) => {

        if (
            booking.resourceType !== resource.resourceType ||
            booking.resourceId !== resource.resourceId
        ) {
            return false;
        }

        if (
            resource.resourceType !== "DISCUSSION_ROOM" &&
            booking.floorId !== resource.floorId
        ) {
            return false;
        }

        if (
            resource.resourceType === "SEAT" &&
            (
                booking.areaId !== resource.areaId ||
                booking.sectionId !== resource.sectionId
            )
        ) {
            return false;
        }

        if (
            resource.resourceType === "COMPUTER" &&
            booking.labId !== resource.labId
        ) {
            return false;
        }

        if (
            resource.resourceType === "SECOND_FLOOR_SEAT" &&
            (
                booking.sectionId !== resource.sectionId ||
                booking.tableId !== resource.tableId
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

        return (
            startAt < booking.endAt.toMillis() &&
            endAt > booking.startAt.toMillis()
        );
    });
}

function matchesPreference(resource, preferenceText) {
    const pref = String(preferenceText || "").trim().toLowerCase();

    if (!pref) return true;

    if (/\b(any|koi bhi|anywhere|none|no preference|n\/a)\b/.test(pref)) {
        return true;
    }

    if (pref.includes("ground") && resource.floorId !== "ground") return false;
    if (pref.includes("first") && resource.floorId !== "first") return false;
    if (pref.includes("second") && resource.floorId !== "second") return false;

    const lab = pref.match(/lab[\s_-]*(\d+)/);
    if (lab && resource.labId !== `lab_${lab[1]}`) return false;

    if (/mac|apple/.test(pref) && resource.computerType !== "APPLE") return false;
    if (/windows|win/.test(pref) && resource.computerType !== "WINDOWS") return false;

    return true;
}

// Pick up to 3 options from different sections/tables/labs, in random order
function pickOptions(list, count = 3) {
    const groups = new Map();

    for (const resource of list) {
        const key = [
            resource.floorId,
            resource.areaId,
            resource.sectionId,
            resource.tableId,
            resource.labId
        ].join("|");

        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(resource);
    }

    const picked = [];
    const leftovers = [];

    for (const items of shuffle([...groups.values()])) {
        shuffle(items);

        if (picked.length < count) {
            picked.push(items.shift());
        }

        leftovers.push(...items);
    }

    shuffle(leftovers);

    while (picked.length < count && leftovers.length > 0) {
        picked.push(leftovers.pop());
    }

    return picked;
}

function toOption(resource) {
    return {
        resourceType: resource.resourceType,
        resourceId: resource.resourceId,
        floorId: resource.floorId,
        areaId: resource.areaId,
        sectionId: resource.sectionId,
        tableId: resource.tableId,
        labId: resource.labId,
        seatNumber: resource.seatNumber,
        capacity: resource.capacity || 0,
        defaultDurationMinutes: resource.defaultDurationMinutes || 60,
        label: resource.label
    };
}


// -------------------------------------------------
// Route
// -------------------------------------------------

module.exports = function registerAiRoutes(
    app,
    { db, verifyFirebaseToken }
) {

    app.post(
        "/api/ai/parse-booking",
        verifyFirebaseToken,
        limiter,
        async (req, res) => {

            const { text } = req.body || {};

            // Answers that are the user's problem (not a server error)
            const notUnderstood = (message) =>
                res.status(200).json({
                    success: true,
                    understood: false,
                    message,
                    proposal: null
                });

            try {
                if (!ai) {
                    return res.status(500).json({
                        success: false,
                        message: "Gemini API key is not configured."
                    });
                }

                if (
                    typeof text !== "string" ||
                    !text.trim() ||
                    text.length > 200
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Please enter a request between 1 and 200 characters."
                    });
                }

                const history = Array.isArray(req.body.history)
                    ? req.body.history
                        .slice(-10)
                        .filter(
                            (item) =>
                                item &&
                                ["user", "assistant"].includes(item.role) &&
                                typeof item.text === "string"
                        )
                        .map((item) => ({
                            role: item.role,
                            text: item.text.slice(0, 200)
                        }))
                    : [];

                if (
                    typeof text !== "string" ||
                    !text.trim() ||
                    text.length > 200
                ) {
                    return res.status(400).json({
                        success: false,
                        message: "Please enter a request between 1 and 200 characters."
                    });
                }

                let parsed;

                try {
                    parsed = await parseBookingRequest(text.trim(), history);
                } catch (error) {
                    console.error("Gemini error:", error.message);

                    return res.status(502).json({
                        success: false,
                        message: "The assistant is unavailable right now. Please try again."
                    });
                }

                if (!parsed || typeof parsed !== "object") {
                    throw new Error("Invalid Gemini response.");
                }

                console.log("AI parsed:", {
                    understood: parsed.understood,
                    kind: parsed.kind,
                    date: parsed.date,
                    startTime: parsed.startTime,
                    endTime: parsed.endTime,
                    preference: parsed.preference
                });


                if (
                    parsed.understood !== true ||
                    !["SEAT", "COMPUTER", "DISCUSSION_ROOM"].includes(parsed.kind)
                ) {
                    return notUnderstood(
                        parsed.clarification ||
                        "Please tell me what you need, the date, and the required time."
                    );
                }

                if (parsed.kind === "DISCUSSION_ROOM") {
                    const groupSize = Number(parsed.groupSize);
                    const durationMinutes = Number(parsed.durationMinutes);

                    if (!Number.isInteger(groupSize) || groupSize < 2 || groupSize > 6) {
                        return notUnderstood("Please specify a group size between 2 and 6 people.");
                    }

                    if (![60, 90, 120].includes(durationMinutes)) {
                        return notUnderstood("Discussion rooms can be booked for 60, 90 or 120 minutes.");
                    }
                }
                if (parsed.kind === "DISCUSSION_ROOM") {
                    const expectedEndAt =
                        getTimestamp(parsed.date, parsed.startTime) +
                        Number(parsed.durationMinutes) * 60 * 1000;

                    if (
                        !isValidDate(parsed.date) ||
                        !isValidTime(parsed.startTime) ||
                        !isValidTime(parsed.endTime) ||
                        getTimestamp(parsed.date, parsed.endTime) !== expectedEndAt
                    ) {
                        return notUnderstood(
                            "Please provide a valid discussion-room date and start time."
                        );
                    }
                }

                const { date, startTime, endTime } = parsed;

                if (
                    !isValidDate(date) ||
                    !isValidTime(startTime) ||
                    !isValidTime(endTime)
                ) {
                    return notUnderstood(
                        parsed.clarification ||
                        "Please tell me the day and the start and end time, for example: tomorrow 10 am to 12 pm."
                    );
                }

                const startAt = getTimestamp(date, startTime);
                const endAt = getTimestamp(date, endTime);

                const today = getISTDate();
                const maxDate = addDaysToDate(today, MAX_DAYS_AHEAD);

                if (date < today || date > maxDate) {
                    return notUnderstood(
                        `Bookings are available only for today and the next ${MAX_DAYS_AHEAD} days.`
                    );
                }

                if (
                    Number.isNaN(startAt) ||
                    Number.isNaN(endAt) ||
                    startAt <= Date.now() ||
                    endAt <= startAt ||
                    endAt - startAt > MAX_HOURS * 60 * 60 * 1000
                ) {
                    return notUnderstood(
                        `Please choose a future time slot of up to ${MAX_HOURS} hours.`
                    );
                }

                const openingTime = getTimestamp(date, "08:00");
                const closingTime = getTimestamp(date, "22:00");

                if (startAt < openingTime || endAt > closingTime) {
                    return notUnderstood(
                        "Library bookings are available between 08:00 AM and 10:00 PM."
                    );
                }

                const [resources, reservationSnapshot] = await Promise.all([
                    getResources(db),
                    db.collection("reservations")
                        .where("date", "==", date)
                        .get()
                ]);

                const bookings = reservationSnapshot.docs.map(
                    (doc) => doc.data()
                );

                const matchingResources = resources.filter((resource) => {
                    if (parsed.kind === "DISCUSSION_ROOM") {
                        return (
                            resource.resourceType === "DISCUSSION_ROOM" &&
                            resource.capacity >= Number(parsed.groupSize)
                        );
                    }

                    const typeMatches =
                        parsed.kind === "COMPUTER"
                            ? resource.resourceType === "COMPUTER"
                            : (
                                resource.resourceType === "SEAT" ||
                                resource.resourceType === "SECOND_FLOOR_SEAT"
                            );

                    return typeMatches &&
                        matchesPreference(resource, parsed.preference);
                });

                const freeResources = matchingResources.filter((resource) =>
                    isResourceAvailable(resource, bookings, startAt, endAt)
                );

                console.log("AI search:", {
                    resources: resources.length,
                    matching: matchingResources.length,
                    free: freeResources.length
                });

                const options = pickOptions(freeResources, 3).map(toOption);

                if (options.length === 0) {
                    return res.status(200).json({
                        success: true,
                        understood: true,
                        message: "Sorry, nothing matching is free for that time. Try another time or location.",
                        proposal: {
                            date,
                            startTime,
                            endTime,
                            groupSize: parsed.kind === "DISCUSSION_ROOM" ? Number(parsed.groupSize) : 0,
                            durationMinutes: parsed.kind === "DISCUSSION_ROOM" ? Number(parsed.durationMinutes) : 0,
                            options: []
                        }
                    });
                }

                const countText =
                    options.length === 1
                        ? "I found 1 free option"
                        : `I found ${options.length} free options`;

                return res.status(200).json({
                    success: true,
                    understood: true,
                    message: `${countText} for ${friendlyDate(date)}, ${startTime} to ${endTime}. Tap one to continue.`,
                    proposal: {
                        date,
                        startTime,
                        endTime,
                        groupSize: parsed.kind === "DISCUSSION_ROOM"
                            ? Number(parsed.groupSize) : 0,
                        durationMinutes: parsed.kind === "DISCUSSION_ROOM"
                            ? Number(parsed.durationMinutes) : 0,
                        options
                    }
                });

            } catch (error) {
                console.error("AI booking error:", error);

                return res.status(500).json({
                    success: false,
                    message: "Unable to process your booking request right now. Please try again."
                });
            }
        }
    );
};
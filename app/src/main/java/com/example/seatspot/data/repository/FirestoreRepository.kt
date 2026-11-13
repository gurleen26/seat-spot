
package com.example.seatspot.data.repository

import com.example.seatspot.data.model.Library
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.seatspot.data.model.Seat
import com.example.seatspot.data.model.ComputerStation

class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Fetch library information once
    fun getLibrary(
        onResult: (Result<Library>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    val library = document.toObject(Library::class.java)

                    if (library != null) {
                        onResult(
                            Result.success(
                                library.copy(libraryId = document.id)
                            )
                        )
                    } else {
                        onResult(
                            Result.failure(
                                Exception("Unable to read library data.")
                            )
                        )
                    }
                } else {
                    onResult(
                        Result.failure(
                            Exception("Library document does not exist.")
                        )
                    )
                }
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    // Listen for real-time library information changes
    fun observeLibrary(
        onUpdate: (Result<Library>) -> Unit
    ): ListenerRegistration {

        return firestore.collection("libraries")
            .document("central_library")
            .addSnapshotListener { document, exception ->

                if (exception != null) {
                    onUpdate(Result.failure(exception))
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val library = document.toObject(Library::class.java)

                    if (library != null) {
                        onUpdate(
                            Result.success(
                                library.copy(libraryId = document.id)
                            )
                        )
                    } else {
                        onUpdate(
                            Result.failure(
                                Exception("Unable to read library data.")
                            )
                        )
                    }
                } else {
                    onUpdate(
                        Result.failure(
                            Exception("Library document does not exist.")
                        )
                    )
                }
            }
    }


    fun getGroundFloorAreas(
        onResult: (Result<List<String>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("ground")
            .collection("areas")
            .get()
            .addOnSuccessListener { snapshot ->
                val areas = snapshot.documents.map { document ->
                    document.id
                }.sorted()

                onResult(Result.success(areas))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }


    fun getGroundFloorSections(
        areaId: String,
        onResult: (Result<List<String>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("ground")
            .collection("areas")
            .document(areaId)
            .collection("sections")
            .get()
            .addOnSuccessListener { snapshot ->
                val sections = snapshot.documents.map { document ->
                    document.id
                }.sorted()

                onResult(Result.success(sections))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }


    fun getGroundFloorSeats(
        areaId: String,
        sectionId: String,
        onResult: (Result<List<Seat>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("ground")
            .collection("areas")
            .document(areaId)
            .collection("sections")
            .document(sectionId)
            .collection("seats")
            .get()
            .addOnSuccessListener { snapshot ->

                val seats = snapshot.documents.mapNotNull { document ->
                    document.toObject(Seat::class.java)
                        ?.copy(seatId = document.id)
                }.sortedBy { it.seatNumber }

                onResult(Result.success(seats))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun getFirstFloorLabs(
        onResult: (Result<List<String>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("first")
            .collection("labs")
            .get()
            .addOnSuccessListener { snapshot ->

                val labs = snapshot.documents
                    .map { document -> document.id }
                    .sorted()

                onResult(Result.success(labs))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun getFirstFloorComputers(
        labId: String,
        onResult: (Result<List<ComputerStation>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("first")
            .collection("labs")
            .document(labId)
            .collection("computers")
            .get()
            .addOnSuccessListener { snapshot ->

                val computers = snapshot.documents
                    .mapNotNull { document ->
                        document.toObject(ComputerStation::class.java)
                            ?.copy(computerId = document.id)
                    }
                    .sortedBy { it.computerNumber }

                onResult(Result.success(computers))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }


    // SECOND FLOOR: Fetch all sections
    fun getSecondFloorSections(
        onResult: (Result<List<String>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("second")
            .collection("sections")
            .get()
            .addOnSuccessListener { snapshot ->
                val sections = snapshot.documents
                    .map { document -> document.id }
                    .sorted()

                onResult(Result.success(sections))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }


    // SECOND FLOOR: Fetch tables inside a section
    fun getSecondFloorTables(
        sectionId: String,
        onResult: (Result<List<String>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("second")
            .collection("sections")
            .document(sectionId)
            .collection("tables")
            .get()
            .addOnSuccessListener { snapshot ->
                val tables = snapshot.documents
                    .map { document -> document.id }
                    .sorted()

                onResult(Result.success(tables))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }


    // SECOND FLOOR: Fetch seats inside a table
    fun getSecondFloorSeats(
        sectionId: String,
        tableId: String,
        onResult: (Result<List<Seat>>) -> Unit
    ) {
        firestore.collection("libraries")
            .document("central_library")
            .collection("floors")
            .document("second")
            .collection("sections")
            .document(sectionId)
            .collection("tables")
            .document(tableId)
            .collection("seats")
            .get()
            .addOnSuccessListener { snapshot ->
                val seats = snapshot.documents
                    .mapNotNull { document ->
                        document.toObject(Seat::class.java)
                            ?.copy(
                                seatId = document.id,
                                floorId = "second",
                                sectionId = sectionId,
                                tableId = tableId
                            )
                    }
                    .sortedBy { it.seatNumber }

                onResult(Result.success(seats))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }



}

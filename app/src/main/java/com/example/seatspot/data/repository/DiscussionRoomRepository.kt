
package com.example.seatspot.data.repository

import com.example.seatspot.data.model.DiscussionRoom
import com.google.firebase.firestore.FirebaseFirestore

class DiscussionRoomRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getDiscussionRooms(
        onResult: (Result<List<DiscussionRoom>>) -> Unit
    ) {
        db.collection("libraries")
            .document("central_library")
            .collection("discussionRooms")
            .get()
            .addOnSuccessListener { snapshot ->

                val rooms = snapshot.documents.mapNotNull { document ->
                    document.toObject(DiscussionRoom::class.java)
                        ?.copy(roomId = document.id)
                }.distinctBy { it.roomId }

                onResult(Result.success(rooms))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }
}

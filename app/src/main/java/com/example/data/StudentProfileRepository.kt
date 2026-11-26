package com.example.data

import android.util.Log
import com.example.model.StudentProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val PROFILE_TAG = "StudentProfileRepo"

class StudentProfileRepository(
    private val db: FirebaseFirestore
) {
    private val usersCollection get() = db.collection("users")

    private fun currentUid(): String? = Firebase.auth.currentUser?.uid

    fun observeStudentProfile(): Flow<StudentProfile?> = callbackFlow {
        val uid = currentUid()
        if (uid.isNullOrBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = usersCollection
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "users/$uid")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val profile = runCatching {
                    snapshot.toObject(StudentProfile::class.java)
                }.getOrNull()
                trySend(profile)
            }

        awaitClose { registration.remove() }
    }

    suspend fun saveStudentProfile(
        studentName: String,
        studentClass: String
    ): Result<StudentProfile> = runCatching {
        val uid = currentUid() ?: throw IllegalStateException("Please sign in first.")
        val cleanName = studentName.trim().take(80)
        val cleanClass = studentClass.trim().take(40)
        require(cleanName.isNotEmpty()) { "Please enter your name." }
        require(cleanClass.isNotEmpty()) { "Please select or enter your class." }

        val docRef = usersCollection.document(uid)
        val existingSnap = docRef.get().await()

        if (existingSnap.exists()) {
            val updatePayload = hashMapOf<String, Any>(
                "studentName" to cleanName,
                "studentClass" to cleanClass,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
        } else {
            val createPayload = hashMapOf<String, Any>(
                "userId" to uid,
                "studentName" to cleanName,
                "studentClass" to cleanClass,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(createPayload).await()
        }

        StudentProfile(
            id = uid,
            userId = uid,
            studentName = cleanName,
            studentClass = cleanClass
        )
    }.onFailure { err ->
        Log.w(PROFILE_TAG, "Failed saving student profile", err)
        handleFirestoreError(err, OperationType.WRITE, "users/${currentUid().orEmpty()}")
    }
}

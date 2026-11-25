package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import com.example.R
import com.example.model.ALLOWED_FILE_EXTENSIONS
import com.example.model.FILE_CHUNK_RAW_BYTES
import com.example.model.MAX_UPLOAD_BYTES
import com.example.model.PickedFileInfo
import com.example.model.StudyDocument
import com.example.model.Subject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.ceil

class DocumentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing documents.")
    }

    /**
     * Real-time stream of all completed public documents for a specific subject.
     * Query filters strictly match `firestore.rules` `allow list` predicates.
     */
    fun observeDocuments(subject: Subject): Flow<List<StudyDocument>> = flow {
        val uid = requireUserId()
        val path = "documents"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .whereEqualTo("subjectId", subject.id)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.uploadComplete }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Uploads file metadata and chunked Base64 file bytes to Firestore so every student
     * on any device can permanently view, preview, and download the study material.
     */
    suspend fun uploadDocument(
        context: Context,
        subject: Subject,
        title: String,
        description: String,
        pickedFile: PickedFileInfo,
        uploaderName: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        var docPath = "documents"
        return try {
            val uid = requireUserId()
            val trimmedTitle = title.trim()
            val trimmedDescription = description.trim()
            val ext = pickedFile.extension.lowercase(Locale.US)

            require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 160) {
                "Title must be between 1 and 160 characters."
            }
            require(trimmedDescription.length <= 500) {
                "Description must be 500 characters or fewer."
            }
            require(ext in ALLOWED_FILE_EXTENSIONS) {
                "Unsupported file type (.$ext). Allowed: PDF, JPG, PNG, WEBP, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT."
            }

            onProgress(0.05f)

            val fileBytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(pickedFile.uriString))?.use { input ->
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(chunk)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_UPLOAD_BYTES) {
                            throw IllegalArgumentException(
                                "File exceeds the 8 MB limit. Please compress or choose a smaller file."
                            )
                        }
                        buffer.write(chunk, 0, read)
                    }
                    buffer.toByteArray()
                } ?: throw IllegalStateException("Could not read the selected file.")
            }

            require(fileBytes.isNotEmpty()) { "Selected file is empty." }

            val chunkCount = ceil(fileBytes.size.toDouble() / FILE_CHUNK_RAW_BYTES).toInt().coerceAtLeast(1)
            require(chunkCount <= 20) { "File exceeds maximum chunk count (20)." }

            val docRef = db.collection("documents").document()
            docPath = docRef.path
            val safeFileName = pickedFile.fileName.take(200).ifBlank { "document.$ext" }
            val safeMime = pickedFile.mimeType.take(120).ifBlank { guessMimeType(ext) }
            val safeUploaderName = uploaderName.trim().take(100).ifBlank { "Student" }

            onProgress(0.12f)

            // 1. Create parent document metadata (uploadComplete = false until all chunks succeed)
            val metadataMap = buildMap<String, Any?> {
                put("subjectId", subject.id)
                put("title", trimmedTitle)
                if (trimmedDescription.isNotEmpty()) {
                    put("description", trimmedDescription)
                }
                put("fileName", safeFileName)
                put("fileExtension", ext)
                put("mimeType", safeMime)
                put("fileSizeBytes", fileBytes.size.toLong())
                put("chunkCount", chunkCount.toLong())
                put("uploadComplete", false)
                put("visibility", "public")
                put("uploaderId", uid)
                put("uploaderName", safeUploaderName)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }.filterValues { it != null }

            docRef.set(metadataMap).await()
            onProgress(0.20f)

            // 2. Write sequential Base64 chunks under documents/{docId}/chunks/chunk_{i}
            try {
                for (index in 0 until chunkCount) {
                    val start = index * FILE_CHUNK_RAW_BYTES
                    val end = minOf(start + FILE_CHUNK_RAW_BYTES, fileBytes.size)
                    val slice = fileBytes.copyOfRange(start, end)
                    val encoded = Base64.encodeToString(slice, Base64.NO_WRAP)

                    val chunkDocRef = docRef.collection("chunks").document("chunk_$index")
                    val chunkPayload = mapOf(
                        "documentId" to docRef.id,
                        "uploaderId" to uid,
                        "visibility" to "public",
                        "chunkIndex" to index.toLong(),
                        "dataBase64" to encoded,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    chunkDocRef.set(chunkPayload).await()

                    val progress = 0.20f + (0.72f * ((index + 1).toFloat() / chunkCount.toFloat()))
                    onProgress(progress)
                }

                // 3. Mark uploadComplete = true (preserving immutable createdAt)
                docRef.update(
                    mapOf(
                        "uploadComplete" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                onProgress(1.0f)
            } catch (chunkErr: Exception) {
                // Clean up partial document on failure so broken entries don't linger
                if (chunkErr !is CancellationException) {
                    runCatching { docRef.delete().await() }
                }
                throw chunkErr
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Reconstructs the original binary file bytes from Firestore chunks.
     */
    suspend fun downloadDocumentBytes(documentId: String): Result<ByteArray> {
        val path = "documents/$documentId/chunks"
        return try {
            requireUserId()
            require(documentId.isNotBlank()) { "Document ID cannot be blank." }
            val snapshot = db.collection("documents")
                .document(documentId)
                .collection("chunks")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("documentId", documentId)
                .get()
                .await()

            val orderedChunks = snapshot.documents
                .map { doc ->
                    val index = (doc.data?.get("chunkIndex") as? Number)?.toInt() ?: 0
                    val base64 = doc.getString("dataBase64").orEmpty()
                    index to base64
                }
                .sortedBy { it.first }

            if (orderedChunks.isEmpty()) {
                throw IllegalStateException("File content could not be found in cloud storage.")
            }

            val reconstructed = withContext(Dispatchers.IO) {
                val out = ByteArrayOutputStream()
                for ((_, encoded) in orderedChunks) {
                    val decoded = Base64.decode(encoded, Base64.DEFAULT)
                    out.write(decoded)
                }
                out.toByteArray()
            }
            Result.success(reconstructed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Deletes a document and its chunks (permitted only for the original uploader).
     */
    suspend fun deleteDocument(documentId: String): Result<Unit> {
        val path = "documents/$documentId"
        return try {
            requireUserId()
            require(documentId.isNotBlank()) { "Document ID cannot be blank." }
            val docRef = db.collection("documents").document(documentId)
            val chunksSnap = docRef.collection("chunks")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("documentId", documentId)
                .get()
                .await()
            for (chunkDoc in chunksSnap.documents) {
                chunkDoc.reference.delete().await()
            }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        fun inspectPickedFile(context: Context, uri: Uri): PickedFileInfo? {
            val resolver = context.contentResolver
            var displayName = "document"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }

            val rawMime = resolver.getType(uri).orEmpty()
            val nameExt = displayName.substringAfterLast('.', "").lowercase(Locale.US)
            val mimeExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(rawMime)?.lowercase(Locale.US).orEmpty()
            val resolvedExt = when {
                nameExt in ALLOWED_FILE_EXTENSIONS -> nameExt
                mimeExt in ALLOWED_FILE_EXTENSIONS -> mimeExt
                rawMime.contains("pdf") -> "pdf"
                rawMime.contains("wordprocessingml") -> "docx"
                rawMime.contains("msword") -> "doc"
                rawMime.contains("presentationml") -> "pptx"
                rawMime.contains("ms-powerpoint") -> "ppt"
                rawMime.contains("spreadsheetml") -> "xlsx"
                rawMime.contains("ms-excel") -> "xls"
                rawMime.contains("plain") -> "txt"
                else -> nameExt.ifBlank { mimeExt }
            }

            val finalMime = rawMime.ifBlank { guessMimeType(resolvedExt) }
            val normalizedFileName = if (displayName.contains('.')) displayName else "$displayName.$resolvedExt"

            return PickedFileInfo(
                uriString = uri.toString(),
                fileName = normalizedFileName,
                extension = resolvedExt,
                mimeType = finalMime,
                sizeBytes = sizeBytes
            )
        }

        fun guessMimeType(ext: String): String = when (ext.lowercase(Locale.US)) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.2f MB", mb)
        }
    }
}

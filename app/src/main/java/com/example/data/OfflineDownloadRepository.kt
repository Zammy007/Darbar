package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.OfflineDocument
import com.example.model.StudyDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

private const val OFFLINE_TAG = "OfflineDownloadRepo"
private const val OFFLINE_DIR_NAME = "darbar_offline_downloads"
private const val INDEX_FILE_NAME = "downloads_index.json"

/**
 * Local device-only offline storage for downloaded Darbar study materials.
 * Uses zero-permission app-private filesDir so documents survive cache clears
 * and remain fully readable without an internet connection.
 */
class OfflineDownloadRepository(context: Context) {

    private val appContext = context.applicationContext
    private val downloadsDir: File by lazy {
        File(appContext.filesDir, OFFLINE_DIR_NAME).apply { mkdirs() }
    }
    private val indexFile: File by lazy {
        File(downloadsDir, INDEX_FILE_NAME)
    }
    private val mutex = Mutex()

    private val _downloads = MutableStateFlow<List<OfflineDocument>>(emptyList())
    val downloads: StateFlow<List<OfflineDocument>> = _downloads.asStateFlow()

    init {
        _downloads.value = loadPersistedIndex()
    }

    fun isDownloaded(documentId: String): Boolean {
        val entry = _downloads.value.firstOrNull { it.documentId == documentId } ?: return false
        return File(downloadsDir, entry.localFileName).exists()
    }

    fun getOfflineFile(offlineDocument: OfflineDocument): File? {
        val file = File(downloadsDir, offlineDocument.localFileName)
        return if (file.exists() && file.isFile) file else null
    }

    suspend fun saveDocument(
        document: StudyDocument,
        bytes: ByteArray
    ): Result<OfflineDocument> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                require(document.id.isNotBlank()) { "Document ID is missing." }
                require(bytes.isNotEmpty()) { "Downloaded file is empty." }

                downloadsDir.mkdirs()

                val safeId = document.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val safeExt = document.fileExtension
                    .lowercase(Locale.US)
                    .replace(Regex("[^a-z0-9]"), "")
                    .ifBlank { "bin" }
                val localFileName = "${safeId}.${safeExt}"
                val targetFile = File(downloadsDir, localFileName)
                val tempFile = File(downloadsDir, "${localFileName}.tmp")

                tempFile.writeBytes(bytes)
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                val entry = OfflineDocument(
                    documentId = document.id,
                    subjectId = document.subjectId,
                    title = document.title,
                    description = document.description,
                    fileName = document.fileName.ifBlank { "${document.title}.$safeExt" },
                    fileExtension = safeExt,
                    mimeType = document.mimeType.ifBlank { "*/*" },
                    fileSizeBytes = bytes.size.toLong(),
                    localFileName = localFileName,
                    downloadedAtMillis = System.currentTimeMillis()
                )

                val updated = listOf(entry) + _downloads.value.filterNot { it.documentId == document.id }
                persistIndex(updated)
                _downloads.value = updated
                entry
            }.onFailure { err ->
                Log.w(OFFLINE_TAG, "Failed saving offline document ${document.id}", err)
            }
        }
    }

    suspend fun readDocumentBytes(documentId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val entry = _downloads.value.firstOrNull { it.documentId == documentId }
                ?: throw IllegalStateException("Document is not saved offline.")
            val file = File(downloadsDir, entry.localFileName)
            if (!file.exists()) {
                throw IllegalStateException("Offline file is no longer available on device.")
            }
            file.readBytes()
        }
    }

    suspend fun removeDocument(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val existing = _downloads.value.firstOrNull { it.documentId == documentId }
                if (existing != null) {
                    val file = File(downloadsDir, existing.localFileName)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                val updated = _downloads.value.filterNot { it.documentId == documentId }
                persistIndex(updated)
                _downloads.value = updated
            }.onFailure { err ->
                Log.w(OFFLINE_TAG, "Failed removing offline document $documentId", err)
            }
        }
    }

    private fun loadPersistedIndex(): List<OfflineDocument> {
        return runCatching {
            if (!indexFile.exists()) return emptyList()
            val raw = indexFile.readText()
            if (raw.isBlank()) return emptyList()

            val arr = JSONArray(raw)
            val result = mutableListOf<OfflineDocument>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val localFileName = obj.optString("localFileName", "")
                if (localFileName.isBlank()) continue
                val backingFile = File(downloadsDir, localFileName)
                if (!backingFile.exists()) continue

                result.add(
                    OfflineDocument(
                        documentId = obj.optString("documentId", ""),
                        subjectId = obj.optString("subjectId", "bengali"),
                        title = obj.optString("title", "Untitled"),
                        description = obj.optString("description", "").takeIf { it.isNotBlank() },
                        fileName = obj.optString("fileName", localFileName),
                        fileExtension = obj.optString("fileExtension", ""),
                        mimeType = obj.optString("mimeType", "*/*"),
                        fileSizeBytes = obj.optLong("fileSizeBytes", backingFile.length()),
                        localFileName = localFileName,
                        downloadedAtMillis = obj.optLong("downloadedAtMillis", backingFile.lastModified())
                    )
                )
            }
            result.sortedByDescending { it.downloadedAtMillis }
        }.getOrElse { err ->
            Log.w(OFFLINE_TAG, "Could not parse offline downloads index", err)
            emptyList()
        }
    }

    private fun persistIndex(items: List<OfflineDocument>) {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("documentId", item.documentId)
                put("subjectId", item.subjectId)
                put("title", item.title)
                put("description", item.description.orEmpty())
                put("fileName", item.fileName)
                put("fileExtension", item.fileExtension)
                put("mimeType", item.mimeType)
                put("fileSizeBytes", item.fileSizeBytes)
                put("localFileName", item.localFileName)
                put("downloadedAtMillis", item.downloadedAtMillis)
            }
            arr.put(obj)
        }
        val tempIndex = File(downloadsDir, "${INDEX_FILE_NAME}.tmp")
        tempIndex.writeText(arr.toString())
        if (indexFile.exists()) {
            indexFile.delete()
        }
        if (!tempIndex.renameTo(indexFile)) {
            tempIndex.copyTo(indexFile, overwrite = true)
            tempIndex.delete()
        }
    }
}

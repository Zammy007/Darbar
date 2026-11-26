package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiTutorRepository
import com.example.data.DailyQuizRepository
import com.example.data.DocumentRepository
import com.example.data.OfflineDownloadRepository
import com.example.data.StudentProfileRepository
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.DailyQuiz
import com.example.model.FALLBACK_GK_QUESTIONS
import com.example.model.OfflineDocument
import com.example.model.PickedFileInfo
import com.example.model.PreviewPayload
import com.example.model.QuizAttempt
import com.example.model.SortOrder
import com.example.model.StudentProfile
import com.example.model.StudyDocument
import com.example.model.Subject
import com.example.model.SubjectProgress
import com.example.model.SubjectTab
import com.example.model.UPLOAD_PASSWORD
import com.example.model.UiState
import java.io.File
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val VM_TAG = "DarbarViewModel"

sealed interface UploadStatus {
    data object Idle : UploadStatus
    data class Uploading(val progress: Float) : UploadStatus
    data class Success(val title: String) : UploadStatus
    data class Error(val message: String) : UploadStatus
}

enum class QuizPhase {
    INTRO,
    IN_PROGRESS,
    RESULT,
    REVIEW
}

data class QuizSessionState(
    val quizState: UiState<DailyQuiz> = UiState.Loading,
    val phase: QuizPhase = QuizPhase.INTRO,
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val todayAttempt: QuizAttempt? = null,
    val usingOfflineBank: Boolean = false
)

data class AiTutorUiState(
    val messages: List<ChatMessage> = emptyList(),
    val focusSubject: Subject? = null,
    val isSending: Boolean = false,
    val lastFailedPrompt: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DarbarViewModel(
    private val documentRepository: DocumentRepository,
    private val quizRepository: DailyQuizRepository,
    private val offlineDownloadRepository: OfflineDownloadRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val aiTutorRepository: AiTutorRepository = AiTutorRepository(),
    val currentUserId: String,
    val currentUserName: String
) : ViewModel() {

    // --- STUDENT ACCOUNT PROFILE STATE (NAME & CLASS) ---
    val studentProfileState: StateFlow<UiState<StudentProfile?>> = studentProfileRepository
        .observeStudentProfile()
        .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(VM_TAG, "Error observing student profile", firestoreError ?: error)
            emit(UiState.Success(null))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Loading
        )

    val effectiveStudentName: StateFlow<String> = studentProfileState
        .map { state ->
            (state as? UiState.Success)?.data?.studentName?.takeIf { it.isNotBlank() }
                ?: currentUserName.ifBlank { "Student" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentUserName.ifBlank { "Student" }
        )

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    // --- OFFLINE DOWNLOADS STATE ---
    val offlineDownloads: StateFlow<List<OfflineDocument>> = offlineDownloadRepository.downloads
    val downloadedDocumentIds: StateFlow<Set<String>> = offlineDownloadRepository.downloads
        .map { list -> list.map { it.documentId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = offlineDownloadRepository.downloads.value.map { it.documentId }.toSet()
        )

    // --- SESSION-ONLY UPLOAD LOCK STATE ---
    // Intentionally stored only in memory for the current session.
    private val _isUploadUnlocked = MutableStateFlow(false)
    val isUploadUnlocked: StateFlow<Boolean> = _isUploadUnlocked.asStateFlow()

    // --- SUBJECT SCREEN STATE ---
    private val _selectedSubject = MutableStateFlow(Subject.BENGALI)
    val selectedSubject: StateFlow<Subject> = _selectedSubject.asStateFlow()

    private val _activeSubjectTab = MutableStateFlow(SubjectTab.VIEW)
    val activeSubjectTab: StateFlow<SubjectTab> = _activeSubjectTab.asStateFlow()

    // Independent search query per subject ID
    private val _searchQueries = MutableStateFlow<Map<String, String>>(emptyMap())
    val searchQueries: StateFlow<Map<String, String>> = _searchQueries.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    // --- DOCUMENT PREVIEW & DOWNLOAD STATE ---
    private val _loadingDocumentId = MutableStateFlow<String?>(null)
    val loadingDocumentId: StateFlow<String?> = _loadingDocumentId.asStateFlow()

    private val _previewPayload = MutableStateFlow<PreviewPayload?>(null)
    val previewPayload: StateFlow<PreviewPayload?> = _previewPayload.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // --- LIVE SUBJECT DOCUMENTS STREAM WITH TWO-TIER CANCELLATION UNWRAPPING ---
    private val rawSubjectDocuments: StateFlow<UiState<List<StudyDocument>>> = _selectedSubject
        .flatMapLatest { subject ->
            documentRepository.observeDocuments(subject)
                .map<List<StudyDocument>, UiState<List<StudyDocument>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(VM_TAG, "Error observing documents for ${subject.id}", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load documents"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Filtered & sorted documents for the currently open subject
    val filteredSubjectDocuments: StateFlow<UiState<List<StudyDocument>>> = combine(
        rawSubjectDocuments,
        _selectedSubject,
        _searchQueries,
        _sortOrder
    ) { docsState, subject, queries, sort ->
        when (docsState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> docsState
            is UiState.Success -> {
                val query = queries[subject.id].orEmpty().trim()
                val filtered = if (query.isEmpty()) {
                    docsState.data
                } else {
                    docsState.data.filter { doc ->
                        doc.title.contains(query, ignoreCase = true) ||
                            doc.description.orEmpty().contains(query, ignoreCase = true)
                    }
                }
                val sorted = when (sort) {
                    SortOrder.NEWEST_FIRST -> filtered.sortedByDescending {
                        it.createdAt ?: Timestamp(0, 0)
                    }
                    SortOrder.OLDEST_FIRST -> filtered.sortedBy {
                        it.createdAt ?: Timestamp(0, 0)
                    }
                    SortOrder.A_TO_Z -> filtered.sortedBy { it.title.lowercase() }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    // --- VISUAL PROGRESS TRACKER (ALL 6 SUBJECTS + REVIEWED MATERIALS) ---
    private val allPublicDocuments: StateFlow<List<StudyDocument>> = documentRepository
        .observeAllPublicDocuments()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(VM_TAG, "Error observing all public documents for progress tracker", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val reviewedDocumentIds: StateFlow<Set<String>> = documentRepository
        .observeReviewedMaterials()
        .map { list -> list.map { it.documentId }.toSet() }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(VM_TAG, "Error observing reviewed materials", firestoreError ?: error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptySet()
        )

    val subjectProgressMap: StateFlow<Map<Subject, SubjectProgress>> = combine(
        allPublicDocuments,
        reviewedDocumentIds
    ) { docs, reviewedIds ->
        val grouped = docs.groupBy { it.subjectId }
        Subject.entries.associateWith { subject ->
            val subjectDocs = grouped[subject.id].orEmpty()
            val reviewedCount = subjectDocs.count { it.id in reviewedIds }
            SubjectProgress(
                subject = subject,
                reviewedCount = reviewedCount,
                totalCount = subjectDocs.size
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = Subject.entries.associateWith { SubjectProgress(it, 0, 0) }
    )

    // --- DAILY QUIZ STATE ---
    private val _quizSession = MutableStateFlow(QuizSessionState())
    val quizSession: StateFlow<QuizSessionState> = _quizSession.asStateFlow()

    // --- MULTI-TURN GEMINI AI TUTOR CHAT STATE ---
    private var activeChatJob: Job? = null
    private val _aiTutorState = MutableStateFlow(
        AiTutorUiState(messages = listOf(createWelcomeMessage()))
    )
    val aiTutorState: StateFlow<AiTutorUiState> = _aiTutorState.asStateFlow()

    init {
        loadTodayQuizAttemptOnly()
    }

    private fun createWelcomeMessage(): ChatMessage {
        val firstName = effectiveStudentName.value.substringBefore(' ').ifBlank { "Student" }
        return ChatMessage(
            id = "welcome_${UUID.randomUUID()}",
            role = ChatRole.MODEL,
            text = "স্বাগতম, $firstName! I'm your দরবার AI Tutor.\n\nAsk me anything across Bengali, English, Physics, Mathematics, Chemistry, or Biology — in English or বাংলা. I remember our conversation so you can ask follow-up questions anytime."
        )
    }

    // --- SUBJECT & TAB NAVIGATION ---
    fun openSubject(subject: Subject) {
        _selectedSubject.value = subject
        _activeSubjectTab.value = SubjectTab.VIEW
        _uploadStatus.value = UploadStatus.Idle
    }

    fun selectSubjectTab(tab: SubjectTab) {
        if (tab == SubjectTab.UPLOAD && !_isUploadUnlocked.value) {
            return
        }
        _activeSubjectTab.value = tab
    }

    /**
     * Exact, strict, case-sensitive equality check against UPLOAD_PASSWORD ("Tana007").
     * No trimming, lowercasing, or altering of the user's input.
     */
    fun verifyUploadPassword(input: String): Boolean {
        val matched = (input == UPLOAD_PASSWORD)
        if (matched) {
            _isUploadUnlocked.value = true
            _activeSubjectTab.value = SubjectTab.UPLOAD
        }
        return matched
    }

    fun lockUploads() {
        _isUploadUnlocked.value = false
        if (_activeSubjectTab.value == SubjectTab.UPLOAD) {
            _activeSubjectTab.value = SubjectTab.VIEW
        }
    }

    fun updateSearchQuery(subject: Subject, query: String) {
        _searchQueries.update { current ->
            current + (subject.id to query)
        }
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun resetUploadStatus() {
        _uploadStatus.value = UploadStatus.Idle
    }

    fun uploadStudyDocument(
        context: Context,
        subject: Subject,
        title: String,
        description: String,
        pickedFile: PickedFileInfo
    ) {
        if (!_isUploadUnlocked.value) {
            _uploadStatus.value = UploadStatus.Error("Upload section is locked. Please unlock with password.")
            return
        }
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Uploading(0.05f)
            val result = documentRepository.uploadDocument(
                context = context,
                subject = subject,
                title = title,
                description = description,
                pickedFile = pickedFile,
                uploaderName = effectiveStudentName.value,
                onProgress = { progress ->
                    _uploadStatus.value = UploadStatus.Uploading(progress.coerceIn(0f, 1f))
                }
            )
            result.fold(
                onSuccess = {
                    _uploadStatus.value = UploadStatus.Success(title.trim())
                },
                onFailure = { error ->
                    _uploadStatus.value = UploadStatus.Error(
                        error.localizedMessage ?: "Upload failed. Please try again."
                    )
                }
            )
        }
    }

    fun openDocumentPreview(document: StudyDocument) {
        if (!document.canPreview()) return
        viewModelScope.launch {
            _loadingDocumentId.value = document.id
            // Prefer instant local offline copy if already downloaded
            val localResult = offlineDownloadRepository.readDocumentBytes(document.id)
            val result = if (localResult.isSuccess) {
                localResult
            } else {
                documentRepository.downloadDocumentBytes(document.id)
            }
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = { bytes ->
                    _previewPayload.value = PreviewPayload(document = document, bytes = bytes)
                    markReviewedSilently(document)
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Unable to load preview."
                }
            )
        }
    }

    fun dismissPreview() {
        _previewPayload.value = null
    }

    fun downloadDocumentForOffline(document: StudyDocument) {
        viewModelScope.launch {
            if (offlineDownloadRepository.isDownloaded(document.id)) {
                _actionMessage.value = "\"${document.title}\" is already saved in Downloads."
                return@launch
            }
            _loadingDocumentId.value = document.id
            val result = documentRepository.downloadDocumentBytes(document.id)
            result.fold(
                onSuccess = { bytes ->
                    val saveRes = offlineDownloadRepository.saveDocument(document, bytes)
                    _loadingDocumentId.value = null
                    saveRes.fold(
                        onSuccess = {
                            markReviewedSilently(document)
                            _actionMessage.value = "Downloaded \"${document.title}\" for offline viewing."
                        },
                        onFailure = { err ->
                            _actionMessage.value = err.localizedMessage ?: "Could not save offline copy."
                        }
                    )
                },
                onFailure = { err ->
                    _loadingDocumentId.value = null
                    _actionMessage.value = err.localizedMessage ?: "Download failed. Please check your connection."
                }
            )
        }
    }

    fun fetchDocumentForDownload(
        document: StudyDocument,
        onBytesReady: (StudyDocument, ByteArray) -> Unit
    ) {
        viewModelScope.launch {
            _loadingDocumentId.value = document.id
            val localResult = offlineDownloadRepository.readDocumentBytes(document.id)
            val result = if (localResult.isSuccess) {
                localResult
            } else {
                documentRepository.downloadDocumentBytes(document.id).also { fetched ->
                    fetched.onSuccess { bytes ->
                        offlineDownloadRepository.saveDocument(document, bytes)
                    }
                }
            }
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = { bytes ->
                    markReviewedSilently(document)
                    onBytesReady(document, bytes)
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Download failed. Please try again."
                }
            )
        }
    }

    fun openOfflinePreview(offlineDoc: OfflineDocument) {
        if (!offlineDoc.canPreview()) return
        viewModelScope.launch {
            _loadingDocumentId.value = offlineDoc.documentId
            val result = offlineDownloadRepository.readDocumentBytes(offlineDoc.documentId)
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = { bytes ->
                    _previewPayload.value = PreviewPayload(
                        document = offlineDoc.toStudyDocument(),
                        bytes = bytes
                    )
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Unable to open offline preview."
                }
            )
        }
    }

    fun readOfflineBytesForExport(
        offlineDoc: OfflineDocument,
        onReady: (StudyDocument, ByteArray) -> Unit
    ) {
        viewModelScope.launch {
            _loadingDocumentId.value = offlineDoc.documentId
            val result = offlineDownloadRepository.readDocumentBytes(offlineDoc.documentId)
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = { bytes ->
                    onReady(offlineDoc.toStudyDocument(), bytes)
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Could not read offline file."
                }
            )
        }
    }

    fun getOfflineFile(offlineDoc: OfflineDocument): File? =
        offlineDownloadRepository.getOfflineFile(offlineDoc)

    fun removeOfflineDownload(offlineDoc: OfflineDocument) {
        viewModelScope.launch {
            offlineDownloadRepository.removeDocument(offlineDoc.documentId).fold(
                onSuccess = {
                    _actionMessage.value = "Removed \"${offlineDoc.title}\" from offline downloads."
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Could not remove offline file."
                }
            )
        }
    }

    private fun markReviewedSilently(document: StudyDocument) {
        if (document.id.isBlank() || document.id in reviewedDocumentIds.value) return
        viewModelScope.launch {
            documentRepository.markMaterialReviewed(
                documentId = document.id,
                subject = Subject.fromId(document.subjectId)
            )
        }
    }

    fun toggleMaterialReviewed(document: StudyDocument) {
        if (document.id.isBlank()) return
        val currentlyReviewed = document.id in reviewedDocumentIds.value
        viewModelScope.launch {
            val result = if (currentlyReviewed) {
                documentRepository.unmarkMaterialReviewed(document.id)
            } else {
                documentRepository.markMaterialReviewed(
                    documentId = document.id,
                    subject = Subject.fromId(document.subjectId)
                )
            }
            result.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not update review status."
            }
        }
    }

    fun deleteDocument(document: StudyDocument) {
        viewModelScope.launch {
            _loadingDocumentId.value = document.id
            val result = documentRepository.deleteDocument(document.id)
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = {
                    _actionMessage.value = "Deleted \"${document.title}\""
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Could not delete document."
                }
            )
        }
    }

    fun postActionMessage(message: String) {
        _actionMessage.value = message
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun saveStudentProfile(
        studentName: String,
        studentClass: String,
        onSaved: () -> Unit = {}
    ) {
        if (_isSavingProfile.value) return
        viewModelScope.launch {
            _isSavingProfile.value = true
            _profileError.value = null
            val result = studentProfileRepository.saveStudentProfile(
                studentName = studentName,
                studentClass = studentClass
            )
            _isSavingProfile.value = false
            result.fold(
                onSuccess = {
                    _actionMessage.value = "Profile updated successfully."
                    onSaved()
                },
                onFailure = { err ->
                    _profileError.value = err.localizedMessage ?: "Could not save profile. Please try again."
                }
            )
        }
    }

    // --- DAILY QUIZ FLOW ---
    private fun loadTodayQuizAttemptOnly() {
        viewModelScope.launch {
            quizRepository.getTodayAttempt().onSuccess { attempt ->
                _quizSession.update { it.copy(todayAttempt = attempt) }
            }
        }
    }

    fun loadDailyQuiz(forceReload: Boolean = false) {
        val current = _quizSession.value
        if (!forceReload && current.quizState is UiState.Success && !current.usingOfflineBank) {
            return
        }
        viewModelScope.launch {
            _quizSession.update {
                it.copy(
                    quizState = UiState.Loading,
                    usingOfflineBank = false
                )
            }
            val quizResult = quizRepository.getOrGenerateTodayQuiz()
            val attemptResult = quizRepository.getTodayAttempt()
            val attempt = attemptResult.getOrNull() ?: _quizSession.value.todayAttempt

            quizResult.fold(
                onSuccess = { dailyQuiz ->
                    _quizSession.update {
                        it.copy(
                            quizState = UiState.Success(dailyQuiz),
                            todayAttempt = attempt,
                            phase = QuizPhase.INTRO,
                            currentQuestionIndex = 0,
                            selectedAnswers = emptyMap(),
                            usingOfflineBank = false
                        )
                    }
                },
                onFailure = { err ->
                    _quizSession.update {
                        it.copy(
                            quizState = UiState.Error(
                                err.localizedMessage ?: "Couldn't generate today's quiz. Check your connection and retry."
                            ),
                            todayAttempt = attempt
                        )
                    }
                }
            )
        }
    }

    fun startOfflinePracticeQuiz() {
        val fallbackQuiz = DailyQuiz(
            dateKey = quizRepository.todayDateKey(),
            questions = FALLBACK_GK_QUESTIONS,
            source = "offline_bank"
        )
        _quizSession.update {
            it.copy(
                quizState = UiState.Success(fallbackQuiz),
                phase = QuizPhase.IN_PROGRESS,
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                usingOfflineBank = true
            )
        }
    }

    fun startQuiz() {
        _quizSession.update {
            it.copy(
                phase = QuizPhase.IN_PROGRESS,
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap()
            )
        }
    }

    fun selectQuizAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _quizSession.value
        if (current.selectedAnswers.containsKey(questionIndex)) return
        _quizSession.update {
            it.copy(selectedAnswers = it.selectedAnswers + (questionIndex to optionIndex))
        }
    }

    fun nextQuestionOrFinish() {
        val current = _quizSession.value
        val quiz = (current.quizState as? UiState.Success)?.data ?: return
        val lastIndex = quiz.questions.lastIndex
        if (current.currentQuestionIndex < lastIndex) {
            _quizSession.update { it.copy(currentQuestionIndex = it.currentQuestionIndex + 1) }
        } else {
            val answersList = quiz.questions.indices.map { idx -> current.selectedAnswers[idx] ?: -1 }
            val score = quiz.questions.indices.count { idx ->
                current.selectedAnswers[idx] == quiz.questions[idx].correctIndex
            }
            _quizSession.update { it.copy(phase = QuizPhase.RESULT) }
            if (!current.usingOfflineBank) {
                viewModelScope.launch {
                    quizRepository.saveTodayAttempt(
                        score = score,
                        totalQuestions = quiz.questions.size,
                        selectedAnswers = answersList
                    ).onSuccess {
                        quizRepository.getTodayAttempt().onSuccess { updatedAttempt ->
                            _quizSession.update { it.copy(todayAttempt = updatedAttempt) }
                        }
                    }
                }
            }
        }
    }

    fun openQuizReview() {
        val current = _quizSession.value
        val attempt = current.todayAttempt
        // If reviewing from INTRO screen using saved today's attempt, populate selectedAnswers
        if (current.selectedAnswers.isEmpty() && attempt != null && attempt.selectedAnswers.isNotEmpty()) {
            val restored = attempt.selectedAnswers.mapIndexed { idx, ans -> idx to ans.toInt() }.toMap()
            _quizSession.update {
                it.copy(selectedAnswers = restored, phase = QuizPhase.REVIEW)
            }
        } else {
            _quizSession.update { it.copy(phase = QuizPhase.REVIEW) }
        }
    }

    fun backToQuizIntroOrResult() {
        _quizSession.update { current ->
            val target = if (current.selectedAnswers.isNotEmpty()) QuizPhase.RESULT else QuizPhase.INTRO
            current.copy(phase = target)
        }
    }

    fun todayDateFormatted(): String = quizRepository.todayDateKey()

    // --- MULTI-TURN GEMINI AI TUTOR CHAT ACTIONS ---
    fun setChatFocusSubject(subject: Subject?) {
        _aiTutorState.update { it.copy(focusSubject = subject) }
    }

    fun sendChatMessage(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty() || _aiTutorState.value.isSending) return

        val snapshotState = _aiTutorState.value
        val committedHistory = snapshotState.messages
        val focusSubject = snapshotState.focusSubject
        val studentClass = (studentProfileState.value as? UiState.Success)?.data?.studentClass

        val userMsg = ChatMessage(
            id = "user_${UUID.randomUUID()}",
            role = ChatRole.USER,
            text = trimmed
        )
        val assistantMsgId = "model_${UUID.randomUUID()}"
        val placeholderMsg = ChatMessage(
            id = assistantMsgId,
            role = ChatRole.MODEL,
            text = "",
            isStreaming = true
        )

        _aiTutorState.update { current ->
            current.copy(
                messages = current.messages + userMsg + placeholderMsg,
                isSending = true,
                lastFailedPrompt = null
            )
        }

        activeChatJob?.cancel()
        var launchedJob: Job? = null
        launchedJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val finalText = aiTutorRepository.generateReplyStream(
                    committedHistory = committedHistory,
                    userMessage = trimmed,
                    focusSubject = focusSubject,
                    studentClass = studentClass,
                    onChunk = { chunk ->
                        accumulated.append(chunk)
                        val currentText = accumulated.toString()
                        _aiTutorState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == assistantMsgId) {
                                        msg.copy(text = currentText, isStreaming = true)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        }
                    }
                )
                _aiTutorState.update { state ->
                    state.copy(
                        isSending = false,
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(text = finalText, isStreaming = false, isError = false)
                            } else {
                                msg
                            }
                        }
                    )
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(VM_TAG, "AI Tutor response timed out", e)
                _aiTutorState.update { state ->
                    state.copy(
                        isSending = false,
                        lastFailedPrompt = trimmed,
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(
                                    text = "Gemini took longer than 60 seconds to respond. Please tap Retry.",
                                    isStreaming = false,
                                    isError = true
                                )
                            } else {
                                msg
                            }
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "AI Tutor generation failed", e)
                val errText = e.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: "Couldn't reach Gemini right now. Please check your connection and tap Retry."
                _aiTutorState.update { state ->
                    state.copy(
                        isSending = false,
                        lastFailedPrompt = trimmed,
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(
                                    text = errText,
                                    isStreaming = false,
                                    isError = true
                                )
                            } else {
                                msg
                            }
                        }
                    )
                }
            } finally {
                if (activeChatJob === launchedJob) {
                    _aiTutorState.update { state ->
                        if (state.isSending || state.messages.any { it.id == assistantMsgId && it.isStreaming }) {
                            state.copy(
                                isSending = false,
                                messages = state.messages.map { msg ->
                                    if (msg.id == assistantMsgId && msg.isStreaming) {
                                        msg.copy(isStreaming = false)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
        activeChatJob = launchedJob
    }

    fun retryLastFailedChatMessage() {
        if (_aiTutorState.value.isSending) return
        val prompt = _aiTutorState.value.lastFailedPrompt ?: return
        _aiTutorState.update { state ->
            // Drop trailing error bubble and duplicate user bubble before resending cleanly
            val trimmedMessages = state.messages.dropLastWhile { it.isError }.let { list ->
                if (list.lastOrNull()?.role == ChatRole.USER && list.lastOrNull()?.text == prompt) {
                    list.dropLast(1)
                } else {
                    list
                }
            }
            state.copy(messages = trimmedMessages, lastFailedPrompt = null)
        }
        sendChatMessage(prompt)
    }

    fun stopChatGeneration() {
        if (!_aiTutorState.value.isSending) return
        activeChatJob?.cancel()
        activeChatJob = null
        _aiTutorState.update { state ->
            val updated = state.messages.mapNotNull { msg ->
                if (!msg.isStreaming) {
                    msg
                } else if (msg.text.isNotBlank()) {
                    msg.copy(isStreaming = false)
                } else {
                    null
                }
            }
            state.copy(
                messages = updated,
                isSending = false
            )
        }
    }

    fun clearChatHistory() {
        activeChatJob?.cancel()
        activeChatJob = null
        _aiTutorState.update { current ->
            AiTutorUiState(
                messages = listOf(createWelcomeMessage()),
                focusSubject = current.focusSubject,
                isSending = false,
                lastFailedPrompt = null
            )
        }
    }
}

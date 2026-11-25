package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyQuizRepository
import com.example.data.DocumentRepository
import com.example.model.DailyQuiz
import com.example.model.FALLBACK_GK_QUESTIONS
import com.example.model.PickedFileInfo
import com.example.model.PreviewPayload
import com.example.model.QuizAttempt
import com.example.model.SortOrder
import com.example.model.StudyDocument
import com.example.model.Subject
import com.example.model.SubjectTab
import com.example.model.UPLOAD_PASSWORD
import com.example.model.UiState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class DarbarViewModel(
    private val documentRepository: DocumentRepository,
    private val quizRepository: DailyQuizRepository,
    val currentUserId: String,
    val currentUserName: String
) : ViewModel() {

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

    // --- DAILY QUIZ STATE ---
    private val _quizSession = MutableStateFlow(QuizSessionState())
    val quizSession: StateFlow<QuizSessionState> = _quizSession.asStateFlow()

    init {
        loadTodayQuizAttemptOnly()
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
                uploaderName = currentUserName,
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
            val result = documentRepository.downloadDocumentBytes(document.id)
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = { bytes ->
                    _previewPayload.value = PreviewPayload(document = document, bytes = bytes)
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

    fun fetchDocumentForDownload(
        document: StudyDocument,
        onBytesReady: (StudyDocument, ByteArray) -> Unit
    ) {
        viewModelScope.launch {
            _loadingDocumentId.value = document.id
            val result = documentRepository.downloadDocumentBytes(document.id)
            _loadingDocumentId.value = null
            result.fold(
                onSuccess = { bytes ->
                    onBytesReady(document, bytes)
                },
                onFailure = { err ->
                    _actionMessage.value = err.localizedMessage ?: "Download failed. Please try again."
                }
            )
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
}

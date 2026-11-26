package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DailyQuizRepository
import com.example.data.DocumentRepository
import com.example.data.OfflineDownloadRepository
import com.example.data.StudentProfileRepository
import com.example.data.ThemePreferenceStore
import com.example.data.authStateFlow
import com.example.data.configureAppCheck
import com.example.data.signOutUser
import com.example.model.Subject
import com.example.model.SubjectProgress
import com.example.model.SubjectTab
import com.example.model.UiState
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DailyQuizScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StudentProfileSetupScreen
import com.example.ui.screens.SubjectScreen
import com.example.ui.theme.DarbarTheme
import com.example.viewmodel.DarbarViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface DarbarRoute {
    data object Home : DarbarRoute
    data class SubjectDetail(val subject: Subject) : DarbarRoute
    data object DailyQuiz : DarbarRoute
    data object Downloads : DarbarRoute
    data object Account : DarbarRoute
    data object AiTutor : DarbarRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent BEFORE UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val themeStore = ThemePreferenceStore(applicationContext)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme by themeStore.observeDarkMode(systemDark)
                .collectAsStateWithLifecycle(initialValue = systemDark)
            val scope = rememberCoroutineScope()

            DarbarTheme(darkTheme = isDarkTheme) {
                DarbarRootApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        scope.launch { themeStore.setDarkMode(!isDarkTheme) }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
private fun DarbarRootApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val reducedMotion = LocalReducedMotion.current
    val fadeMs = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion)

    AnimatedContent(
        targetState = currentUser?.uid,
        transitionSpec = {
            fadeIn(tween(fadeMs, easing = DarbarMotion.EaseOut)) togetherWith
                fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut))
        },
        label = "auth_root_transition"
    ) { uid ->
        if (uid == null) {
            AuthScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ }
            )
        } else {
            // Key composition & ViewModel strictly by authenticated UID so account switches tear down cleanly
            key(uid) {
                val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Student"
                val userEmail = currentUser?.email.orEmpty()
                DarbarAuthenticatedShell(
                    currentUserId = uid,
                    currentUserName = displayName,
                    currentUserEmail = userEmail,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }
        }
    }
}

@Composable
private fun DarbarAuthenticatedShell(
    currentUserId: String,
    currentUserName: String,
    currentUserEmail: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DarbarViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                // Always resolve the provisioned named database ID from resources
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DarbarViewModel(
                    documentRepository = DocumentRepository(db),
                    quizRepository = DailyQuizRepository(db),
                    offlineDownloadRepository = OfflineDownloadRepository(app.applicationContext),
                    studentProfileRepository = StudentProfileRepository(db),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName
                )
            }
        }
    )

    var activeRoute by remember { mutableStateOf<DarbarRoute>(DarbarRoute.Home) }
    var aiTutorReturnRoute by remember { mutableStateOf<DarbarRoute>(DarbarRoute.Home) }
    var downloadsReturnRoute by remember { mutableStateOf<DarbarRoute>(DarbarRoute.Home) }

    val studentProfileState by viewModel.studentProfileState.collectAsStateWithLifecycle()
    val effectiveStudentName by viewModel.effectiveStudentName.collectAsStateWithLifecycle()
    val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
    val profileError by viewModel.profileError.collectAsStateWithLifecycle()
    val isUploadUnlocked by viewModel.isUploadUnlocked.collectAsStateWithLifecycle()
    val activeSubjectTab by viewModel.activeSubjectTab.collectAsStateWithLifecycle()
    val searchQueries by viewModel.searchQueries.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val documentsState by viewModel.filteredSubjectDocuments.collectAsStateWithLifecycle()
    val subjectProgressMap by viewModel.subjectProgressMap.collectAsStateWithLifecycle()
    val reviewedDocumentIds by viewModel.reviewedDocumentIds.collectAsStateWithLifecycle()
    val offlineDownloads by viewModel.offlineDownloads.collectAsStateWithLifecycle()
    val downloadedDocumentIds by viewModel.downloadedDocumentIds.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val loadingDocumentId by viewModel.loadingDocumentId.collectAsStateWithLifecycle()
    val previewPayload by viewModel.previewPayload.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()
    val aiTutorState by viewModel.aiTutorState.collectAsStateWithLifecycle()

    // Gate first-time students so they enter their Name & Class once before entering the dashboard
    when (val profileUi = studentProfileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        is UiState.Success -> {
            if (profileUi.data == null) {
                StudentProfileSetupScreen(
                    suggestedName = currentUserName,
                    isSaving = isSavingProfile,
                    errorMessage = profileError,
                    onSaveProfile = { name, studentClass ->
                        viewModel.saveStudentProfile(name, studentClass)
                    },
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    }
                )
                return
            }
        }

        is UiState.Error -> {
            // Proceed gracefully with fallback name if profile fetch errors out
        }
    }

    val savedProfile = (studentProfileState as? UiState.Success)?.data

    val slidePx = remember(density, reducedMotion) {
        if (reducedMotion) 0 else with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()
    }
    val pageDuration = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion)
    val exitDuration = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)

    AnimatedContent(
        targetState = activeRoute,
        transitionSpec = {
            val forward = when {
                targetState is DarbarRoute.Home -> false
                initialState is DarbarRoute.AiTutor && targetState is DarbarRoute.SubjectDetail -> false
                initialState is DarbarRoute.Downloads && targetState is DarbarRoute.SubjectDetail -> false
                initialState is DarbarRoute.Downloads && targetState is DarbarRoute.Account -> false
                else -> true
            }
            val dir = if (forward) 1 else -1
            val enter = fadeIn(tween(pageDuration, easing = DarbarMotion.EaseOut)) +
                slideInHorizontally(tween(pageDuration, easing = DarbarMotion.EaseOut)) { dir * slidePx }
            val exit = fadeOut(tween(exitDuration, easing = DarbarMotion.EaseOut)) +
                slideOutHorizontally(tween(exitDuration, easing = DarbarMotion.EaseOut)) { -dir * slidePx }
            enter togetherWith exit
        },
        label = "darbar_page_transition"
    ) { route ->
        when (route) {
            is DarbarRoute.Home -> {
                HomeScreen(
                    userName = effectiveStudentName,
                    studentClass = savedProfile?.studentClass,
                    isDarkTheme = isDarkTheme,
                    isUploadUnlocked = isUploadUnlocked,
                    todayQuizAttempt = quizSession.todayAttempt,
                    todayDateKey = viewModel.todayDateFormatted(),
                    subjectProgressMap = subjectProgressMap,
                    offlineDownloadCount = offlineDownloads.size,
                    offlineTotalBytes = offlineDownloads.sumOf { it.fileSizeBytes },
                    onToggleTheme = onToggleTheme,
                    onOpenAccount = {
                        activeRoute = DarbarRoute.Account
                    },
                    onSelectSubject = { subject ->
                        viewModel.openSubject(subject)
                        activeRoute = DarbarRoute.SubjectDetail(subject)
                    },
                    onOpenDailyQuiz = {
                        activeRoute = DarbarRoute.DailyQuiz
                    },
                    onOpenDownloads = {
                        downloadsReturnRoute = DarbarRoute.Home
                        activeRoute = DarbarRoute.Downloads
                    },
                    onOpenAiTutor = {
                        aiTutorReturnRoute = DarbarRoute.Home
                        viewModel.setChatFocusSubject(null)
                        activeRoute = DarbarRoute.AiTutor
                    }
                )
            }

            is DarbarRoute.SubjectDetail -> {
                val subject = route.subject
                SubjectScreen(
                    subject = subject,
                    currentUserId = currentUserId,
                    activeTab = activeSubjectTab,
                    isUploadUnlocked = isUploadUnlocked,
                    searchQuery = searchQueries[subject.id].orEmpty(),
                    sortOrder = sortOrder,
                    documentsState = documentsState,
                    subjectProgress = subjectProgressMap[subject] ?: SubjectProgress(subject),
                    reviewedDocumentIds = reviewedDocumentIds,
                    downloadedDocumentIds = downloadedDocumentIds,
                    uploadStatus = uploadStatus,
                    loadingDocumentId = loadingDocumentId,
                    previewPayload = previewPayload,
                    actionMessage = actionMessage,
                    onNavigateBack = { activeRoute = DarbarRoute.Home },
                    onSelectViewTab = { viewModel.selectSubjectTab(SubjectTab.VIEW) },
                    onSelectUploadTab = { viewModel.selectSubjectTab(SubjectTab.UPLOAD) },
                    onVerifyUploadPassword = { rawInput -> viewModel.verifyUploadPassword(rawInput) },
                    onLockUploads = { viewModel.lockUploads() },
                    onSearchQueryChange = { q -> viewModel.updateSearchQuery(subject, q) },
                    onSortOrderChange = { order -> viewModel.updateSortOrder(order) },
                    onUploadDocument = { title, desc, file ->
                        viewModel.uploadStudyDocument(
                            context = context,
                            subject = subject,
                            title = title,
                            description = desc,
                            pickedFile = file
                        )
                    },
                    onResetUploadStatus = { viewModel.resetUploadStatus() },
                    onPreviewDocument = { doc -> viewModel.openDocumentPreview(doc) },
                    onDismissPreview = { viewModel.dismissPreview() },
                    onSaveOfflineDocument = { doc -> viewModel.downloadDocumentForOffline(doc) },
                    onDownloadDocument = { doc, onReady ->
                        viewModel.fetchDocumentForDownload(doc, onReady)
                    },
                    onDeleteDocument = { doc -> viewModel.deleteDocument(doc) },
                    onToggleReviewed = { doc -> viewModel.toggleMaterialReviewed(doc) },
                    onOpenDownloads = {
                        downloadsReturnRoute = route
                        activeRoute = DarbarRoute.Downloads
                    },
                    onAskAiTutor = {
                        aiTutorReturnRoute = route
                        viewModel.setChatFocusSubject(subject)
                        activeRoute = DarbarRoute.AiTutor
                    },
                    onPostMessage = { msg -> viewModel.postActionMessage(msg) },
                    onClearMessage = { viewModel.clearActionMessage() }
                )
            }

            is DarbarRoute.DailyQuiz -> {
                DailyQuizScreen(
                    session = quizSession,
                    todayDateKey = viewModel.todayDateFormatted(),
                    onLoadQuiz = { force -> viewModel.loadDailyQuiz(force) },
                    onStartOfflineBank = { viewModel.startOfflinePracticeQuiz() },
                    onStartQuiz = { viewModel.startQuiz() },
                    onSelectAnswer = { qIdx, optIdx -> viewModel.selectQuizAnswer(qIdx, optIdx) },
                    onNextOrFinish = { viewModel.nextQuestionOrFinish() },
                    onOpenReview = { viewModel.openQuizReview() },
                    onBackFromReview = { viewModel.backToQuizIntroOrResult() },
                    onNavigateBack = { activeRoute = DarbarRoute.Home }
                )
            }

            is DarbarRoute.Downloads -> {
                DownloadsScreen(
                    downloads = offlineDownloads,
                    loadingDocumentId = loadingDocumentId,
                    previewPayload = previewPayload,
                    actionMessage = actionMessage,
                    onPreviewOffline = { offlineDoc -> viewModel.openOfflinePreview(offlineDoc) },
                    onDismissPreview = { viewModel.dismissPreview() },
                    onGetOfflineFile = { offlineDoc -> viewModel.getOfflineFile(offlineDoc) },
                    onReadOfflineBytes = { offlineDoc, onReady ->
                        viewModel.readOfflineBytesForExport(offlineDoc, onReady)
                    },
                    onRemoveOffline = { offlineDoc -> viewModel.removeOfflineDownload(offlineDoc) },
                    onPostMessage = { msg -> viewModel.postActionMessage(msg) },
                    onClearMessage = { viewModel.clearActionMessage() },
                    onNavigateBack = { activeRoute = downloadsReturnRoute }
                )
            }

            is DarbarRoute.Account -> {
                AccountScreen(
                    studentProfile = savedProfile,
                    fallbackName = currentUserName,
                    userEmail = currentUserEmail,
                    isDarkTheme = isDarkTheme,
                    isUploadUnlocked = isUploadUnlocked,
                    isSavingProfile = isSavingProfile,
                    profileError = profileError,
                    subjectProgressMap = subjectProgressMap,
                    offlineDownloadCount = offlineDownloads.size,
                    offlineTotalBytes = offlineDownloads.sumOf { it.fileSizeBytes },
                    todayQuizAttempt = quizSession.todayAttempt,
                    actionMessage = actionMessage,
                    onSaveProfile = { name, studentClass, onDone ->
                        viewModel.saveStudentProfile(name, studentClass, onSaved = onDone)
                    },
                    onToggleTheme = onToggleTheme,
                    onLockUploads = { viewModel.lockUploads() },
                    onOpenDownloads = {
                        downloadsReturnRoute = DarbarRoute.Account
                        activeRoute = DarbarRoute.Downloads
                    },
                    onOpenDailyQuiz = {
                        activeRoute = DarbarRoute.DailyQuiz
                    },
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    },
                    onClearMessage = { viewModel.clearActionMessage() },
                    onNavigateBack = { activeRoute = DarbarRoute.Home }
                )
            }

            is DarbarRoute.AiTutor -> {
                AiTutorScreen(
                    chatState = aiTutorState,
                    onSelectFocusSubject = { subject -> viewModel.setChatFocusSubject(subject) },
                    onSendMessage = { text -> viewModel.sendChatMessage(text) },
                    onStopGenerating = { viewModel.stopChatGeneration() },
                    onRetryLastFailed = { viewModel.retryLastFailedChatMessage() },
                    onClearChat = { viewModel.clearChatHistory() },
                    onNavigateBack = { activeRoute = aiTutorReturnRoute }
                )
            }
        }
    }
}

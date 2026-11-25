package com.example

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DailyQuizRepository
import com.example.data.DocumentRepository
import com.example.data.ThemePreferenceStore
import com.example.data.authStateFlow
import com.example.data.configureAppCheck
import com.example.data.signOutUser
import com.example.model.Subject
import com.example.model.SubjectTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DailyQuizScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SubjectScreen
import com.example.ui.theme.DarbarTheme
import com.example.viewmodel.DarbarViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private sealed interface DarbarRoute {
    data object Home : DarbarRoute
    data class SubjectDetail(val subject: Subject) : DarbarRoute
    data object DailyQuiz : DarbarRoute
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
}

@Composable
private fun DarbarRootApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ }
        )
    } else {
        // Key composition & ViewModel strictly by authenticated UID so account switches tear down cleanly
        key(user.uid) {
            val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
            DarbarAuthenticatedShell(
                currentUserId = user.uid,
                currentUserName = displayName,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@Composable
private fun DarbarAuthenticatedShell(
    currentUserId: String,
    currentUserName: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
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
                    currentUserId = currentUserId,
                    currentUserName = currentUserName
                )
            }
        }
    )

    var activeRoute by remember { mutableStateOf<DarbarRoute>(DarbarRoute.Home) }

    val isUploadUnlocked by viewModel.isUploadUnlocked.collectAsStateWithLifecycle()
    val activeSubjectTab by viewModel.activeSubjectTab.collectAsStateWithLifecycle()
    val searchQueries by viewModel.searchQueries.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val documentsState by viewModel.filteredSubjectDocuments.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val loadingDocumentId by viewModel.loadingDocumentId.collectAsStateWithLifecycle()
    val previewPayload by viewModel.previewPayload.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = activeRoute,
        transitionSpec = {
            val forward = targetState !is DarbarRoute.Home
            val dir = if (forward) 1 else -1
            (fadeIn(tween(240)) + slideInHorizontally(tween(280)) { width ->
                dir * (width / 7)
            }) togetherWith (fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { width ->
                -dir * (width / 7)
            })
        },
        label = "darbar_page_transition"
    ) { route ->
        when (route) {
            is DarbarRoute.Home -> {
                HomeScreen(
                    userName = currentUserName,
                    isDarkTheme = isDarkTheme,
                    isUploadUnlocked = isUploadUnlocked,
                    todayQuizAttempt = quizSession.todayAttempt,
                    todayDateKey = viewModel.todayDateFormatted(),
                    onToggleTheme = onToggleTheme,
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    },
                    onSelectSubject = { subject ->
                        viewModel.openSubject(subject)
                        activeRoute = DarbarRoute.SubjectDetail(subject)
                    },
                    onOpenDailyQuiz = {
                        activeRoute = DarbarRoute.DailyQuiz
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
                    onDownloadDocument = { doc, onReady ->
                        viewModel.fetchDocumentForDownload(doc, onReady)
                    },
                    onDeleteDocument = { doc -> viewModel.deleteDocument(doc) },
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
        }
    }
}

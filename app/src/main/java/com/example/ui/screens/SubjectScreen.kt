package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.DocumentRepository
import com.example.model.ALLOWED_FILE_EXTENSIONS
import com.example.model.MAX_UPLOAD_BYTES
import com.example.model.PickedFileInfo
import com.example.model.PreviewPayload
import com.example.model.SortOrder
import com.example.model.StudyDocument
import com.example.model.Subject
import com.example.model.SubjectProgress
import com.example.model.SubjectTab
import com.example.model.UiState
import com.example.ui.components.DarbarFooter
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionIconButton
import com.example.ui.components.ShimmerDocumentSkeleton
import com.example.ui.components.StaggeredFadeIn
import com.example.ui.components.SubjectSlidingTabBar
import com.example.ui.components.interactiveButtonMotion
import com.example.ui.components.interactiveCardMotion
import com.example.ui.theme.IconAccentPurple
import com.example.ui.theme.IconActionBlue
import com.example.ui.theme.IconAiViolet
import com.example.ui.theme.IconBrandIndigo
import com.example.ui.theme.IconDangerRose
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconProfileTeal
import com.example.ui.theme.IconSuccessEmerald
import com.example.ui.theme.IconUploadOrange
import com.example.ui.theme.IconWarningAmber
import com.example.viewmodel.UploadStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    subject: Subject,
    currentUserId: String,
    activeTab: SubjectTab,
    isUploadUnlocked: Boolean,
    searchQuery: String,
    sortOrder: SortOrder,
    documentsState: UiState<List<StudyDocument>>,
    subjectProgress: SubjectProgress,
    reviewedDocumentIds: Set<String>,
    downloadedDocumentIds: Set<String>,
    uploadStatus: UploadStatus,
    loadingDocumentId: String?,
    previewPayload: PreviewPayload?,
    actionMessage: String?,
    onNavigateBack: () -> Unit,
    onSelectViewTab: () -> Unit,
    onSelectUploadTab: () -> Unit,
    onVerifyUploadPassword: (String) -> Boolean,
    onLockUploads: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onUploadDocument: (title: String, description: String, file: PickedFileInfo) -> Unit,
    onResetUploadStatus: () -> Unit,
    onPreviewDocument: (StudyDocument) -> Unit,
    onDismissPreview: () -> Unit,
    onSaveOfflineDocument: (StudyDocument) -> Unit,
    onDownloadDocument: (StudyDocument, onReady: (StudyDocument, ByteArray) -> Unit) -> Unit,
    onDeleteDocument: (StudyDocument) -> Unit,
    onToggleReviewed: (StudyDocument) -> Unit,
    onOpenDownloads: () -> Unit,
    onAskAiTutor: () -> Unit,
    onPostMessage: (String) -> Unit,
    onClearMessage: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPasswordModal by remember { mutableStateOf(false) }
    var docPendingDelete by remember { mutableStateOf<StudyDocument?>(null) }

    // Pending download bytes waiting for SAF CreateDocument URI
    var pendingSavePayload by remember { mutableStateOf<Pair<StudyDocument, ByteArray>?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val payload = pendingSavePayload
        pendingSavePayload = null
        if (uri != null && payload != null) {
            scope.launch {
                val saveRes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(payload.second)
                            out.flush()
                        } ?: throw IllegalStateException("Could not open destination file.")
                    }
                }
                saveRes.fold(
                    onSuccess = { onPostMessage("Saved \"${payload.first.fileName}\" successfully.") },
                    onFailure = { err -> onPostMessage(err.localizedMessage ?: "Failed to save file.") }
                )
            }
        }
    }

    fun triggerSaveOrOpen(doc: StudyDocument, bytes: ByteArray) {
        pendingSavePayload = doc to bytes
        try {
            saveFileLauncher.launch(doc.fileName)
        } catch (_: Exception) {
            // Fallback to FileProvider share/open intent if CreateDocument picker is unavailable
            scope.launch {
                val openRes = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(context.cacheDir, "darbar_docs").apply { mkdirs() }
                        val outFile = File(dir, doc.fileName)
                        outFile.writeBytes(bytes)
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            outFile
                        )
                    }
                }
                openRes.onSuccess { contentUri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, doc.mimeType.ifBlank { "*/*" })
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, "Open ${doc.fileName}"))
                    }.onFailure {
                        onPostMessage("Downloaded to app cache: ${doc.fileName}")
                    }
                }.onFailure { err ->
                    onPostMessage(err.localizedMessage ?: "Download failed.")
                }
            }
        }
    }

    LaunchedEffect(actionMessage) {
        if (!actionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMessage)
            onClearMessage()
        }
    }

    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    MotionIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("subject_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd),
                            tint = IconNavCyan
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = subject.icon,
                                contentDescription = null,
                                tint = subject.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = subject.titleEn,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${subject.titleBn} • দরবার",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    MotionIconButton(
                        onClick = onOpenDownloads,
                        modifier = Modifier.testTag("subject_downloads_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = stringResource(R.string.downloads_open_cd),
                            tint = IconSuccessEmerald
                        )
                    }
                    MotionIconButton(
                        onClick = onAskAiTutor,
                        modifier = Modifier.testTag("subject_ask_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = stringResource(R.string.ask_ai_tutor_cd),
                            tint = IconAiViolet
                        )
                    }
                    val fastMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                    AnimatedVisibility(
                        visible = isUploadUnlocked,
                        enter = fadeIn(tween(fastMs, easing = DarbarMotion.EaseOut)) +
                            scaleIn(initialScale = if (reducedMotion) 1f else 0.9f, animationSpec = tween(fastMs, easing = DarbarMotion.EaseOut)),
                        exit = fadeOut(tween(fastMs, easing = DarbarMotion.EaseOut)) +
                            scaleOut(targetScale = if (reducedMotion) 1f else 0.9f, animationSpec = tween(fastMs, easing = DarbarMotion.EaseOut))
                    ) {
                        val lockInteraction = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = onLockUploads,
                            interactionSource = lockInteraction,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .interactiveButtonMotion(lockInteraction)
                                .testTag("lock_uploads_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = IconWarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.lock_uploads),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 880.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Separate VIEW and UPLOAD buttons with monochrome animated sliding indicator + colored icons
                SubjectSlidingTabBar(
                    activeTab = activeTab,
                    isUploadUnlocked = isUploadUnlocked,
                    accentColor = subject.accentColor,
                    onViewClick = onSelectViewTab,
                    onUploadClick = {
                        if (isUploadUnlocked) {
                            onSelectUploadTab()
                        } else {
                            showPasswordModal = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Smooth animated switch between VIEW and UPLOAD tabs
                AnimatedContent(
                    targetState = activeTab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val direction = if (targetState == SubjectTab.UPLOAD) 1 else -1
                        val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                        val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)
                        if (reducedMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            (fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) +
                                slideInHorizontally(tween(enterMs, easing = DarbarMotion.EaseOut)) {
                                    direction * slidePx
                                }) togetherWith (fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut)) +
                                slideOutHorizontally(tween(exitMs, easing = DarbarMotion.EaseOut)) {
                                    -direction * slidePx
                                })
                        }
                    },
                    label = "subject_tab_content"
                ) { tab ->
                    when (tab) {
                        SubjectTab.VIEW -> SubjectViewSection(
                            subject = subject,
                            currentUserId = currentUserId,
                            searchQuery = searchQuery,
                            sortOrder = sortOrder,
                            documentsState = documentsState,
                            subjectProgress = subjectProgress,
                            reviewedDocumentIds = reviewedDocumentIds,
                            downloadedDocumentIds = downloadedDocumentIds,
                            loadingDocumentId = loadingDocumentId,
                            onSearchQueryChange = onSearchQueryChange,
                            onSortOrderChange = onSortOrderChange,
                            onPreviewClick = onPreviewDocument,
                            onSaveOfflineClick = onSaveOfflineDocument,
                            onOpenDownloads = onOpenDownloads,
                            onExportCopyClick = { doc ->
                                onDownloadDocument(doc) { fetchedDoc, bytes ->
                                    triggerSaveOrOpen(fetchedDoc, bytes)
                                }
                            },
                            onDeleteClick = { doc -> docPendingDelete = doc },
                            onToggleReviewed = onToggleReviewed
                        )

                        SubjectTab.UPLOAD -> SubjectUploadSection(
                            subject = subject,
                            uploadStatus = uploadStatus,
                            onLockUploads = onLockUploads,
                            onUploadSubmit = onUploadDocument,
                            onResetStatus = onResetUploadStatus,
                            onGoToView = onSelectViewTab
                        )
                    }
                }
            }
        }
    }

    // Password Protection Modal before showing Upload form
    if (showPasswordModal) {
        UploadPasswordDialog(
            onDismiss = { showPasswordModal = false },
            onVerifyPassword = onVerifyUploadPassword
        )
    }

    // PDF & Image Full-Screen Preview Modal
    if (previewPayload != null) {
        DocumentPreviewDialog(
            payload = previewPayload,
            onDismiss = onDismissPreview,
            onDownloadClick = { doc, bytes ->
                onDismissPreview()
                triggerSaveOrOpen(doc, bytes)
            }
        )
    }

    // Delete Confirmation Dialog for uploader's own documents (scale + fade enter & exit)
    val deleteTarget = docPendingDelete
    if (deleteTarget != null) {
        AnimatedConfirmDialog(
            title = "Delete document?",
            message = "Remove \"${deleteTarget.title}\" permanently from ${subject.titleEn}? All students will no longer see it.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteDocument(deleteTarget)
                docPendingDelete = null
            },
            onDismiss = { docPendingDelete = null }
        )
    }
}

@Composable
private fun SubjectViewSection(
    subject: Subject,
    currentUserId: String,
    searchQuery: String,
    sortOrder: SortOrder,
    documentsState: UiState<List<StudyDocument>>,
    subjectProgress: SubjectProgress,
    reviewedDocumentIds: Set<String>,
    downloadedDocumentIds: Set<String>,
    loadingDocumentId: String?,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onPreviewClick: (StudyDocument) -> Unit,
    onSaveOfflineClick: (StudyDocument) -> Unit,
    onOpenDownloads: () -> Unit,
    onExportCopyClick: (StudyDocument) -> Unit,
    onDeleteClick: (StudyDocument) -> Unit,
    onToggleReviewed: (StudyDocument) -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedSubjectProgress by animateFloatAsState(
        targetValue = subjectProgress.fraction,
        animationSpec = tween(
            durationMillis = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion),
            easing = DarbarMotion.EaseOut
        ),
        label = "subject_banner_progress"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Per-Subject Visual Progress Banner (Monochrome + Colored Icon)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subject_progress_banner")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (subjectProgress.isComplete) {
                                Icons.Rounded.CheckCircle
                            } else {
                                subject.icon
                            },
                            contentDescription = null,
                            tint = if (subjectProgress.isComplete) IconSuccessEmerald else subject.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (subjectProgress.totalCount == 0) {
                                "No study materials uploaded yet"
                            } else {
                                "${subjectProgress.reviewedCount} of ${subjectProgress.totalCount} materials reviewed"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = if (subjectProgress.isComplete) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (subjectProgress.isComplete) {
                                stringResource(R.string.all_reviewed_label)
                            } else {
                                "${subjectProgress.percent}%"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (subjectProgress.isComplete) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { animatedSubjectProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dedicated Per-Subject Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_documents_placeholder, subject.titleEn),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search ${subject.titleEn} documents",
                    tint = IconActionBlue
                )
            },
            trailingIcon = {
                val fastMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(tween(fastMs, easing = DarbarMotion.EaseOut)) + scaleIn(initialScale = 0.85f, animationSpec = tween(fastMs, easing = DarbarMotion.EaseOut)),
                    exit = fadeOut(tween(fastMs, easing = DarbarMotion.EaseOut)) + scaleOut(targetScale = 0.85f, animationSpec = tween(fastMs, easing = DarbarMotion.EaseOut))
                ) {
                    MotionIconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = IconDangerRose
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subject_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sort Option Chips: Newest first, Oldest first, A to Z (Monochrome)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortOrder.entries.forEach { order ->
                val selected = sortOrder == order
                FilterChip(
                    selected = selected,
                    onClick = { onSortOrderChange(order) },
                    label = { Text(order.label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("sort_chip_${order.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Document List States with smooth shimmer-to-content fade transition
        AnimatedContent(
            targetState = documentsState,
            contentKey = { state ->
                when (state) {
                    is UiState.Loading -> "loading"
                    is UiState.Error -> "error"
                    is UiState.Success -> if (state.data.isEmpty()) "empty" else "list"
                }
            },
            transitionSpec = {
                val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                val exitMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) togetherWith
                    fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut))
            },
            modifier = Modifier.weight(1f),
            label = "documents_state_fade"
        ) { state ->
            when (state) {
                is UiState.Loading -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(4) {
                            ShimmerDocumentSkeleton()
                        }
                    }
                }

                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = IconDangerRose,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Could not load ${subject.titleEn} documents",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val docs = state.data
                    if (docs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (searchQuery.isNotBlank()) {
                                                Icons.Rounded.SearchOff
                                            } else {
                                                Icons.Rounded.FolderOpen
                                            },
                                            contentDescription = null,
                                            tint = if (searchQuery.isNotBlank()) IconActionBlue else IconUploadOrange,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "No matching notes found"
                                        } else {
                                            "No ${subject.titleEn} documents yet"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "Nothing in ${subject.titleEn} matched \"$searchQuery\". Try a different keyword."
                                        } else {
                                            "Study materials uploaded for ${subject.titleEn} will appear here immediately for all students."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        val clearBtnInteraction = remember { MutableInteractionSource() }
                                        FilledTonalButton(
                                            onClick = { onSearchQueryChange("") },
                                            interactionSource = clearBtnInteraction,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.interactiveButtonMotion(clearBtnInteraction)
                                        ) {
                                            Text("Clear search")
                                        }
                                    }
                                }
                            }
                            DarbarFooter()
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(sortOrder) {
                            if (docs.isNotEmpty()) {
                                if (reducedMotion) {
                                    listState.scrollToItem(0)
                                } else {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        }
                        val itemAnimMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                        val itemExitMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = docs,
                                key = { _, doc -> doc.id }
                            ) { index, doc ->
                                StaggeredFadeIn(
                                    index = index,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(itemAnimMs, easing = DarbarMotion.EaseOut),
                                        placementSpec = tween(itemAnimMs, easing = DarbarMotion.EaseOut),
                                        fadeOutSpec = tween(itemExitMs, easing = DarbarMotion.EaseOut)
                                    )
                                ) {
                                    StudyDocumentCard(
                                        document = doc,
                                        isReviewed = doc.id in reviewedDocumentIds,
                                        isDownloaded = doc.id in downloadedDocumentIds,
                                        isBusy = loadingDocumentId == doc.id,
                                        isOwner = doc.uploaderId == currentUserId,
                                        onToggleReviewed = { onToggleReviewed(doc) },
                                        onPreviewClick = { onPreviewClick(doc) },
                                        onSaveOfflineClick = { onSaveOfflineClick(doc) },
                                        onOpenDownloads = onOpenDownloads,
                                        onExportCopyClick = { onExportCopyClick(doc) },
                                        onDeleteClick = { onDeleteClick(doc) }
                                    )
                                }
                            }
                            item(key = "footer") {
                                DarbarFooter()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyDocumentCard(
    document: StudyDocument,
    isReviewed: Boolean,
    isDownloaded: Boolean,
    isBusy: Boolean,
    isOwner: Boolean,
    onToggleReviewed: () -> Unit,
    onPreviewClick: () -> Unit,
    onSaveOfflineClick: () -> Unit,
    onOpenDownloads: () -> Unit,
    onExportCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val fileVisual = remember(document.fileExtension) { fileTypeVisual(document.fileExtension) }
    val dateLabel = remember(document.createdAt, document.updatedAt) {
        val ts = document.createdAt ?: document.updatedAt ?: Timestamp.now()
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(ts.toDate())
    }
    val sizeLabel = remember(document.fileSizeBytes) {
        DocumentRepository.formatFileSize(document.fileSizeBytes)
    }

    val badgeBgColor by animateColorAsState(
        targetValue = if (isReviewed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut),
        label = "reviewed_pill_bg"
    )
    val badgeTextColor by animateColorAsState(
        targetValue = if (isReviewed) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut),
        label = "reviewed_pill_text"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isReviewed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut),
        label = "reviewed_card_border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveCardMotion(shape = RoundedCornerShape(20.dp))
            .testTag("document_card_${document.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, cardBorderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileVisual.icon,
                        contentDescription = fileVisual.badge,
                        tint = fileVisual.color,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = fileVisual.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "$sizeLabel • $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!document.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = document.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isOwner) {
                    MotionIconButton(
                        onClick = onDeleteClick,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("delete_doc_${document.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete document",
                            tint = IconDangerRose
                        )
                    }
                }
            }

            // Interactive Reviewed Status Pill + Saved Offline Pill (Monochrome + Colored Icons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reviewInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onToggleReviewed,
                    interactionSource = reviewInteraction,
                    shape = CircleShape,
                    color = badgeBgColor,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isReviewed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .interactiveButtonMotion(reviewInteraction)
                        .testTag("toggle_reviewed_${document.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isReviewed) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (isReviewed) IconSuccessEmerald else IconAccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isReviewed) {
                                stringResource(R.string.reviewed_label)
                            } else {
                                stringResource(R.string.mark_reviewed)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeTextColor
                        )
                    }
                }

                if (isDownloaded) {
                    val offlineChipInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onOpenDownloads,
                        interactionSource = offlineChipInteraction,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .interactiveButtonMotion(offlineChipInteraction)
                            .testTag("offline_badge_${document.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = null,
                                tint = IconSuccessEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.saved_offline_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (document.canPreview()) {
                    val previewInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onPreviewClick,
                        enabled = !isBusy,
                        interactionSource = previewInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .interactiveButtonMotion(previewInteraction, enabled = !isBusy)
                            .testTag("preview_doc_${document.id}")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = IconActionBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.preview_button))
                        }
                    }
                }

                val downloadInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        if (isDownloaded) {
                            onOpenDownloads()
                        } else {
                            onSaveOfflineClick()
                        }
                    },
                    enabled = !isBusy,
                    interactionSource = downloadInteraction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .interactiveButtonMotion(downloadInteraction, enabled = !isBusy)
                        .testTag("download_doc_${document.id}")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saving…")
                    } else {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = null,
                            tint = if (isDownloaded) IconSuccessEmerald else IconProfileTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDownloaded) {
                                stringResource(R.string.downloads_title)
                            } else {
                                stringResource(R.string.download_button)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                MotionIconButton(
                    onClick = onExportCopyClick,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("export_doc_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SaveAlt,
                        contentDescription = stringResource(R.string.save_copy_button),
                        tint = IconActionBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectUploadSection(
    subject: Subject,
    uploadStatus: UploadStatus,
    onLockUploads: () -> Unit,
    onUploadSubmit: (title: String, description: String, file: PickedFileInfo) -> Unit,
    onResetStatus: () -> Unit,
    onGoToView: () -> Unit
) {
    val context = LocalContext.current
    var pickedFile by remember(subject.id) { mutableStateOf<PickedFileInfo?>(null) }
    var fileValidationError by remember(subject.id) { mutableStateOf<String?>(null) }
    var title by remember(subject.id) { mutableStateOf("") }
    var description by remember(subject.id) { mutableStateOf("") }

    // Zero-permission Android Storage Access Framework document picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val inspected = DocumentRepository.inspectPickedFile(context, uri)
            if (inspected == null) {
                fileValidationError = "Could not inspect the selected file."
                return@rememberLauncherForActivityResult
            }
            if (inspected.extension.lowercase(Locale.US) !in ALLOWED_FILE_EXTENSIONS) {
                fileValidationError =
                    "Unsupported file type (.${inspected.extension}). Allowed: PDF, JPG, PNG, WEBP, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT."
                pickedFile = null
                return@rememberLauncherForActivityResult
            }
            if (inspected.sizeBytes > MAX_UPLOAD_BYTES) {
                fileValidationError =
                    "File is ${DocumentRepository.formatFileSize(inspected.sizeBytes)}. Maximum size is 8 MB."
                pickedFile = null
                return@rememberLauncherForActivityResult
            }
            fileValidationError = null
            pickedFile = inspected
            if (title.isBlank()) {
                title = inspected.fileName.substringBeforeLast('.').take(160)
            }
            if (uploadStatus is UploadStatus.Success || uploadStatus is UploadStatus.Error) {
                onResetStatus()
            }
        }
    }

    val isUploading = uploadStatus is UploadStatus.Uploading
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()
    val bannerEnterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
    val bannerExitMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Unlocked Session Banner (Monochrome)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Upload Unlocked (This Session)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Files uploaded here appear in ${subject.titleEn} for all students.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val sectionLockInteraction = remember { MutableInteractionSource() }
                TextButton(
                    onClick = onLockUploads,
                    interactionSource = sectionLockInteraction,
                    modifier = Modifier
                        .interactiveButtonMotion(sectionLockInteraction)
                        .testTag("upload_section_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = IconWarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.lock_uploads))
                }
            }
        }

        // Success Feedback Card (GPU transform + opacity only)
        AnimatedVisibility(
            visible = uploadStatus is UploadStatus.Success,
            enter = fadeIn(tween(bannerEnterMs, easing = DarbarMotion.EaseOut)) +
                slideInVertically(tween(bannerEnterMs, easing = DarbarMotion.EaseOut)) { if (reducedMotion) 0 else -slidePx },
            exit = fadeOut(tween(bannerExitMs, easing = DarbarMotion.EaseOut))
        ) {
            val uploadedTitle = (uploadStatus as? UploadStatus.Success)?.title.orEmpty()
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_success_banner")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = IconSuccessEmerald
                        )
                        Column {
                            Text(
                                text = "Uploaded successfully!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "\"$uploadedTitle\" is now live in ${subject.titleEn}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val viewDocsInteraction = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = onGoToView,
                            interactionSource = viewDocsInteraction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.interactiveButtonMotion(viewDocsInteraction)
                        ) {
                            Text("View Documents")
                        }
                        val anotherInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = {
                                pickedFile = null
                                title = ""
                                description = ""
                                onResetStatus()
                            },
                            interactionSource = anotherInteraction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.interactiveButtonMotion(anotherInteraction)
                        ) {
                            Text("Upload Another")
                        }
                    }
                }
            }
        }

        // File Picker Drop Zone Card (Monochrome + Colored Upload Icon)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pick_file_button")
                .interactiveCardMotion(
                    enabled = !isUploading,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    documentPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/jpeg",
                            "image/png",
                            "image/webp",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "text/plain",
                            "*/*"
                        )
                    )
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (pickedFile != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = "Choose study document",
                        tint = IconUploadOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (pickedFile == null) {
                    Text(
                        text = "Tap to choose a study file",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PDF, JPG, PNG, WEBP, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT (max 8 MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = pickedFile!!.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pickedFile!!.extension.uppercase(Locale.US)} • ${
                            DocumentRepository.formatFileSize(pickedFile!!.sizeBytes)
                        } • Tap to change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!fileValidationError.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = fileValidationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Title Field (Required)
        OutlinedTextField(
            value = title,
            onValueChange = { if (it.length <= 160) title = it },
            label = { Text("Document Title *") },
            placeholder = { Text("e.g., Chapter 4 Mechanics Formula Sheet") },
            singleLine = true,
            enabled = !isUploading,
            supportingText = { Text("${title.length}/160") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upload_title_input")
        )

        // Description Field (Optional)
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 500) description = it },
            label = { Text("Description (Optional)") },
            placeholder = { Text("Key chapters, exam year, or teacher notes…") },
            minLines = 3,
            maxLines = 5,
            enabled = !isUploading,
            supportingText = { Text("${description.length}/500") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upload_description_input")
        )

        // Upload Progress Bar (GPU transform + opacity only)
        AnimatedVisibility(
            visible = uploadStatus is UploadStatus.Uploading,
            enter = fadeIn(tween(bannerEnterMs, easing = DarbarMotion.EaseOut)) +
                slideInVertically(tween(bannerEnterMs, easing = DarbarMotion.EaseOut)) { if (reducedMotion) 0 else slidePx / 2 },
            exit = fadeOut(tween(bannerExitMs, easing = DarbarMotion.EaseOut))
        ) {
            val rawProgress = (uploadStatus as? UploadStatus.Uploading)?.progress ?: 0f
            val smoothProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut),
                label = "upload_progress"
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Uploading to cloud…",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${(rawProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { smoothProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // Upload Error Message
        AnimatedVisibility(
            visible = uploadStatus is UploadStatus.Error,
            enter = fadeIn(tween(bannerEnterMs, easing = DarbarMotion.EaseOut)),
            exit = fadeOut(tween(bannerExitMs, easing = DarbarMotion.EaseOut))
        ) {
            val errorMsg = (uploadStatus as? UploadStatus.Error)?.message.orEmpty()
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Submit Button (Monochrome + Colored Upload Icon)
        val submitEnabled = !isUploading && pickedFile != null && title.isNotBlank()
        val submitInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                val file = pickedFile
                if (file != null && title.isNotBlank()) {
                    onUploadSubmit(title, description, file)
                }
            },
            enabled = submitEnabled,
            interactionSource = submitInteraction,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .interactiveButtonMotion(submitInteraction, enabled = submitEnabled)
                .testTag("upload_submit_button")
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Uploading…")
            } else {
                Icon(
                    imageVector = Icons.Rounded.CloudUpload,
                    contentDescription = null,
                    tint = if (submitEnabled) IconUploadOrange else IconUploadOrange.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.upload_submit_button),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        DarbarFooter()
    }
}

private data class FileTypeVisual(
    val badge: String,
    val icon: ImageVector,
    val color: Color
)

private fun fileTypeVisual(extension: String): FileTypeVisual = when (extension.lowercase(Locale.US)) {
    "pdf" -> FileTypeVisual("PDF", Icons.Rounded.PictureAsPdf, IconDangerRose)
    "jpg", "jpeg", "png", "webp" -> FileTypeVisual(
        extension.uppercase(Locale.US),
        Icons.Rounded.Image,
        IconProfileTeal
    )
    "doc", "docx" -> FileTypeVisual("DOC", Icons.Rounded.Description, IconActionBlue)
    "ppt", "pptx" -> FileTypeVisual("PPT", Icons.Rounded.Slideshow, IconUploadOrange)
    "xls", "xlsx" -> FileTypeVisual("XLS", Icons.Rounded.TableChart, IconSuccessEmerald)
    "txt" -> FileTypeVisual("TXT", Icons.Rounded.Description, IconBrandIndigo)
    else -> FileTypeVisual("FILE", Icons.Rounded.Description, IconBrandIndigo)
}

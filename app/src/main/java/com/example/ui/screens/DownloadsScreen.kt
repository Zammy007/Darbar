package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.DocumentRepository
import com.example.model.OfflineDocument
import com.example.model.PreviewPayload
import com.example.model.StudyDocument
import com.example.model.Subject
import com.example.ui.components.DarbarFooter
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionIconButton
import com.example.ui.components.StaggeredFadeIn
import com.example.ui.components.interactiveButtonMotion
import com.example.ui.components.interactiveCardMotion
import com.example.ui.theme.IconActionBlue
import com.example.ui.theme.IconBrandIndigo
import com.example.ui.theme.IconDangerRose
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconProfileTeal
import com.example.ui.theme.IconSuccessEmerald
import com.example.ui.theme.IconUploadOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<OfflineDocument>,
    loadingDocumentId: String?,
    previewPayload: PreviewPayload?,
    actionMessage: String?,
    onPreviewOffline: (OfflineDocument) -> Unit,
    onDismissPreview: () -> Unit,
    onGetOfflineFile: (OfflineDocument) -> File?,
    onReadOfflineBytes: (OfflineDocument, onReady: (StudyDocument, ByteArray) -> Unit) -> Unit,
    onRemoveOffline: (OfflineDocument) -> Unit,
    onPostMessage: (String) -> Unit,
    onClearMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var docPendingRemove by remember { mutableStateOf<OfflineDocument?>(null) }
    var pendingSavePayload by remember { mutableStateOf<Pair<StudyDocument, ByteArray>?>(null) }

    val totalBytes = remember(downloads) { downloads.sumOf { it.fileSizeBytes } }
    val totalSizeLabel = remember(totalBytes) { DocumentRepository.formatFileSize(totalBytes) }

    val filteredDownloads = remember(downloads, searchQuery, selectedSubject) {
        val q = searchQuery.trim().lowercase(Locale.US)
        downloads.filter { doc ->
            val subjectMatch = selectedSubject == null || doc.subjectId == selectedSubject?.id
            val queryMatch = q.isEmpty() ||
                doc.title.lowercase(Locale.US).contains(q) ||
                doc.description.orEmpty().lowercase(Locale.US).contains(q) ||
                doc.fileName.lowercase(Locale.US).contains(q)
            subjectMatch && queryMatch
        }
    }

    val saveCopyLauncher = rememberLauncherForActivityResult(
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
                    onSuccess = { onPostMessage("Exported \"${payload.first.fileName}\" to device storage.") },
                    onFailure = { err -> onPostMessage(err.localizedMessage ?: "Failed to save copy.") }
                )
            }
        }
    }

    fun triggerExportCopy(doc: StudyDocument, bytes: ByteArray) {
        pendingSavePayload = doc to bytes
        try {
            saveCopyLauncher.launch(doc.fileName)
        } catch (_: Exception) {
            pendingSavePayload = null
            onPostMessage("File already saved in Darbar Downloads.")
        }
    }

    fun openOfflineExternally(offlineDoc: OfflineDocument) {
        val localFile = onGetOfflineFile(offlineDoc)
        if (localFile == null || !localFile.exists()) {
            onPostMessage("Offline file could not be found on this device.")
            return
        }
        runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, offlineDoc.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open ${offlineDoc.fileName}"))
        }.onFailure {
            // Fallback to exporting a copy via SAF if no compatible viewer app is installed
            onReadOfflineBytes(offlineDoc) { studyDoc, bytes ->
                triggerExportCopy(studyDoc, bytes)
            }
        }
    }

    LaunchedEffect(actionMessage) {
        if (!actionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMessage)
            onClearMessage()
        }
    }

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
                        modifier = Modifier.testTag("downloads_back_button")
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
                                imageVector = Icons.Rounded.DownloadDone,
                                contentDescription = null,
                                tint = IconSuccessEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.downloads_title),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Offline Library • দরবার",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = null,
                                tint = IconSuccessEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.offline_ready_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
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

                // Offline Storage Summary Banner (Monochrome)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("downloads_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (downloads.isEmpty()) {
                                    "Your Offline Study Locker"
                                } else {
                                    "${downloads.size} ${if (downloads.size == 1) "Document" else "Documents"} Saved"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.downloads_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = totalSizeLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (downloads.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Offline Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.downloads_search_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search offline downloads",
                                tint = IconActionBlue
                            )
                        },
                        trailingIcon = {
                            val fastMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                            AnimatedVisibility(
                                visible = searchQuery.isNotEmpty(),
                                enter = fadeIn(tween(fastMs, easing = DarbarMotion.EaseOut)) +
                                    scaleIn(initialScale = 0.85f, animationSpec = tween(fastMs, easing = DarbarMotion.EaseOut)),
                                exit = fadeOut(tween(fastMs, easing = DarbarMotion.EaseOut)) +
                                    scaleOut(targetScale = 0.85f, animationSpec = tween(fastMs, easing = DarbarMotion.EaseOut))
                            ) {
                                MotionIconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("downloads_clear_search_button")
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
                            .testTag("downloads_search_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subject Filter Chips (Monochrome + Colored Subject Icons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedSubject == null,
                            onClick = { selectedSubject = null },
                            label = {
                                Text(
                                    text = "All (${downloads.size})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("downloads_filter_all")
                        )

                        Subject.entries.forEach { subject ->
                            val count = downloads.count { it.subjectId == subject.id }
                            if (count > 0 || selectedSubject == subject) {
                                FilterChip(
                                    selected = selectedSubject == subject,
                                    onClick = {
                                        selectedSubject = if (selectedSubject == subject) null else subject
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = subject.icon,
                                            contentDescription = null,
                                            tint = subject.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${subject.titleEn} ($count)",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = subject.accentColor
                                    ),
                                    modifier = Modifier.testTag("downloads_filter_${subject.id}")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = when {
                        downloads.isEmpty() -> "none"
                        filteredDownloads.isEmpty() -> "no_match"
                        else -> "list"
                    },
                    transitionSpec = {
                        val ms = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                        fadeIn(tween(ms, easing = DarbarMotion.EaseOut)) togetherWith
                            fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut))
                    },
                    modifier = Modifier.weight(1f),
                    label = "downloads_content_state"
                ) { stateKey ->
                    when (stateKey) {
                        "none" -> {
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
                                        .testTag("downloads_empty_state")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FolderOff,
                                                contentDescription = null,
                                                tint = IconUploadOrange,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Text(
                                            text = stringResource(R.string.downloads_empty_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = stringResource(R.string.downloads_empty_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        val browseInteraction = remember { MutableInteractionSource() }
                                        Button(
                                            onClick = onNavigateBack,
                                            interactionSource = browseInteraction,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier
                                                .interactiveButtonMotion(browseInteraction)
                                                .testTag("downloads_browse_subjects_button")
                                        ) {
                                            Text(text = stringResource(R.string.downloads_browse_subjects))
                                        }
                                    }
                                }
                                DarbarFooter()
                            }
                        }

                        "no_match" -> {
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
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.SearchOff,
                                                contentDescription = null,
                                                tint = IconActionBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            text = "No matching offline files",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Try clearing your search or switching the subject filter.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        val clearInteraction = remember { MutableInteractionSource() }
                                        FilledTonalButton(
                                            onClick = {
                                                searchQuery = ""
                                                selectedSubject = null
                                            },
                                            interactionSource = clearInteraction,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.interactiveButtonMotion(clearInteraction)
                                        ) {
                                            Text("Reset filters")
                                        }
                                    }
                                }
                                DarbarFooter()
                            }
                        }

                        else -> {
                            val itemAnimMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                            val itemExitMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = filteredDownloads,
                                    key = { _, item -> item.documentId }
                                ) { index, offlineDoc ->
                                    StaggeredFadeIn(
                                        index = index,
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(itemAnimMs, easing = DarbarMotion.EaseOut),
                                            placementSpec = tween(itemAnimMs, easing = DarbarMotion.EaseOut),
                                            fadeOutSpec = tween(itemExitMs, easing = DarbarMotion.EaseOut)
                                        )
                                    ) {
                                        OfflineDocumentCard(
                                            document = offlineDoc,
                                            isBusy = loadingDocumentId == offlineDoc.documentId,
                                            onPreviewClick = { onPreviewOffline(offlineDoc) },
                                            onOpenClick = { openOfflineExternally(offlineDoc) },
                                            onExportClick = {
                                                onReadOfflineBytes(offlineDoc) { studyDoc, bytes ->
                                                    triggerExportCopy(studyDoc, bytes)
                                                }
                                            },
                                            onRemoveClick = { docPendingRemove = offlineDoc }
                                        )
                                    }
                                }
                                item(key = "downloads_footer") {
                                    DarbarFooter()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-screen in-app PDF / Image viewer running 100% offline
    if (previewPayload != null) {
        DocumentPreviewDialog(
            payload = previewPayload,
            onDismiss = onDismissPreview,
            onDownloadClick = { doc, bytes ->
                onDismissPreview()
                triggerExportCopy(doc, bytes)
            }
        )
    }

    // Confirmation dialog before deleting a local offline download
    val removeTarget = docPendingRemove
    if (removeTarget != null) {
        AnimatedConfirmDialog(
            title = "Remove offline download?",
            message = "Delete the local copy of \"${removeTarget.title}\" from this device? You can re-download it anytime when online.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                onRemoveOffline(removeTarget)
                docPendingRemove = null
            },
            onDismiss = { docPendingRemove = null }
        )
    }
}

@Composable
private fun OfflineDocumentCard(
    document: OfflineDocument,
    isBusy: Boolean,
    onPreviewClick: () -> Unit,
    onOpenClick: () -> Unit,
    onExportClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val subject = remember(document.subjectId) { Subject.fromId(document.subjectId) }
    val fileVisual = remember(document.fileExtension) { offlineFileVisual(document.fileExtension) }
    val sizeLabel = remember(document.fileSizeBytes) {
        DocumentRepository.formatFileSize(document.fileSizeBytes)
    }
    val dateLabel = remember(document.downloadedAtMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(document.downloadedAtMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveCardMotion(shape = RoundedCornerShape(20.dp))
            .testTag("offline_doc_card_${document.documentId}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = subject.titleEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OfflinePin,
                            contentDescription = null,
                            tint = IconSuccessEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$sizeLabel • Saved $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    MotionIconButton(
                        onClick = onExportClick,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("offline_export_${document.documentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SaveAlt,
                            contentDescription = stringResource(R.string.save_copy_button),
                            tint = IconActionBlue
                        )
                    }
                    MotionIconButton(
                        onClick = onRemoveClick,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("offline_remove_${document.documentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.remove_download_cd),
                            tint = IconDangerRose
                        )
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
                    Button(
                        onClick = onPreviewClick,
                        enabled = !isBusy,
                        interactionSource = previewInteraction,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .interactiveButtonMotion(previewInteraction, enabled = !isBusy)
                            .testTag("offline_preview_${document.documentId}")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
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
                            Text(text = stringResource(R.string.preview_button))
                        }
                    }
                }

                val openInteraction = remember { MutableInteractionSource() }
                if (document.canPreview()) {
                    OutlinedButton(
                        onClick = onOpenClick,
                        enabled = !isBusy,
                        interactionSource = openInteraction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .interactiveButtonMotion(openInteraction, enabled = !isBusy)
                            .testTag("offline_open_${document.documentId}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = IconNavCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.open_file_button))
                    }
                } else {
                    Button(
                        onClick = onOpenClick,
                        enabled = !isBusy,
                        interactionSource = openInteraction,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .interactiveButtonMotion(openInteraction, enabled = !isBusy)
                            .testTag("offline_open_${document.documentId}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = IconNavCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.open_file_button))
                    }
                }
            }
        }
    }
}

private data class OfflineFileVisual(
    val icon: ImageVector,
    val badge: String,
    val color: Color
)

private fun offlineFileVisual(ext: String): OfflineFileVisual {
    return when (ext.lowercase(Locale.US)) {
        "pdf" -> OfflineFileVisual(Icons.Rounded.PictureAsPdf, "PDF", IconDangerRose)
        "jpg", "jpeg", "png", "webp" -> OfflineFileVisual(Icons.Rounded.Image, ext.uppercase(Locale.US), IconProfileTeal)
        "doc", "docx" -> OfflineFileVisual(Icons.Rounded.Description, "DOC", IconActionBlue)
        "ppt", "pptx" -> OfflineFileVisual(Icons.Rounded.Slideshow, "PPT", IconUploadOrange)
        "xls", "xlsx" -> OfflineFileVisual(Icons.Rounded.TableChart, "XLS", IconSuccessEmerald)
        else -> OfflineFileVisual(Icons.Rounded.Description, ext.uppercase(Locale.US).ifBlank { "FILE" }, IconBrandIndigo)
    }
}

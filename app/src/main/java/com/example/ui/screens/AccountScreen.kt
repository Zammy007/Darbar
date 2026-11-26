package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DocumentRepository
import com.example.model.QuizAttempt
import com.example.model.STUDENT_CLASS_OPTIONS
import com.example.model.StudentProfile
import com.example.model.Subject
import com.example.model.SubjectProgress
import com.example.ui.components.DarbarFooter
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionIconButton
import com.example.ui.components.StaggeredFadeIn
import com.example.ui.components.interactiveButtonMotion
import com.example.ui.components.interactiveCardMotion
import com.example.ui.theme.IconActionBlue
import com.example.ui.theme.IconAiViolet
import com.example.ui.theme.IconBrandIndigo
import com.example.ui.theme.IconDangerRose
import com.example.ui.theme.IconMoonIndigo
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconProfileTeal
import com.example.ui.theme.IconSuccessEmerald
import com.example.ui.theme.IconWarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentProfileSetupScreen(
    suggestedName: String,
    isSaving: Boolean,
    errorMessage: String?,
    onSaveProfile: (name: String, studentClass: String) -> Unit,
    onSignOut: () -> Unit
) {
    var nameInput by remember(suggestedName) {
        mutableStateOf(
            suggestedName.takeIf { it.isNotBlank() && !it.equals("Student", ignoreCase = true) }.orEmpty()
        )
    }
    var classInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayError = localError ?: errorMessage
    val submitInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StaggeredFadeIn(index = 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = IconBrandIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.student_setup_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.student_setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            StaggeredFadeIn(index = 1) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_setup_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Student Name field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.student_name_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it.take(80)
                                    localError = null
                                },
                                placeholder = {
                                    Text(stringResource(R.string.student_name_placeholder))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = IconProfileTeal
                                    )
                                },
                                singleLine = true,
                                enabled = !isSaving,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_student_name_input")
                            )
                        }

                        // Student Class quick-select chips + custom input
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.student_class_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                STUDENT_CLASS_OPTIONS.forEach { option ->
                                    val selected = classInput.equals(option, ignoreCase = true)
                                    FilterChip(
                                        selected = selected,
                                        enabled = !isSaving,
                                        onClick = {
                                            classInput = option
                                            localError = null
                                        },
                                        label = {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("setup_class_chip_${option.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = classInput,
                                onValueChange = {
                                    classInput = it.take(40)
                                    localError = null
                                },
                                placeholder = {
                                    Text(stringResource(R.string.student_class_placeholder))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Class,
                                        contentDescription = null,
                                        tint = IconBrandIndigo
                                    )
                                },
                                singleLine = true,
                                enabled = !isSaving,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_student_class_input")
                            )
                        }

                        if (!displayError.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = displayError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cleanName = nameInput.trim()
                                val cleanClass = classInput.trim()
                                when {
                                    cleanName.isEmpty() -> localError = "Please enter your name."
                                    cleanClass.isEmpty() -> localError = "Please select or enter your class."
                                    else -> onSaveProfile(cleanName, cleanClass)
                                }
                            },
                            enabled = !isSaving,
                            interactionSource = submitInteraction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .interactiveButtonMotion(submitInteraction, enabled = !isSaving)
                                .testTag("student_setup_submit_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving…")
                            } else {
                                Text(
                                    text = stringResource(R.string.continue_to_darbar),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = IconNavCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onSignOut,
                enabled = !isSaving,
                modifier = Modifier.testTag("setup_sign_out_button")
            ) {
                Text(
                    text = "Switch Google account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    studentProfile: StudentProfile?,
    fallbackName: String,
    userEmail: String,
    isDarkTheme: Boolean,
    isUploadUnlocked: Boolean,
    isSavingProfile: Boolean,
    profileError: String?,
    subjectProgressMap: Map<Subject, SubjectProgress>,
    offlineDownloadCount: Int,
    offlineTotalBytes: Long,
    todayQuizAttempt: QuizAttempt?,
    actionMessage: String?,
    onSaveProfile: (name: String, studentClass: String, onDone: () -> Unit) -> Unit,
    onToggleTheme: () -> Unit,
    onLockUploads: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenDailyQuiz: () -> Unit,
    onSignOut: () -> Unit,
    onClearMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val reducedMotion = LocalReducedMotion.current
    val snackbarHostState = remember { SnackbarHostState() }

    val displayName = studentProfile?.studentName?.takeIf { it.isNotBlank() }
        ?: fallbackName.ifBlank { "Student" }
    val displayClass = studentProfile?.studentClass?.takeIf { it.isNotBlank() } ?: "Class not set"
    val initials = remember(displayName) { computeInitials(displayName) }

    val totalReviewed = remember(subjectProgressMap) {
        subjectProgressMap.values.sumOf { it.reviewedCount }
    }
    val totalMaterials = remember(subjectProgressMap) {
        subjectProgressMap.values.sumOf { it.totalCount }
    }
    val offlineSizeLabel = remember(offlineTotalBytes) {
        DocumentRepository.formatFileSize(offlineTotalBytes)
    }

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(displayName) { mutableStateOf(displayName) }
    var editClass by remember(studentProfile?.studentClass) {
        mutableStateOf(studentProfile?.studentClass.orEmpty())
    }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

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
                        modifier = Modifier.testTag("account_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd),
                            tint = IconNavCyan
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.account_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Student Profile • দরবার",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Student Identity Card (Monochrome + Colored Icons)
                StaggeredFadeIn(index = 0) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_profile_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("account_student_name")
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("account_student_class")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.School,
                                                contentDescription = null,
                                                tint = IconBrandIndigo,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = displayClass,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    if (userEmail.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Email,
                                                contentDescription = null,
                                                tint = IconProfileTeal,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = userEmail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            val editInteraction = remember { MutableInteractionSource() }
                            FilledTonalButton(
                                onClick = {
                                    if (!isEditing) {
                                        editName = displayName
                                        editClass = studentProfile?.studentClass.orEmpty()
                                        localValidationError = null
                                    }
                                    isEditing = !isEditing
                                },
                                interactionSource = editInteraction,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .interactiveButtonMotion(editInteraction)
                                    .testTag("account_edit_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = IconActionBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEditing) "Close Editor" else stringResource(R.string.edit_profile_button),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            AnimatedVisibility(
                                visible = isEditing,
                                enter = fadeIn(tween(DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion), easing = DarbarMotion.EaseOut)),
                                exit = fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut))
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = {
                                            editName = it.take(80)
                                            localValidationError = null
                                        },
                                        label = { Text(stringResource(R.string.student_name_label)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = IconProfileTeal
                                            )
                                        },
                                        singleLine = true,
                                        enabled = !isSavingProfile,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("account_edit_name_input")
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        STUDENT_CLASS_OPTIONS.forEach { option ->
                                            val selected = editClass.equals(option, ignoreCase = true)
                                            FilterChip(
                                                selected = selected,
                                                enabled = !isSavingProfile,
                                                onClick = {
                                                    editClass = option
                                                    localValidationError = null
                                                },
                                                label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = editClass,
                                        onValueChange = {
                                            editClass = it.take(40)
                                            localValidationError = null
                                        },
                                        label = { Text(stringResource(R.string.student_class_label)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Class,
                                                contentDescription = null,
                                                tint = IconBrandIndigo
                                            )
                                        },
                                        singleLine = true,
                                        enabled = !isSavingProfile,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("account_edit_class_input")
                                    )

                                    val errText = localValidationError ?: profileError
                                    if (!errText.isNullOrBlank()) {
                                        Text(
                                            text = errText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    val saveInteraction = remember { MutableInteractionSource() }
                                    Button(
                                        onClick = {
                                            val cleanName = editName.trim()
                                            val cleanClass = editClass.trim()
                                            when {
                                                cleanName.isEmpty() -> localValidationError = "Please enter your name."
                                                cleanClass.isEmpty() -> localValidationError = "Please enter your class."
                                                else -> onSaveProfile(cleanName, cleanClass) {
                                                    isEditing = false
                                                }
                                            }
                                        },
                                        enabled = !isSavingProfile,
                                        interactionSource = saveInteraction,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .interactiveButtonMotion(saveInteraction, enabled = !isSavingProfile)
                                            .testTag("account_save_profile_button")
                                    ) {
                                        if (isSavingProfile) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Saving…")
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = IconSuccessEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = stringResource(R.string.save_profile_button))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Learning Stats Summary Row (Monochrome cards + Colored icons)
                StaggeredFadeIn(index = 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccountStatCard(
                            icon = Icons.Rounded.Insights,
                            value = "$totalReviewed/$totalMaterials",
                            label = "Reviewed",
                            iconTint = IconWarningAmber,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateBack
                        )
                        AccountStatCard(
                            icon = Icons.Rounded.DownloadDone,
                            value = "$offlineDownloadCount",
                            label = "Downloads",
                            iconTint = IconSuccessEmerald,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenDownloads
                        )
                        AccountStatCard(
                            icon = Icons.Rounded.AutoAwesome,
                            value = if (todayQuizAttempt != null) {
                                "${todayQuizAttempt.score}/${todayQuizAttempt.totalQuestions}"
                            } else {
                                "Play"
                            },
                            label = "Daily Quiz",
                            iconTint = IconAiViolet,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenDailyQuiz
                        )
                    }
                }

                // 3. Preferences & Quick Actions Card
                StaggeredFadeIn(index = 2) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Dark Mode Toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) IconMoonIndigo else IconWarningAmber
                                    )
                                    Column {
                                        Text(
                                            text = "Dark Mode",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isDarkTheme) "Dark theme enabled" else "Light theme enabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = { onToggleTheme() },
                                    modifier = Modifier.testTag("account_theme_switch")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Offline Downloads Shortcut Row
                            Surface(
                                onClick = onOpenDownloads,
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_downloads_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DownloadDone,
                                            contentDescription = null,
                                            tint = IconSuccessEmerald
                                        )
                                        Column {
                                            Text(
                                                text = "Offline Downloads",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (offlineDownloadCount == 0) {
                                                    "No documents saved offline yet"
                                                } else {
                                                    "$offlineDownloadCount saved • $offlineSizeLabel"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = IconNavCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Upload Access Status Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isUploadUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (isUploadUnlocked) IconSuccessEmerald else IconWarningAmber
                                    )
                                    Column {
                                        Text(
                                            text = "Upload Access",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isUploadUnlocked) {
                                                "Unlocked for current session"
                                            } else {
                                                "Protected by password"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isUploadUnlocked) {
                                    FilledTonalButton(
                                        onClick = onLockUploads,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("account_lock_uploads_button")
                                    ) {
                                        Text("Lock", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Sign Out Action (Monochrome outline + Colored Logout icon)
                StaggeredFadeIn(index = 3) {
                    val signOutInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        interactionSource = signOutInteraction,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .interactiveButtonMotion(signOutInteraction)
                            .testTag("account_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = IconDangerRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sign_out),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                DarbarFooter()
            }
        }
    }

    if (showSignOutConfirm) {
        AnimatedConfirmDialog(
            title = "Sign out of দরবার?",
            message = "Your student profile and study progress stay safely saved in your account when you sign back in.",
            confirmLabel = "Sign out",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }
}

@Composable
private fun AccountStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.interactiveCardMotion(
            shape = RoundedCornerShape(18.dp),
            onClick = onClick
        ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun computeInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "S"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

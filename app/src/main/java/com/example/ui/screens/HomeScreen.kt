package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DocumentRepository
import com.example.model.QuizAttempt
import com.example.model.Subject
import com.example.model.SubjectProgress
import com.example.ui.components.DarbarFooter
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionIconButton
import com.example.ui.components.StaggeredFadeIn
import com.example.ui.components.interactiveButtonMotion
import com.example.ui.components.interactiveCardMotion
import com.example.ui.theme.IconAiViolet
import com.example.ui.theme.IconBrandIndigo
import com.example.ui.theme.IconMoonIndigo
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconSuccessEmerald
import com.example.ui.theme.IconWarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    studentClass: String?,
    isDarkTheme: Boolean,
    isUploadUnlocked: Boolean,
    todayQuizAttempt: QuizAttempt?,
    todayDateKey: String,
    subjectProgressMap: Map<Subject, SubjectProgress>,
    offlineDownloadCount: Int,
    offlineTotalBytes: Long,
    onToggleTheme: () -> Unit,
    onOpenAccount: () -> Unit,
    onSelectSubject: (Subject) -> Unit,
    onOpenDailyQuiz: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAiTutor: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current
    val fabInteraction = remember { MutableInteractionSource() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAiTutor,
                interactionSource = fabInteraction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = IconAiViolet
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.ai_tutor_fab),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier
                    .interactiveButtonMotion(fabInteraction)
                    .testTag("open_ai_tutor_fab")
            )
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                scope.launch {
                                    if (reducedMotion) {
                                        scrollState.scrollTo(0)
                                    } else {
                                        scrollState.animateScrollTo(
                                            value = 0,
                                            animationSpec = tween(
                                                durationMillis = DarbarMotion.PAGE_MS,
                                                easing = DarbarMotion.EaseOut
                                            )
                                        )
                                    }
                                }
                            }
                            .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = IconBrandIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isUploadUnlocked,
                        enter = fadeIn(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)) +
                            scaleIn(initialScale = 0.9f, animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)),
                        exit = fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut))
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = "Uploads unlocked",
                                    tint = IconSuccessEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Upload On",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    MotionIconButton(
                        onClick = onOpenDownloads,
                        modifier = Modifier.testTag("top_bar_downloads_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (offlineDownloadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = if (offlineDownloadCount > 99) "99+" else "$offlineDownloadCount",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DownloadDone,
                                contentDescription = stringResource(R.string.downloads_open_cd),
                                tint = IconSuccessEmerald
                            )
                        }
                    }
                    MotionIconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        AnimatedContent(
                            targetState = isDarkTheme,
                            transitionSpec = {
                                val ms = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                                (fadeIn(tween(ms, easing = DarbarMotion.EaseOut)) + scaleIn(initialScale = 0.85f, animationSpec = tween(ms, easing = DarbarMotion.EaseOut))) togetherWith
                                    (fadeOut(tween(ms, easing = DarbarMotion.EaseOut)) + scaleOut(targetScale = 0.85f, animationSpec = tween(ms, easing = DarbarMotion.EaseOut)))
                            },
                            label = "theme_icon_toggle"
                        ) { dark ->
                            Icon(
                                imageVector = if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme_cd),
                                tint = if (dark) IconWarningAmber else IconMoonIndigo
                            )
                        }
                    }
                    val accountCd = stringResource(R.string.account_open_cd)
                    MotionIconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier
                            .semantics { contentDescription = accountCd }
                            .testTag("top_bar_account_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = computeInitials(userName),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
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
                    .fillMaxWidth()
                    .widthIn(max = 880.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 1. Editorial Monochrome Hero Card
                StaggeredFadeIn(index = 0) {
                    HeroBannerCard(
                        userName = userName,
                        studentClass = studentClass,
                        onOpenAccount = onOpenAccount
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Overall Study Progress Overview across all 6 Subject Hubs
                StaggeredFadeIn(index = 1) {
                    StudyProgressSummaryCard(
                        subjectProgressMap = subjectProgressMap,
                        onSelectSubject = onSelectSubject
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 3. Section Heading for 6 Subjects
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.subjects_section_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Track your reviewed notes across every subject hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "6 Hubs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Responsive 2-column (phone) or 3-column (tablet/desktop) grid of 6 subjects
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 600.dp) 3 else 2
                    val rows = Subject.entries.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEachIndexed { rowIdx, rowSubjects ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowSubjects.forEachIndexed { colIdx, subject ->
                                    val itemIndex = rowIdx * columns + colIdx + 2
                                    val progress = subjectProgressMap[subject] ?: SubjectProgress(subject)
                                    StaggeredFadeIn(
                                        index = itemIndex,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        SubjectGridCard(
                                            subject = subject,
                                            progress = progress,
                                            onClick = { onSelectSubject(subject) }
                                        )
                                    }
                                }
                                repeat(columns - rowSubjects.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 4. Offline Downloads Section Card
                StaggeredFadeIn(index = 8) {
                    OfflineDownloadsSectionCard(
                        downloadCount = offlineDownloadCount,
                        totalBytes = offlineTotalBytes,
                        onClick = onOpenDownloads
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Daily Quiz Spotlight Card
                StaggeredFadeIn(index = 9) {
                    DailyQuizSpotlightCard(
                        todayAttempt = todayQuizAttempt,
                        todayDateKey = todayDateKey,
                        onClick = onOpenDailyQuiz
                    )
                }

                // 6. Footer + clearance for Extended FAB
                DarbarFooter()
                Spacer(modifier = Modifier.height(76.dp))
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    userName: String,
    studentClass: String?,
    onOpenAccount: () -> Unit
) {
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_darbar_hero_1795616569708),
            contentDescription = "Darbar hero artwork",
            contentScale = ContentScale.Crop,
            colorFilter = grayscaleFilter,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.64f),
                            Color.Black.copy(alpha = 0.16f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val welcomeLabel = buildString {
                append("Welcome, ")
                append(userName.substringBefore(' ').ifBlank { "Student" })
                if (!studentClass.isNullOrBlank()) {
                    append(" • ")
                    append(studentClass)
                }
            }
            Surface(
                onClick = onOpenAccount,
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                shape = CircleShape,
                modifier = Modifier.testTag("hero_account_pill")
            ) {
                Text(
                    text = welcomeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StudyProgressSummaryCard(
    subjectProgressMap: Map<Subject, SubjectProgress>,
    onSelectSubject: (Subject) -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val totalReviewed = subjectProgressMap.values.sumOf { it.reviewedCount }
    val totalMaterials = subjectProgressMap.values.sumOf { it.totalCount }
    val overallFraction = if (totalMaterials == 0) 0f else (totalReviewed.toFloat() / totalMaterials).coerceIn(0f, 1f)
    val overallPercent = (overallFraction * 100f).toInt()
    val completedHubs = subjectProgressMap.values.count { it.isComplete }
    val isAllDone = totalMaterials > 0 && totalReviewed >= totalMaterials

    val animatedOverall by animateFloatAsState(
        targetValue = overallFraction,
        animationSpec = tween(
            durationMillis = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion),
            easing = DarbarMotion.EaseOut
        ),
        label = "overall_study_progress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_progress_overview_card"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = IconWarningAmber,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.study_progress_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalMaterials == 0) {
                                "Preview or mark notes reviewed to track mastery"
                            } else {
                                "$totalReviewed of $totalMaterials materials reviewed" +
                                    if (completedHubs > 0) " • $completedHubs/6 hubs complete" else ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isAllDone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = "$overallPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllDone) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedOverall },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Mini 6-subject quick-glance pills (monochrome surface + colored icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Subject.entries.forEach { subject ->
                    val progress = subjectProgressMap[subject] ?: SubjectProgress(subject)
                    Surface(
                        onClick = { onSelectSubject(subject) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (progress.isComplete) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (progress.isComplete) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.testTag("progress_pill_${subject.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (progress.isComplete) Icons.Rounded.CheckCircle else subject.icon,
                                contentDescription = null,
                                tint = if (progress.isComplete) IconSuccessEmerald else subject.accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = subject.titleEn.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (progress.isComplete) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "${progress.reviewedCount}/${progress.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (progress.isComplete) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectGridCard(
    subject: Subject,
    progress: SubjectProgress,
    onClick: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current

    val animatedFraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(
            durationMillis = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion),
            easing = DarbarMotion.EaseOut
        ),
        label = "subject_progress_${subject.id}"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}")
            .interactiveCardMotion(
                shape = RoundedCornerShape(20.dp),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (progress.isComplete) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = subject.icon,
                        contentDescription = subject.titleEn,
                        tint = subject.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (progress.isComplete) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.all_reviewed_label),
                            tint = IconSuccessEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = IconNavCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = subject.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subject.titleBn,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subject.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Visual per-subject review progress tracker (monochrome crisp bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_progress_${subject.id}"),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress.totalCount == 0) {
                            "No notes yet"
                        } else {
                            "${progress.reviewedCount}/${progress.totalCount} reviewed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (progress.totalCount > 0) {
                        Text(
                            text = "${progress.percent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuizSpotlightCard(
    todayAttempt: QuizAttempt?,
    todayDateKey: String,
    onClick: () -> Unit
) {
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_quiz_card")
            .interactiveCardMotion(
                shape = RoundedCornerShape(24.dp),
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_daily_quiz_art_1795616551651),
            contentDescription = "Daily General Knowledge Quiz artwork",
            contentScale = ContentScale.Crop,
            colorFilter = grayscaleFilter,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.94f),
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.30f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = IconAiViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AI DAILY CHALLENGE • $todayDateKey",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.daily_quiz_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.daily_quiz_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.84f),
                    modifier = Modifier.fillMaxWidth(0.78f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (todayAttempt != null) "View / Retake Quiz" else "Start Today's Quiz",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = IconNavCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (todayAttempt != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed today",
                                tint = IconSuccessEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${todayAttempt.score}/${todayAttempt.totalQuestions}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineDownloadsSectionCard(
    downloadCount: Int,
    totalBytes: Long,
    onClick: () -> Unit
) {
    val sizeLabel = remember(totalBytes) { DocumentRepository.formatFileSize(totalBytes) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_downloads_card")
            .interactiveCardMotion(
                shape = RoundedCornerShape(22.dp),
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = null,
                    tint = IconSuccessEmerald,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.downloads_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = null,
                                tint = IconSuccessEmerald,
                                modifier = Modifier.size(12.dp)
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
                Text(
                    text = if (downloadCount == 0) {
                        "Download notes from any subject to read them offline anytime"
                    } else {
                        "$downloadCount ${if (downloadCount == 1) "document" else "documents"} saved offline • $sizeLabel"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.downloads_open_cd),
                    tint = IconNavCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

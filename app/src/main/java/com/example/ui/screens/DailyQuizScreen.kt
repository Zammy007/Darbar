package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DailyQuiz
import com.example.model.QuizAttempt
import com.example.model.QuizQuestion
import com.example.model.UiState
import com.example.ui.components.DarbarFooter
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionIconButton
import com.example.ui.components.ShimmerDocumentSkeleton
import com.example.ui.components.StaggeredFadeIn
import com.example.ui.components.interactiveButtonMotion
import com.example.ui.components.interactiveCardMotion
import com.example.ui.theme.IconAiViolet
import com.example.ui.theme.IconDangerRose
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconSuccessEmerald
import com.example.ui.theme.IconUploadOrange
import com.example.ui.theme.IconWarningAmber
import com.example.viewmodel.QuizPhase
import com.example.viewmodel.QuizSessionState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuizScreen(
    session: QuizSessionState,
    todayDateKey: String,
    onLoadQuiz: (forceReload: Boolean) -> Unit,
    onStartOfflineBank: () -> Unit,
    onStartQuiz: () -> Unit,
    onSelectAnswer: (questionIndex: Int, optionIndex: Int) -> Unit,
    onNextOrFinish: () -> Unit,
    onOpenReview: () -> Unit,
    onBackFromReview: () -> Unit,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        onLoadQuiz(false)
    }

    BackHandler {
        if (session.phase == QuizPhase.REVIEW) {
            onBackFromReview()
        } else {
            onNavigateBack()
        }
    }

    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    MotionIconButton(
                        onClick = {
                            if (session.phase == QuizPhase.REVIEW) {
                                onBackFromReview()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("quiz_back_button")
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
                            text = if (session.phase == QuizPhase.REVIEW) {
                                "Answer Review"
                            } else {
                                stringResource(R.string.daily_quiz_title)
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "$todayDateKey • দরবার",
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
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = 16.dp)
            ) {
                AnimatedContent(
                    targetState = session.quizState,
                    contentKey = { it::class },
                    transitionSpec = {
                        val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                        val exitMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                        fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) togetherWith
                            fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "quiz_loading_to_content"
                ) { state ->
                    when (state) {
                        is UiState.Loading -> QuizLoadingView()

                        is UiState.Error -> QuizErrorView(
                            message = state.message,
                            onRetry = { onLoadQuiz(true) },
                            onPlayOffline = onStartOfflineBank
                        )

                        is UiState.Success -> {
                            val quiz = state.data
                            AnimatedContent(
                                targetState = session.phase,
                                transitionSpec = {
                                    val enterMs = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion)
                                    val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)
                                    if (reducedMotion) {
                                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                    } else {
                                        (fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) +
                                            slideInHorizontally(tween(enterMs, easing = DarbarMotion.EaseOut)) { slidePx }) togetherWith
                                            (fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut)) +
                                                slideOutHorizontally(tween(exitMs, easing = DarbarMotion.EaseOut)) { -slidePx })
                                    }
                                },
                                label = "quiz_phase_transition"
                            ) { phase ->
                                when (phase) {
                                    QuizPhase.INTRO -> QuizIntroView(
                                        quiz = quiz,
                                        todayAttempt = session.todayAttempt,
                                        onStart = onStartQuiz,
                                        onReviewSaved = onOpenReview
                                    )

                                    QuizPhase.IN_PROGRESS -> QuizQuestionView(
                                        quiz = quiz,
                                        questionIndex = session.currentQuestionIndex,
                                        selectedOption = session.selectedAnswers[session.currentQuestionIndex],
                                        usingOfflineBank = session.usingOfflineBank,
                                        onSelectOption = { optIdx ->
                                            onSelectAnswer(session.currentQuestionIndex, optIdx)
                                        },
                                        onNext = onNextOrFinish
                                    )

                                    QuizPhase.RESULT -> QuizResultView(
                                        quiz = quiz,
                                        selectedAnswers = session.selectedAnswers,
                                        usingOfflineBank = session.usingOfflineBank,
                                        onReviewAnswers = onOpenReview,
                                        onRetake = onStartQuiz,
                                        onBackHome = onNavigateBack
                                    )

                                    QuizPhase.REVIEW -> QuizReviewView(
                                        quiz = quiz,
                                        selectedAnswers = session.selectedAnswers,
                                        onRetake = onStartQuiz,
                                        onBackHome = onNavigateBack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
                Text(
                    text = "Preparing Today's Daily Quiz…",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Checking today's shared quiz or crafting 10 fresh General Knowledge questions with Gemini AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        ShimmerDocumentSkeleton()
        ShimmerDocumentSkeleton()
        DarbarFooter()
    }
}

@Composable
private fun QuizErrorView(
    message: String,
    onRetry: () -> Unit,
    onPlayOffline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = IconDangerRose,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "Couldn't load today's AI Quiz",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val retryInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onRetry,
                    interactionSource = retryInteraction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .interactiveButtonMotion(retryInteraction)
                        .testTag("quiz_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = IconUploadOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Generation")
                }

                val offlineInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onPlayOffline,
                    interactionSource = offlineInteraction,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .interactiveButtonMotion(offlineInteraction)
                        .testTag("quiz_fallback_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = IconSuccessEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Built-in GK Bank Instead")
                }
            }
        }
        DarbarFooter()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizIntroView(
    quiz: DailyQuiz,
    todayAttempt: QuizAttempt?,
    onStart: () -> Unit,
    onReviewSaved: () -> Unit
) {
    val topics = quiz.questions.map { it.topic }.distinct()
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Black & White Banner
        StaggeredFadeIn(index = 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_daily_quiz_art_1795616551651),
                    contentDescription = "Daily Quiz Banner",
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
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.20f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
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
                                text = "SHARED DAILY QUIZ • ${quiz.dateKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "General Knowledge",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${quiz.questions.size} Multiple-Choice Questions • Instant Explanations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }
                }
            }
        }

        // Topics Included
        StaggeredFadeIn(index = 1) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Today's Topics",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topics.forEach { topic ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Score Banner if user already completed today's quiz
        if (todayAttempt != null) {
            StaggeredFadeIn(index = 2) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = IconSuccessEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Completed Today",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Your score: ${todayAttempt.score} / ${todayAttempt.totalQuestions}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        val reviewSavedInteraction = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = onReviewSaved,
                            interactionSource = reviewSavedInteraction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.interactiveButtonMotion(reviewSavedInteraction)
                        ) {
                            Text("Review")
                        }
                    }
                }
            }
        }

        // Start CTA (Monochrome + Colored Play Icon)
        StaggeredFadeIn(index = 3) {
            val startInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onStart,
                interactionSource = startInteraction,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .interactiveButtonMotion(startInteraction)
                    .testTag("start_quiz_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = IconSuccessEmerald
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (todayAttempt != null) "Retake Daily Quiz" else "Start Daily Quiz",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        DarbarFooter()
    }
}

@Composable
private fun QuizQuestionView(
    quiz: DailyQuiz,
    questionIndex: Int,
    selectedOption: Int?,
    usingOfflineBank: Boolean,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()
    val scrollState = rememberScrollState()

    LaunchedEffect(questionIndex) {
        if (reducedMotion) {
            scrollState.scrollTo(0)
        } else {
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(DarbarMotion.MEDIUM_MS, easing = DarbarMotion.EaseOut)
            )
        }
    }

    val total = quiz.questions.size
    val question = quiz.questions[questionIndex]
    val targetProgress = (questionIndex + 1).toFloat() / total.coerceAtLeast(1).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = DarbarMotion.duration(DarbarMotion.PAGE_MS, reducedMotion),
            easing = DarbarMotion.EaseOut
        ),
        label = "quiz_progress"
    )
    val isAnswered = selectedOption != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${questionIndex + 1} of $total",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = CircleShape
            ) {
                Text(
                    text = if (usingOfflineBank) "${question.topic} • Practice" else question.topic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        )

        // Animated Question Card
        AnimatedContent(
            targetState = questionIndex,
            transitionSpec = {
                val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)
                if (reducedMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) +
                        slideInHorizontally(tween(enterMs, easing = DarbarMotion.EaseOut)) { slidePx }) togetherWith
                        (fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut)) +
                            slideOutHorizontally(tween(exitMs, easing = DarbarMotion.EaseOut)) { -slidePx })
                }
            },
            label = "question_slide"
        ) { idx ->
            val currentQ = quiz.questions[idx]
            val currentSelection = if (idx == questionIndex) selectedOption else null
            val answeredHere = currentSelection != null

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentQ.question,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                // 4 Option Cards (Monochrome surfaces + Vibrantly colored check/cancel icons)
                val optionLetters = listOf("A", "B", "C", "D")
                val colorMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                currentQ.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = currentSelection == optIndex
                    val isCorrectOption = optIndex == currentQ.correctIndex

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            !answeredHere -> MaterialTheme.colorScheme.outlineVariant
                            isCorrectOption -> MaterialTheme.colorScheme.primary
                            isSelected && !isCorrectOption -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        },
                        animationSpec = tween(colorMs, easing = DarbarMotion.EaseOut),
                        label = "option_border_$optIndex"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = when {
                            !answeredHere -> MaterialTheme.colorScheme.surface
                            isCorrectOption -> MaterialTheme.colorScheme.surfaceVariant
                            isSelected && !isCorrectOption -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        },
                        animationSpec = tween(colorMs, easing = DarbarMotion.EaseOut),
                        label = "option_bg_$optIndex"
                    )
                    val badgeBg by animateColorAsState(
                        targetValue = when {
                            answeredHere && (isCorrectOption || isSelected) -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        animationSpec = tween(colorMs, easing = DarbarMotion.EaseOut),
                        label = "option_badge_bg_$optIndex"
                    )
                    val badgeText by animateColorAsState(
                        targetValue = if (answeredHere && (isCorrectOption || isSelected)) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(colorMs, easing = DarbarMotion.EaseOut),
                        label = "option_badge_text_$optIndex"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_option_$optIndex")
                            .interactiveCardMotion(
                                enabled = !answeredHere,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                onSelectOption(optIndex)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = bgColor,
                        border = BorderStroke(if (answeredHere && (isCorrectOption || isSelected)) 1.8.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLetters.getOrElse(optIndex) { "${optIndex + 1}" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = badgeText
                                )
                            }

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (answeredHere && isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Correct answer",
                                    tint = IconSuccessEmerald
                                )
                            } else if (answeredHere && isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = "Wrong answer",
                                    tint = IconDangerRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // Instant Feedback + Short Explanation Card (Monochrome + Colored Lightbulb)
        val revealMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
        val hideMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn(tween(revealMs, easing = DarbarMotion.EaseOut)) +
                slideInVertically(tween(revealMs, easing = DarbarMotion.EaseOut)) { if (reducedMotion) 0 else slidePx },
            exit = fadeOut(tween(hideMs, easing = DarbarMotion.EaseOut))
        ) {
            val isCorrect = selectedOption == question.correctIndex
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_explanation_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = if (isCorrect) IconSuccessEmerald else IconWarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isCorrect) "Correct!" else "Not quite — here's why:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Next / Finish Button (Monochrome + Colored Arrow)
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn(tween(revealMs, easing = DarbarMotion.EaseOut)) +
                slideInVertically(tween(revealMs, easing = DarbarMotion.EaseOut)) { if (reducedMotion) 0 else slidePx },
            exit = fadeOut(tween(hideMs, easing = DarbarMotion.EaseOut))
        ) {
            val nextInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onNext,
                interactionSource = nextInteraction,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .interactiveButtonMotion(nextInteraction)
                    .testTag("quiz_next_button")
            ) {
                Text(
                    text = if (questionIndex < total - 1) "Next Question" else "See Final Score",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = IconNavCyan
                )
            }
        }

        DarbarFooter()
    }
}

@Composable
private fun QuizResultView(
    quiz: DailyQuiz,
    selectedAnswers: Map<Int, Int>,
    usingOfflineBank: Boolean,
    onReviewAnswers: () -> Unit,
    onRetake: () -> Unit,
    onBackHome: () -> Unit
) {
    val total = quiz.questions.size
    val correctCount = quiz.questions.indices.count { idx ->
        selectedAnswers[idx] == quiz.questions[idx].correctIndex
    }
    val wrongCount = total - correctCount
    val percent = if (total > 0) (correctCount * 100) / total else 0

    val resultHeadline = when {
        percent >= 90 -> "Outstanding Scholar!"
        percent >= 70 -> "Great Job!"
        percent >= 50 -> "Good Effort!"
        else -> "Keep Learning!"
    }
    val resultMessage = when {
        percent >= 90 -> "Remarkable command over general knowledge. Keep shining on দরবার!"
        percent >= 70 -> "Strong accuracy across today's topics. Review the ones you missed to hit 10/10."
        percent >= 50 -> "Solid foundation! Check the answer explanations to lock in new facts."
        else -> "Every quiz makes you sharper. Review the explanations and try again!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StaggeredFadeIn(index = 0) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = IconWarningAmber,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = "$correctCount / $total",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = resultHeadline,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = resultMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (usingOfflineBank) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Offline Practice Bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultStatPill(
                            label = "Correct",
                            value = "$correctCount",
                            modifier = Modifier.weight(1f)
                        )
                        ResultStatPill(
                            label = "Wrong",
                            value = "$wrongCount",
                            modifier = Modifier.weight(1f)
                        )
                        ResultStatPill(
                            label = "Accuracy",
                            value = "$percent%",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        StaggeredFadeIn(index = 1) {
            val reviewInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onReviewAnswers,
                interactionSource = reviewInteraction,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .interactiveButtonMotion(reviewInteraction)
                    .testTag("quiz_review_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.FactCheck,
                    contentDescription = null,
                    tint = IconSuccessEmerald
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review Answers", style = MaterialTheme.typography.titleMedium)
            }
        }

        StaggeredFadeIn(index = 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val retakeInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onRetake,
                    interactionSource = retakeInteraction,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .interactiveButtonMotion(retakeInteraction)
                        .testTag("quiz_retake_button")
                ) {
                    Text("Retake")
                }
                val homeInteraction = remember { MutableInteractionSource() }
                FilledTonalButton(
                    onClick = onBackHome,
                    interactionSource = homeInteraction,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .interactiveButtonMotion(homeInteraction)
                        .testTag("quiz_home_button")
                ) {
                    Text("Back to Home")
                }
            }
        }

        DarbarFooter()
    }
}

@Composable
private fun ResultStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuizReviewView(
    quiz: DailyQuiz,
    selectedAnswers: Map<Int, Int>,
    onRetake: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        quiz.questions.forEachIndexed { index, q ->
            val userChoice = selectedAnswers[index]
            val isCorrect = userChoice == q.correctIndex

            StaggeredFadeIn(index = index) {
                ReviewQuestionCard(
                    number = index + 1,
                    question = q,
                    userChoiceIndex = userChoice,
                    isCorrect = isCorrect
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val retakeReviewInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onRetake,
                interactionSource = retakeReviewInteraction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .interactiveButtonMotion(retakeReviewInteraction)
            ) {
                Text("Retake Quiz")
            }
            val homeReviewInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onBackHome,
                interactionSource = homeReviewInteraction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .interactiveButtonMotion(homeReviewInteraction)
            ) {
                Text("Back to Home")
            }
        }

        DarbarFooter()
    }
}

@Composable
private fun ReviewQuestionCard(
    number: Int,
    question: QuizQuestion,
    userChoiceIndex: Int?,
    isCorrect: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .interactiveCardMotion(shape = RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q$number • ${question.topic}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                    tint = if (isCorrect) IconSuccessEmerald else IconDangerRose,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = question.question,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            val userAnswerText = userChoiceIndex?.let { question.options.getOrNull(it) } ?: "Unanswered"
            val correctAnswerText = question.options.getOrNull(question.correctIndex).orEmpty()

            if (!isCorrect) {
                Text(
                    text = "Your answer: $userAnswerText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Correct answer: $correctAnswerText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

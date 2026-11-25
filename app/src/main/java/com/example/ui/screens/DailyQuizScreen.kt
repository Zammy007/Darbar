package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.components.ShimmerDocumentSkeleton
import com.example.ui.components.StaggeredFadeIn
import com.example.ui.components.bounceClick
import com.example.ui.theme.DarbarGoldBright
import com.example.ui.theme.DarbarTeal
import com.example.ui.theme.StatusErrorLight
import com.example.ui.theme.StatusSuccessLight
import com.example.viewmodel.QuizPhase
import com.example.viewmodel.QuizSessionState

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
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
                            contentDescription = stringResource(R.string.back_cd)
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
                when (val state = session.quizState) {
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
                                (fadeIn(tween(240)) + slideInHorizontally(tween(260)) { it / 8 }) togetherWith
                                    (fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { -it / 8 })
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
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = DarbarTeal,
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
            tonalElevation = 2.dp,
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
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
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

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quiz_retry_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Generation")
                }

                OutlinedButton(
                    onClick = onPlayOffline,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quiz_fallback_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner
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
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0A2A35).copy(alpha = 0.92f),
                                Color(0xFF0F3B4A).copy(alpha = 0.72f),
                                Color.Transparent
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
                    color = DarbarGoldBright.copy(alpha = 0.22f),
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
                            tint = DarbarGoldBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SHARED DAILY QUIZ • ${quiz.dateKey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarbarGoldBright
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
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // Topics Included
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
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
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Saved Score Banner if user already completed today's quiz
        if (todayAttempt != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StatusSuccessLight.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, StatusSuccessLight.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Completed Today",
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusSuccessLight
                        )
                        Text(
                            text = "Your score: ${todayAttempt.score} / ${todayAttempt.totalQuestions}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    FilledTonalButton(
                        onClick = onReviewSaved,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Review")
                    }
                }
            }
        }

        // Start CTA
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarbarTeal),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_quiz_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (todayAttempt != null) "Retake Daily Quiz" else "Start Daily Quiz",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
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
    val total = quiz.questions.size
    val question = quiz.questions[questionIndex]
    val targetProgress = (questionIndex + 1).toFloat() / total.coerceAtLeast(1).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "quiz_progress"
    )
    val isAnswered = selectedOption != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = if (usingOfflineBank) "${question.topic} • Practice" else question.topic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            color = DarbarTeal,
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
                (fadeIn(tween(240)) + slideInHorizontally(tween(260)) { it / 6 }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { -it / 6 })
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
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentQ.question,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                // 4 Option Cards
                val optionLetters = listOf("A", "B", "C", "D")
                currentQ.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = currentSelection == optIndex
                    val isCorrectOption = optIndex == currentQ.correctIndex

                    val borderColor = when {
                        !answeredHere -> MaterialTheme.colorScheme.outlineVariant
                        isCorrectOption -> StatusSuccessLight
                        isSelected && !isCorrectOption -> StatusErrorLight
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                    val bgColor = when {
                        !answeredHere -> MaterialTheme.colorScheme.surface
                        isCorrectOption -> StatusSuccessLight.copy(alpha = 0.13f)
                        isSelected && !isCorrectOption -> StatusErrorLight.copy(alpha = 0.13f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_option_$optIndex")
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClick(enabled = !answeredHere) {
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
                                    .background(
                                        when {
                                            answeredHere && isCorrectOption -> StatusSuccessLight
                                            answeredHere && isSelected -> StatusErrorLight
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLetters.getOrElse(optIndex) { "${optIndex + 1}" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (answeredHere && (isCorrectOption || isSelected)) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
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
                                    tint = StatusSuccessLight
                                )
                            } else if (answeredHere && isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = "Wrong answer",
                                    tint = StatusErrorLight
                                )
                            }
                        }
                    }
                }
            }
        }

        // Instant Feedback + Short Explanation Card
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn(tween(240)) + expandVertically(tween(260))
        ) {
            val isCorrect = selectedOption == question.correctIndex
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isCorrect) {
                    StatusSuccessLight.copy(alpha = 0.12f)
                } else {
                    StatusErrorLight.copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isCorrect) StatusSuccessLight.copy(alpha = 0.35f) else StatusErrorLight.copy(alpha = 0.35f)
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
                            tint = if (isCorrect) StatusSuccessLight else StatusErrorLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isCorrect) "Correct!" else "Not quite — here's why:",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isCorrect) StatusSuccessLight else StatusErrorLight
                        )
                    }
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Next / Finish Button
        AnimatedVisibility(visible = isAnswered) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarbarTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("quiz_next_button")
            ) {
                Text(
                    text = if (questionIndex < total - 1) "Next Question" else "See Final Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
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
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
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
                        .background(DarbarGoldBright.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = DarbarGoldBright,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "$correctCount / $total",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Offline Practice Bank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
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
                        color = StatusSuccessLight,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatPill(
                        label = "Wrong",
                        value = "$wrongCount",
                        color = StatusErrorLight,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatPill(
                        label = "Accuracy",
                        value = "$percent%",
                        color = DarbarTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Button(
            onClick = onReviewAnswers,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarbarTeal),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("quiz_review_button")
        ) {
            Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Review Answers", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("quiz_retake_button")
            ) {
                Text("Retake")
            }
            FilledTonalButton(
                onClick = onBackHome,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("quiz_home_button")
            ) {
                Text("Back to Home")
            }
        }

        DarbarFooter()
    }
}

@Composable
private fun ResultStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
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
            OutlinedButton(
                onClick = onRetake,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Retake Quiz")
            }
            Button(
                onClick = onBackHome,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
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
            if (isCorrect) StatusSuccessLight.copy(alpha = 0.35f) else StatusErrorLight.copy(alpha = 0.35f)
        ),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
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
                    tint = if (isCorrect) StatusSuccessLight else StatusErrorLight,
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
                    color = StatusErrorLight,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Correct answer: $correctAnswerText",
                style = MaterialTheme.typography.bodySmall,
                color = StatusSuccessLight,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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

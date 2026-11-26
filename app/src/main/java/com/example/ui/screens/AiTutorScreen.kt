package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.Subject
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
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconUploadOrange
import com.example.viewmodel.AiTutorUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorScreen(
    chatState: AiTutorUiState,
    onSelectFocusSubject: (Subject?) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    onRetryLastFailed: () -> Unit,
    onClearChat: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()
    val listState = rememberLazyListState()

    var draftText by rememberSaveable { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val hasConversation = chatState.messages.any { it.role == ChatRole.USER }
    val activeIconColor = chatState.focusSubject?.accentColor ?: IconAiViolet

    // Smoothly auto-scroll when a new message arrives, and pin without animation jitter during live streaming
    val lastMsg = chatState.messages.lastOrNull()
    val lastMsgLength = lastMsg?.text?.length ?: 0
    val isLastStreaming = lastMsg?.isStreaming == true
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            val targetIdx = chatState.messages.lastIndex
            if (reducedMotion) {
                listState.scrollToItem(targetIdx)
            } else {
                listState.animateScrollToItem(targetIdx)
            }
        }
    }
    LaunchedEffect(lastMsgLength, isLastStreaming) {
        if (isLastStreaming && chatState.messages.isNotEmpty()) {
            listState.scrollToItem(chatState.messages.lastIndex)
        }
    }

    fun submitDraft() {
        val text = draftText.trim()
        if (text.isNotEmpty() && !chatState.isSending) {
            draftText = ""
            keyboardController?.hide()
            onSendMessage(text)
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
                    MotionIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_tutor_back_button")
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
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = activeIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.ai_tutor_title),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    chatState.isSending -> "Gemini is replying…"
                                    chatState.focusSubject != null ->
                                        "Focus: ${chatState.focusSubject.titleEn} (${chatState.focusSubject.titleBn})"
                                    else -> "Gemini Study Mentor • All 6 Subjects"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (chatState.isSending) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = hasConversation,
                        enter = fadeIn(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)),
                        exit = fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut))
                    ) {
                        MotionIconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("ai_tutor_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.ai_tutor_clear_cd),
                                tint = IconDangerRose
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { if (it.length <= 2000) draftText = it },
                            placeholder = {
                                Text(
                                    text = if (chatState.focusSubject != null) {
                                        "Ask about ${chatState.focusSubject.titleEn}…"
                                    } else {
                                        stringResource(R.string.ai_tutor_placeholder)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(onSend = { submitDraft() }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_tutor_input_field")
                        )

                        val canSend = draftText.trim().isNotEmpty() && !chatState.isSending
                        val isActionEnabled = chatState.isSending || canSend
                        val sendBg by animateColorAsState(
                            targetValue = when {
                                chatState.isSending -> MaterialTheme.colorScheme.primary
                                canSend -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            animationSpec = tween(
                                DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion),
                                easing = DarbarMotion.EaseOut
                            ),
                            label = "send_btn_bg"
                        )
                        val sendTint by animateColorAsState(
                            targetValue = when {
                                chatState.isSending -> IconDangerRose
                                canSend -> IconNavCyan
                                else -> IconActionBlue.copy(alpha = 0.55f)
                            },
                            animationSpec = tween(
                                DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion),
                                easing = DarbarMotion.EaseOut
                            ),
                            label = "send_btn_tint"
                        )
                        val sendInteraction = remember { MutableInteractionSource() }

                        Surface(
                            onClick = {
                                if (chatState.isSending) {
                                    onStopGenerating()
                                } else {
                                    submitDraft()
                                }
                            },
                            enabled = isActionEnabled,
                            interactionSource = sendInteraction,
                            shape = CircleShape,
                            color = sendBg,
                            modifier = Modifier
                                .size(50.dp)
                                .interactiveButtonMotion(sendInteraction, enabled = isActionEnabled)
                                .testTag(if (chatState.isSending) "ai_tutor_stop_button" else "ai_tutor_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (chatState.isSending) {
                                        Icons.Rounded.Stop
                                    } else {
                                        Icons.AutoMirrored.Rounded.Send
                                    },
                                    contentDescription = if (chatState.isSending) {
                                        "Stop response"
                                    } else {
                                        stringResource(R.string.ai_tutor_send_cd)
                                    },
                                    tint = sendTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
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
                    .widthIn(max = 840.dp)
            ) {
                // Subject Focus Filter Strip (Monochrome Black & White Chips + Vibrantly Colored Icons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = chatState.focusSubject == null,
                        onClick = { onSelectFocusSubject(null) },
                        label = { Text("All Subjects", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = IconBrandIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = IconBrandIndigo
                        ),
                        modifier = Modifier.testTag("tutor_chip_all")
                    )

                    Subject.entries.forEach { subject ->
                        val selected = chatState.focusSubject == subject
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFocusSubject(subject) },
                            label = { Text(subject.titleEn, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = subject.icon,
                                    contentDescription = null,
                                    tint = subject.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = subject.accentColor
                            ),
                            modifier = Modifier.testTag("tutor_chip_${subject.id}")
                        )
                    }
                }

                // Scrollable Multi-turn Message Thread
                val itemAnimMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("ai_tutor_message_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = chatState.messages,
                        key = { it.id }
                    ) { message ->
                        ChatBubbleItem(
                            message = message,
                            aiIconColor = activeIconColor,
                            retryEnabled = !chatState.isSending,
                            onRetry = onRetryLastFailed,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(itemAnimMs, easing = DarbarMotion.EaseOut),
                                placementSpec = tween(itemAnimMs, easing = DarbarMotion.EaseOut)
                            )
                        )
                    }

                    // Starter suggestions when conversation hasn't started yet
                    if (!hasConversation) {
                        item(key = "starter_prompts") {
                            AnimatedVisibility(
                                visible = !hasConversation,
                                enter = fadeIn(tween(itemAnimMs, easing = DarbarMotion.EaseOut)) +
                                    slideInVertically(tween(itemAnimMs, easing = DarbarMotion.EaseOut)) { if (reducedMotion) 0 else slidePx },
                                exit = fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)))
                            ) {
                                StarterPromptSuggestions(
                                    focusSubject = chatState.focusSubject,
                                    iconColor = activeIconColor,
                                    onPromptClick = { prompt ->
                                        if (!chatState.isSending) onSendMessage(prompt)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AnimatedConfirmDialog(
            title = "Start a new chat?",
            message = "This clears the current conversation history with দরবার AI Tutor so you can begin a fresh study session.",
            confirmLabel = "New Chat",
            dismissLabel = "Cancel",
            isDestructive = false,
            onConfirm = {
                showClearConfirm = false
                onClearChat()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    aiIconColor: Color,
    retryEnabled: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (message.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.AutoAwesome,
                    contentDescription = "AI Tutor",
                    tint = if (message.isError) IconDangerRose else aiIconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = if (isUser) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
            } else {
                RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            },
            color = when {
                isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            border = if (!isUser) {
                BorderStroke(
                    1.dp,
                    if (message.isError) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
                )
            } else {
                null
            },
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.isStreaming && message.text.isEmpty()) {
                    TypingDotsIndicator(color = MaterialTheme.colorScheme.onSurface)
                } else {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isUser -> MaterialTheme.colorScheme.onPrimary
                                message.isError -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    if (message.isStreaming) {
                        TypingDotsIndicator(color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (message.isError) {
                    val retryInteraction = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = onRetry,
                        enabled = retryEnabled,
                        interactionSource = retryInteraction,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .interactiveButtonMotion(retryInteraction, enabled = retryEnabled)
                            .testTag("ai_tutor_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = IconUploadOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingDotsIndicator(color: Color) {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) {
        Text(
            text = "Thinking…",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 560,
                        delayMillis = index * 140,
                        easing = DarbarMotion.EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha_$index"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun StarterPromptSuggestions(
    focusSubject: Subject?,
    iconColor: Color,
    onPromptClick: (String) -> Unit
) {
    val prompts = remember(focusSubject) {
        when (focusSubject) {
            Subject.BENGALI -> listOf(
                "সমাস কাকে বলে? উদাহরণসহ সহজভাবে বোঝাও",
                "রবীন্দ্রনাথ ঠাকুরের ছোটগল্পের মূল বৈশিষ্ট্য কী?",
                "সন্ধি ও সমাসের মধ্যে পার্থক্য কী?"
            )
            Subject.ENGLISH -> listOf(
                "Explain active vs passive voice with 4 clear examples",
                "How do I structure a strong argumentative essay?",
                "Give me quick rules for conditional sentences (Type 1–3)"
            )
            Subject.PHYSICS -> listOf(
                "Explain Newton's three laws of motion with real-life examples",
                "Derive the three kinematic equations step by step",
                "What is the difference between Ohm's law and resistance?"
            )
            Subject.MATHEMATICS -> listOf(
                "Walk me through solving quadratic equations using the formula",
                "Explain sine, cosine, and tangent with a quick memory trick",
                "How do derivatives represent rate of change?"
            )
            Subject.CHEMISTRY -> listOf(
                "Explain covalent vs ionic bonding with examples",
                "How do I balance chemical equations quickly?",
                "Summarize functional groups in Organic Chemistry"
            )
            Subject.BIOLOGY -> listOf(
                "Compare mitosis and meiosis in a simple summary",
                "Explain the light and dark reactions of photosynthesis",
                "How does DNA replication work step by step?"
            )
            null -> listOf(
                "Explain Newton's 2nd Law with an easy solved problem",
                "সমাস ও এর প্রধান প্রকারভেদ সহজভাবে বোঝাও",
                "Give me a 5-minute revision summary of photosynthesis",
                "How do I solve quadratic equations without mistakes?"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Try asking",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        prompts.forEachIndexed { idx, prompt ->
            StaggeredFadeIn(index = idx) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .interactiveCardMotion(shape = RoundedCornerShape(16.dp)) {
                            onPromptClick(prompt)
                        }
                        .testTag("starter_prompt_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.PreviewPayload
import com.example.model.StudyDocument
import com.example.ui.components.DarbarMotion
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MotionIconButton
import com.example.ui.components.interactiveButtonMotion
import com.example.ui.theme.IconActionBlue
import com.example.ui.theme.IconDangerRose
import com.example.ui.theme.IconNavCyan
import com.example.ui.theme.IconProfileTeal
import com.example.ui.theme.IconUploadOrange
import com.example.ui.theme.IconWarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Animated password modal protecting the Upload section across all subjects.
 * Uses exact strict equality (no trimming/alteration) with horizontal shake on wrong password.
 */
@Composable
fun UploadPasswordDialog(
    onDismiss: () -> Unit,
    onVerifyPassword: (String) -> Boolean
) {
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }.roundToInt()

    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(reducedMotion) }
    var isClosing by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
    }

    fun triggerClose() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            visible = false
            val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)
            if (exitMs > 0) delay(exitMs.toLong())
            onDismiss()
        }
    }

    fun attemptUnlock() {
        // Strict comparison — pass raw input unmodified to ViewModel (`input == UPLOAD_PASSWORD`)
        val isCorrect = onVerifyPassword(passwordInput)
        if (isCorrect) {
            showError = false
            triggerClose()
        } else {
            showError = true
            if (!reducedMotion) {
                scope.launch {
                    val pattern = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
                    for (target in pattern) {
                        shakeOffset.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = 40, easing = DarbarMotion.EaseOut)
                        )
                    }
                }
            }
        }
    }

    val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
    val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)

    Dialog(
        onDismissRequest = { triggerClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) + scaleIn(
                initialScale = if (reducedMotion) 1f else 0.92f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(enterMs, easing = DarbarMotion.EaseOut)
            ),
            exit = fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut)) + scaleOut(
                targetScale = if (reducedMotion) 1f else 0.92f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(exitMs, easing = DarbarMotion.EaseOut)
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .graphicsLayer {
                        translationX = shakeOffset.value
                    }
                    .testTag("password_modal_card"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = IconWarningAmber
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.password_modal_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Protected section",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        MotionIconButton(
                            onClick = { triggerClose() },
                            modifier = Modifier.testTag("password_modal_close")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close dialog",
                                tint = IconDangerRose
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.password_modal_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            if (showError) showError = false
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Enter upload password") },
                        singleLine = true,
                        isError = showError,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { attemptUnlock() }),
                        trailingIcon = {
                            MotionIconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = IconActionBlue
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_password_input")
                    )

                    AnimatedVisibility(
                        visible = showError,
                        enter = fadeIn(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)) +
                            slideInVertically(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)) {
                                if (reducedMotion) 0 else -slidePx / 2
                            },
                        exit = fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.testTag("password_error_text")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = IconDangerRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.password_incorrect),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val cancelInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = { triggerClose() },
                            interactionSource = cancelInteraction,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .interactiveButtonMotion(cancelInteraction)
                                .testTag("password_cancel_button")
                        ) {
                            Text(stringResource(R.string.cancel_button))
                        }

                        val unlockInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { attemptUnlock() },
                            interactionSource = unlockInteraction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .interactiveButtonMotion(unlockInteraction)
                                .testTag("password_unlock_button")
                        ) {
                            Text(stringResource(R.string.unlock_button))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen animated Preview Dialog supporting both Images (pinch-to-zoom)
 * and PDFs (native Android PdfRenderer multi-page viewer).
 */
@Composable
fun DocumentPreviewDialog(
    payload: PreviewPayload,
    onDismiss: () -> Unit,
    onDownloadClick: (StudyDocument, ByteArray) -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(reducedMotion) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    fun triggerClose(afterClose: () -> Unit = onDismiss) {
        if (isClosing) return
        isClosing = true
        scope.launch {
            visible = false
            val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)
            if (exitMs > 0) delay(exitMs.toLong())
            afterClose()
        }
    }

    val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
    val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)

    Dialog(
        onDismissRequest = { triggerClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) + scaleIn(
                initialScale = if (reducedMotion) 1f else 0.94f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(enterMs, easing = DarbarMotion.EaseOut)
            ),
            exit = fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut)) + scaleOut(
                targetScale = if (reducedMotion) 1f else 0.94f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(exitMs, easing = DarbarMotion.EaseOut)
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MotionIconButton(
                            onClick = { triggerClose() },
                            modifier = Modifier.testTag("preview_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close preview",
                                tint = IconDangerRose
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = payload.document.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = payload.document.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val downloadInteraction = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = {
                                triggerClose { onDownloadClick(payload.document, payload.bytes) }
                            },
                            interactionSource = downloadInteraction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .interactiveButtonMotion(downloadInteraction)
                                .testTag("preview_download_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = IconProfileTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.download_button))
                        }
                    }

                    // Body Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF09090B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (payload.document.isImage()) {
                            ZoomableImagePreview(bytes = payload.bytes, title = payload.document.title)
                        } else if (payload.document.isPdf()) {
                            PdfDocumentPreview(bytes = payload.bytes, docId = payload.document.id)
                        } else {
                            Text(
                                text = "Preview is available for PDFs and images.",
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
private fun ZoomableImagePreview(bytes: ByteArray, title: String) {
    val bitmap = remember(bytes) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (bitmap == null) {
        Text(text = "Unable to decode image preview.", color = Color.White)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale
                    offset = if (newScale > 1f) offset + pan else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        val reducedMotion = LocalReducedMotion.current
        AnimatedVisibility(
            visible = scale > 1.05f,
            enter = fadeIn(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)),
            exit = fadeOut(tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)) +
                scaleOut(targetScale = 0.85f, animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion), easing = DarbarMotion.EaseOut)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                MotionIconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset zoom",
                        tint = IconUploadOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfDocumentPreview(bytes: ByteArray, docId: String) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isRendering by remember { mutableStateOf(true) }

    val tempFile = remember(docId) {
        File(context.cacheDir, "darbar_preview_${docId.ifBlank { "doc" }}.pdf")
    }

    DisposableEffect(tempFile) {
        onDispose {
            runCatching { tempFile.delete() }
        }
    }

    LaunchedEffect(bytes, currentPage) {
        isRendering = true
        errorText = null
        val renderResult = withContext(Dispatchers.IO) {
            runCatching {
                if (!tempFile.exists() || tempFile.length() != bytes.size.toLong()) {
                    tempFile.writeBytes(bytes)
                }
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val total = renderer.pageCount
                        if (total <= 0) {
                            throw IllegalStateException("This PDF contains no pages.")
                        }
                        val safePage = currentPage.coerceIn(0, total - 1)
                        renderer.openPage(safePage).use { page ->
                            val targetWidth = 1080
                            val ratio = targetWidth.toFloat() / page.width.coerceAtLeast(1).toFloat()
                            val targetHeight = (page.height * ratio).roundToInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(
                                targetWidth,
                                targetHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            total to bmp
                        }
                    }
                }
            }
        }
        isRendering = false
        renderResult.fold(
            onSuccess = { (total, bmp) ->
                pageCount = total
                renderedBitmap = bmp
            },
            onFailure = { err ->
                errorText = err.localizedMessage ?: "Failed to render PDF preview."
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val reducedMotion = LocalReducedMotion.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = Triple(errorText, isRendering && renderedBitmap == null, renderedBitmap),
                contentKey = { (err, loading, bmp) ->
                    when {
                        err != null -> "error"
                        loading -> "loading"
                        bmp != null -> "page_$currentPage"
                        else -> "empty"
                    }
                },
                transitionSpec = {
                    val ms = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
                    fadeIn(tween(ms, easing = DarbarMotion.EaseOut)) togetherWith
                        fadeOut(tween(ms, easing = DarbarMotion.EaseOut))
                },
                label = "pdf_page_fade"
            ) { (err, loading, bmp) ->
                when {
                    err != null -> {
                        Text(
                            text = err,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    loading -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    bmp != null -> {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }

        if (pageCount > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    MotionIconButton(
                        onClick = { if (currentPage > 0) currentPage -= 1 },
                        enabled = currentPage > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = "Previous page",
                            tint = if (currentPage > 0) IconNavCyan else IconNavCyan.copy(alpha = 0.35f)
                        )
                    }
                    Text(
                        text = "Page ${currentPage + 1} / $pageCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    MotionIconButton(
                        onClick = { if (currentPage < pageCount - 1) currentPage += 1 },
                        enabled = currentPage < pageCount - 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "Next page",
                            tint = if (currentPage < pageCount - 1) IconNavCyan else IconNavCyan.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable confirmation modal with GPU-friendly scale + fade enter AND exit animation.
 */
@Composable
fun AnimatedConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(reducedMotion) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    fun triggerClose(afterClose: () -> Unit) {
        if (isClosing) return
        isClosing = true
        scope.launch {
            visible = false
            val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)
            if (exitMs > 0) delay(exitMs.toLong())
            afterClose()
        }
    }

    val enterMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
    val exitMs = DarbarMotion.duration(DarbarMotion.MODAL_EXIT_MS, reducedMotion)

    Dialog(
        onDismissRequest = { triggerClose(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(enterMs, easing = DarbarMotion.EaseOut)) + scaleIn(
                initialScale = if (reducedMotion) 1f else 0.92f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(enterMs, easing = DarbarMotion.EaseOut)
            ),
            exit = fadeOut(tween(exitMs, easing = DarbarMotion.EaseOut)) + scaleOut(
                targetScale = if (reducedMotion) 1f else 0.92f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(exitMs, easing = DarbarMotion.EaseOut)
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.88f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val cancelInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = { triggerClose(onDismiss) },
                            interactionSource = cancelInteraction,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .interactiveButtonMotion(cancelInteraction)
                        ) {
                            Text(dismissLabel)
                        }
                        val confirmInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { triggerClose(onConfirm) },
                            interactionSource = confirmInteraction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .interactiveButtonMotion(confirmInteraction)
                        ) {
                            Text(confirmLabel)
                        }
                    }
                }
            }
        }
    }
}
